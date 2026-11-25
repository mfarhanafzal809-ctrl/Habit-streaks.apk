package com.example.native

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.HabitDatabase
import com.example.data.model.Habit
import com.example.domain.StreakEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        const val MAX_REMINDERS_PER_HABIT = 20
        const val SNOOZE_DURATION_MILLIS = 10 * 60 * 1000L // 10 minutes

        fun getReminderRequestCode(habitId: Long, reminderIndex: Int): Int {
            val safeId = (habitId and 0x7FFF).toInt()
            return safeId * 100 + (reminderIndex % 50)
        }

        fun getWidgetPreReminderRequestCode(habitId: Long, reminderIndex: Int): Int {
            val safeId = (habitId and 0x7FFF).toInt()
            return safeId * 100 + 50 + (reminderIndex % 50)
        }

        fun getSnoozeRequestCode(habitId: Long): Int {
            val safeId = (habitId and 0x7FFF).toInt()
            return safeId * 100 + 99
        }

        @Volatile
        private var cachedGlobalNotifications: Boolean = true

        @JvmStatic
        fun updateGlobalNotificationsCache(enabled: Boolean) {
            cachedGlobalNotifications = enabled
        }
    }

    data class NextAlarmInfo(
        val habit: Habit,
        val triggerTimeMillis: Long,
        val hour: Int,
        val minute: Int
    )

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (_: Throwable) {
                false
            }
        } else {
            true
        }
    }

    suspend fun isGlobalNotificationsEnabled(): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = HabitDatabase.getInstance(context)
            val value = db.appSettingDao().getSettingValue("global_notifications")?.toBoolean() ?: true
            cachedGlobalNotifications = value
            value
        } catch (e: Exception) {
            Log.e(TAG, "Error checking global notifications preference", e)
            cachedGlobalNotifications
        }
    }

    fun isGlobalNotificationsEnabledSync(): Boolean {
        return cachedGlobalNotifications
    }

    /**
     * Computes exact trigger millis for a reminder slot respecting DAILY / WEEKDAYS / WEEKENDS.
     */
    fun computeNextTriggerTimeMillis(
        hour: Int,
        minute: Int,
        frequency: com.example.data.model.HabitFrequency,
        isDoneToday: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now(),
        minTriggerLeadMillis: Long = 0L
    ): Long {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val isDueToday = StreakEngine.isDueOn(frequency, today)
        val canTriggerLaterToday = isDueToday && !isDoneToday && calendar.timeInMillis > (nowMillis + minTriggerLeadMillis)

        return if (canTriggerLaterToday) {
            calendar.timeInMillis
        } else {
            var nextDate = today.plusDays(1)
            while (!StreakEngine.isDueOn(frequency, nextDate)) {
                nextDate = nextDate.plusDays(1)
            }
            Calendar.getInstance().apply {
                set(Calendar.YEAR, nextDate.year)
                set(Calendar.MONTH, nextDate.monthValue - 1)
                set(Calendar.DAY_OF_MONTH, nextDate.dayOfMonth)
                set(Calendar.HOUR_OF_DAY, safeHour)
                set(Calendar.MINUTE, safeMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    /**
     * Returns the earliest upcoming scheduled reminder across all active habits.
     */
    fun getNextScheduledAlarm(
        habits: List<Habit>,
        completedTodayIds: Set<Long> = emptySet(),
        nowMillis: Long = System.currentTimeMillis(),
        today: LocalDate = LocalDate.now()
    ): NextAlarmInfo? {
        if (!cachedGlobalNotifications) return null
        var best: NextAlarmInfo? = null

        for (habit in habits) {
            if (!habit.reminderEnabled || habit.isArchived) continue
            val isDone = completedTodayIds.contains(habit.id)
            for ((h, m) in habit.getAllReminderTimes().take(MAX_REMINDERS_PER_HABIT)) {
                val trigger = computeNextTriggerTimeMillis(h, m, habit.frequency, isDone, nowMillis, today)
                if (best == null || trigger < best.triggerTimeMillis) {
                    best = NextAlarmInfo(habit, trigger, h.coerceIn(0, 23), m.coerceIn(0, 59))
                }
            }
        }
        return best
    }

    /**
     * Schedules alarms for all applicable reminder times of a habit using setAlarmClock()
     * for exact-second Doze-bypassing execution.
     */
    fun scheduleHabitReminder(
        habit: Habit,
        isDoneTodayOverride: Boolean? = null,
        minTriggerLeadMillis: Long = 0L
    ) {
        // Cancel all existing alarms, snooze, and pre-reminders for this habit first
        cancelHabitReminder(habit.id)

        val isMain = android.os.Looper.myLooper() != null && android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
        if (!isMain) {
            // Read live global setting directly whenever already on a background thread
            try {
                val db = HabitDatabase.getInstance(context)
                val value = db.appSettingDao().getSettingValueSync("global_notifications")?.toBoolean() ?: true
                cachedGlobalNotifications = value
            } catch (_: Exception) {}
        }

        // If global notifications are disabled, habit reminder is disabled, or habit is archived, do not schedule
        if (!isGlobalNotificationsEnabledSync() || !habit.reminderEnabled || habit.isArchived) {
            Log.d(TAG, "Skipping scheduling for habit '${habit.name}' (globalEnabled=${isGlobalNotificationsEnabledSync()}, reminderEnabled=${habit.reminderEnabled}, archived=${habit.isArchived})")
            return
        }

        val allTimes = habit.getAllReminderTimes().take(MAX_REMINDERS_PER_HABIT)
        val today = LocalDate.now()
        val todayStr = StreakEngine.formatDate(today)

        // Check if the habit is already completed today without blocking the main thread
        val isDoneToday = isDoneTodayOverride ?: run {
            if (isMain) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = HabitDatabase.getInstance(context)
                        val globalEnabled = db.appSettingDao().getSettingValue("global_notifications")?.toBoolean() ?: true
                        cachedGlobalNotifications = globalEnabled
                        val latestHabit = db.habitDao().getHabitById(habit.id) ?: habit
                        val done = db.habitCompletionDao().getCompletionSync(habit.id, todayStr) != null
                        scheduleHabitReminder(
                            latestHabit,
                            isDoneTodayOverride = done,
                            minTriggerLeadMillis = minTriggerLeadMillis
                        )
                    } catch (_: Throwable) {}
                }
                return
            } else {
                try {
                    val db = HabitDatabase.getInstance(context)
                    db.habitCompletionDao().getCompletionSync(habit.id, todayStr) != null
                } catch (e: Exception) {
                    false
                }
            }
        }

        for ((index, timePair) in allTimes.withIndex()) {
            val (hour, minute) = timePair
            val safeHour = hour.coerceIn(0, 23)
            val safeMinute = minute.coerceIn(0, 59)

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra(NotificationHelper.EXTRA_HABIT_ID, habit.id)
                putExtra(NotificationHelper.EXTRA_HABIT_NAME, habit.name)
            }

            val requestCode = getReminderRequestCode(habit.id, index)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = computeNextTriggerTimeMillis(
                hour = safeHour,
                minute = safeMinute,
                frequency = habit.frequency,
                isDoneToday = isDoneToday,
                nowMillis = System.currentTimeMillis(),
                today = today,
                minTriggerLeadMillis = minTriggerLeadMillis
            )

            // Schedule widget pre-reminder update 15 minutes before the first reminder slot
            if (index == 0 && !isDoneToday) {
                scheduleWidgetPreReminderUpdate(habit.id, index, triggerTime)
            }

            setAlarmSafely(
                triggerTime = triggerTime,
                pendingIntent = pendingIntent,
                habitId = habit.id,
                description = "reminder #$index for habit '${habit.name}' (id=${habit.id})"
            )
        }
    }

    /**
     * Schedules a one-off snooze alarm for the specified habit.
     */
    fun scheduleSnooze(habitId: Long, habitName: String, delayMillis: Long = SNOOZE_DURATION_MILLIS) {
        val isMain = android.os.Looper.myLooper() != null && android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
        if (!isMain) {
            try {
                val db = HabitDatabase.getInstance(context)
                val value = db.appSettingDao().getSettingValueSync("global_notifications")?.toBoolean() ?: true
                cachedGlobalNotifications = value
            } catch (_: Exception) {}
        }

        if (!isGlobalNotificationsEnabledSync()) {
            Log.d(TAG, "Skipping snooze: global notifications disabled")
            return
        }

        val triggerTime = System.currentTimeMillis() + delayMillis
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_HABIT_ID, habitId)
            putExtra(NotificationHelper.EXTRA_HABIT_NAME, habitName)
            putExtra(NotificationHelper.EXTRA_IS_SNOOZE_TRIGGER, true)
        }
        val requestCode = getSnoozeRequestCode(habitId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(
            triggerTime = triggerTime,
            pendingIntent = pendingIntent,
            habitId = habitId,
            description = "snooze for habit '$habitName' (id=$habitId)"
        )
    }

    private fun buildShowIntent(habitId: Long): PendingIntent {
        val showIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getActivity(
            context,
            habitId.toInt() + 40000,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarmSafely(
        triggerTime: Long,
        pendingIntent: PendingIntent,
        habitId: Long,
        description: String
    ) {
        try {
            val canExact = canScheduleExactAlarms()
            if (canExact) {
                // setAlarmClock() wakes the device at the exact millisecond even under Doze/App Standby
                val info = AlarmManager.AlarmClockInfo(triggerTime, buildShowIntent(habitId))
                alarmManager.setAlarmClock(info, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled $description (exactAlarmClock=$canExact) at ${java.util.Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm for $description; falling back to inexact", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed fallback inexact alarm for $description", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error scheduling alarm for $description", e)
        }
    }

    private fun scheduleWidgetPreReminderUpdate(habitId: Long, index: Int, reminderTime: Long) {
        val updateTime = reminderTime - (15 * 60 * 1000)
        if (updateTime <= System.currentTimeMillis()) return

        val intent = Intent(context, com.example.widget.WidgetUpdateReceiver::class.java)
        val requestCode = getWidgetPreReminderRequestCode(habitId, index)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, updateTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, updateTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled pre-reminder widget update for habit id=$habitId at ${java.util.Date(updateTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule pre-reminder widget update for habit id=$habitId", e)
        }
    }

    /**
     * Cancels all main alarms, snooze alarms, and widget pre-reminders for a specific habit.
     */
    fun cancelHabitReminder(habitId: Long) {
        val reminderIntent = Intent(context, ReminderReceiver::class.java)
        val widgetIntent = Intent(context, com.example.widget.WidgetUpdateReceiver::class.java)

        // Cancel snooze alarm
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            getSnoozeRequestCode(habitId),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(snoozePendingIntent)

        // Cancel legacy codes for backward compatibility
        val legacyMain = PendingIntent.getBroadcast(
            context,
            habitId.toInt(),
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(legacyMain)

        val legacyWidget = PendingIntent.getBroadcast(
            context,
            habitId.toInt() + 20000,
            widgetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(legacyWidget)

        // Cancel all slots up to MAX_REMINDERS_PER_HABIT
        for (i in 0 until MAX_REMINDERS_PER_HABIT) {
            val mainPendingIntent = PendingIntent.getBroadcast(
                context,
                getReminderRequestCode(habitId, i),
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(mainPendingIntent)

            val widgetPendingIntent = PendingIntent.getBroadcast(
                context,
                getWidgetPreReminderRequestCode(habitId, i),
                widgetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(widgetPendingIntent)
        }
        Log.d(TAG, "Cancelled all alarms, snooze, and widget pre-reminders for habit id=$habitId")
    }

    /**
     * Cancels all scheduled alarms and widget pre-reminders across all habits.
     */
    suspend fun cancelAllReminders() = withContext(Dispatchers.IO) {
        try {
            val db = HabitDatabase.getInstance(context)
            val habits = db.habitDao().getAllHabitsList()
            for (habit in habits) {
                cancelHabitReminder(habit.id)
            }
            Log.d(TAG, "Cancelled all alarms and widget pre-reminders across ${habits.size} habits")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling all reminders", e)
        }
    }

    /**
     * Suspending reconciliation that guarantees all habits are loaded and scheduled
     * or cancelled before completing. Used by receivers with goAsync().
     */
    suspend fun reconcileAllReminders() = withContext(Dispatchers.IO) {
        try {
            if (!isGlobalNotificationsEnabled()) {
                Log.d(TAG, "Global notifications disabled. Cancelling all active alarms.")
                cancelAllReminders()
                return@withContext
            }

            val db = HabitDatabase.getInstance(context)
            val habits = db.habitDao().getActiveReminders()
            for (habit in habits) {
                scheduleHabitReminder(habit)
            }
            Log.d(TAG, "Reconciled and rescheduled ${habits.size} habit reminders")
        } catch (e: Exception) {
            Log.e(TAG, "Error during reconcileAllReminders", e)
        }
    }

    /**
     * Asynchronous fire-and-forget helper.
     */
    fun rescheduleAllReminders() {
        CoroutineScope(Dispatchers.IO).launch {
            reconcileAllReminders()
        }
    }
}
