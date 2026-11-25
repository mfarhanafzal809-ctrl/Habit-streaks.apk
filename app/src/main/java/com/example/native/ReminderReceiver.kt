package com.example.native

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.example.data.local.HabitDatabase
import com.example.data.model.RingMode
import com.example.domain.StreakEngine
import com.example.widget.HabitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        private const val WAKE_LOCK_TIMEOUT_MS = 15_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(NotificationHelper.EXTRA_HABIT_ID, -1L)
        if (habitId == -1L) return

        val isSnoozeTrigger = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_SNOOZE_TRIGGER, false)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HabitStreaks:ReminderReceiver")?.apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        } catch (_: Throwable) {
            null
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = HabitDatabase.getInstance(context)
                val scheduler = AlarmScheduler(context)

                // 1. Check global notifications preference and sync shared cache
                val globalEnabled = db.appSettingDao().getSettingValue("global_notifications")?.toBoolean() ?: true
                AlarmScheduler.updateGlobalNotificationsCache(globalEnabled)
                if (!globalEnabled) {
                    Log.d(TAG, "Global reminders disabled. Suppressing alarm and clearing schedule for habitId=$habitId")
                    scheduler.cancelHabitReminder(habitId)
                    return@launch
                }

                // 2. Check habit existence, reminderEnabled, and archived state
                val habit = db.habitDao().getHabitById(habitId)
                if (habit == null || !habit.reminderEnabled || habit.isArchived) {
                    Log.d(TAG, "Habit is inactive/disabled/deleted. Cancelling alarms for habitId=$habitId")
                    scheduler.cancelHabitReminder(habitId)
                    return@launch
                }

                // 3. Check frequency validity for today and completion status
                val today = LocalDate.now()
                val isDueToday = StreakEngine.isDueOn(habit.frequency, today)
                val todayStr = StreakEngine.formatDate(today)
                val isDoneToday = db.habitCompletionDao().getCompletion(habitId, todayStr) != null

                if (isDueToday && !isDoneToday) {
                    val globalVibration = db.appSettingDao().getSettingValue("global_vibration")?.toBoolean() ?: true
                    val effectiveVibrate = habit.vibrateEnabled && globalVibration
                    val completions = db.habitCompletionDao().getCompletionsListForHabit(habitId)
                    val (currentStreak, _) = StreakEngine.calculateStreaks(completions, today, habit.frequency)
                    val notificationHelper = NotificationHelper(context)

                    val shouldRingOrVibrate = when (habit.ringMode) {
                        RingMode.SOUND_AND_VIBRATE -> true
                        RingMode.VIBRATE_ONLY -> effectiveVibrate
                        RingMode.SILENT -> false
                    }

                    if (!shouldRingOrVibrate) {
                        notificationHelper.showHabitNotification(
                            habit = habit,
                            currentStreak = currentStreak,
                            playChannelAlert = false,
                            globalVibrationEnabled = globalVibration
                        )
                        Log.d(TAG, "Displayed silent reminder notification for habit: ${habit.name}")
                    } else {
                        val playSound = habit.ringMode == RingMode.SOUND_AND_VIBRATE
                        try {
                            AlarmRingingService.start(
                                context = context,
                                habitId = habit.id,
                                habitName = habit.name,
                                habitColor = habit.colorHex,
                                currentStreak = currentStreak,
                                ringtoneUri = habit.ringtoneUri,
                                playSound = playSound,
                                vibrate = effectiveVibrate,
                                isTestAlarm = false
                            )
                            Log.d(TAG, "Launched AlarmRingingService for habit: ${habit.name} (playSound=$playSound, vibrate=$effectiveVibrate)")
                        } catch (e: Exception) {
                            // Catch ForegroundServiceStartNotAllowedException / SecurityException cleanly and fall back
                            // to the notification channel's FileProvider sound + vibration without double-ringing or leaking SoundPlayer
                            Log.w(TAG, "Foreground service start blocked for habitId=$habitId; using notification fallback", e)
                            notificationHelper.showHabitNotification(
                                habit = habit,
                                currentStreak = currentStreak,
                                playChannelAlert = true,
                                globalVibrationEnabled = globalVibration
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "Skipping alarm (isDueToday=$isDueToday, isDoneToday=$isDoneToday, snooze=$isSnoozeTrigger) for habit: ${habit.name}")
                }

                // 4. Reschedule next occurrence cleanly (no need when snooze fired for non-primary chain)
                if (!isSnoozeTrigger) {
                    scheduler.scheduleHabitReminder(
                        habit,
                        isDoneTodayOverride = isDoneToday,
                        minTriggerLeadMillis = 60_000L
                    )
                }

                // 5. Refresh glance widgets
                try {
                    HabitWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to refresh widget in ReminderReceiver", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onReceive for habitId=$habitId", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Throwable) {}
                pendingResult.finish()
            }
        }
    }
}
