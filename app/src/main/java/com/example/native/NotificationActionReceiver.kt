package com.example.native

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import com.example.data.local.HabitDatabase
import com.example.data.repository.HabitRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.StreakEngine
import com.example.widget.HabitWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val habitId = intent.getLongExtra(NotificationHelper.EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(NotificationHelper.EXTRA_HABIT_NAME) ?: "Habit"
        val isTestAlarm = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_TEST_ALARM, false)
        val snoozeMinutes = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 10).coerceIn(1, 60)

        if (habitId == -1L && !isTestAlarm) return

        // Stop active alarm ringing/vibration and dismiss AlarmAlertActivity immediately
        if (action == NotificationHelper.ACTION_MARK_DONE ||
            action == NotificationHelper.ACTION_SNOOZE ||
            action == NotificationHelper.ACTION_DISMISS_ALARM
        ) {
            AlarmRingingService.stop(context, habitId)
        }

        if (action == NotificationHelper.ACTION_DISMISS_ALARM) {
            NotificationHelper(context).cancelNotification(habitId)
            Log.d(TAG, "Dismissed alarm for habitId=$habitId ($habitName)")
            return
        }

        if (action == NotificationHelper.ACTION_MARK_DONE) {
            if (isTestAlarm) {
                NotificationHelper(context).cancelNotification(habitId)
                Handler(Looper.getMainLooper()).post {
                    try {
                        Toast.makeText(context, "✓ Test alarm completed successfully!", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = HabitDatabase.getInstance(context)
                    val repository = HabitRepository(db.habitDao(), db.habitCompletionDao())
                    val settingsRepo = SettingsRepository(db.appSettingDao())
                    val today = LocalDate.now()
                    val todayStr = StreakEngine.formatDate(today)

                    val habit = repository.getHabitById(habitId)
                    if (habit == null || habit.isArchived) {
                        Log.w(TAG, "Cannot mark done: habit $habitId is null or archived")
                        return@launch
                    }
                    val existingCompletions = repository.getCompletionsListForHabit(habitId)
                    val alreadyDone = existingCompletions.any { it.completionDate == todayStr }
                    if (alreadyDone) {
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.cancelNotification(habitId)
                        try {
                            HabitWidget().updateAll(context)
                        } catch (_: Exception) {}
                        Handler(Looper.getMainLooper()).post {
                            try {
                                Toast.makeText(context, "'$habitName' was already completed today.", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        }
                        return@launch
                    }

                    // Use repository's idempotent markHabitDone (prevents duplicate entries)
                    repository.markHabitDone(habitId, todayStr, "Completed via notification action")

                    // Re-calculate streak and record milestone if reached
                    val completions = repository.getCompletionsListForHabit(habitId)
                    val (currentStreak, _) = StreakEngine.calculateStreaks(completions, today, habit.frequency)
                    val milestone = StreakEngine.findMilestoneForStreak(currentStreak)
                    if (milestone != null) {
                        val celebratedKey = "milestone_celebrated_${habitId}_${currentStreak}"
                        val alreadyCelebrated = settingsRepo.getSetting(celebratedKey, "") == todayStr
                        if (!alreadyCelebrated) {
                            settingsRepo.setSetting(celebratedKey, todayStr)
                        }
                    }

                    // Cancel any active notification for this habit
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.cancelNotification(habitId)

                    // Keep alarm schedule accurate for next occurrence
                    val scheduler = AlarmScheduler(context)
                    scheduler.scheduleHabitReminder(habit, isDoneTodayOverride = true)

                    // Refresh widget immediately
                    try {
                        HabitWidget().updateAll(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Widget refresh error", e)
                    }

                    Handler(Looper.getMainLooper()).post {
                        try {
                            Toast.makeText(
                                context,
                                "🎉 '$habitName' marked complete! Streak saved.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (_: Exception) {}
                    }
                    Log.d(TAG, "Marked habit $habitId done via notification action; widget refreshed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking done for habit $habitId", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == NotificationHelper.ACTION_SNOOZE) {
            if (isTestAlarm) {
                NotificationHelper(context).cancelNotification(habitId)
                Handler(Looper.getMainLooper()).post {
                    try {
                        Toast.makeText(context, "⏰ Test snooze ($snoozeMinutes min) triggered", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.cancelNotification(habitId)

                    val scheduler = AlarmScheduler(context)
                    scheduler.scheduleSnooze(habitId, habitName, snoozeMinutes * 60_000L)

                    try {
                        HabitWidget().updateAll(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Widget refresh error after snooze", e)
                    }

                    Handler(Looper.getMainLooper()).post {
                        try {
                            Toast.makeText(
                                context,
                                "⏰ Snoozed '$habitName' for $snoozeMinutes minutes",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (_: Exception) {}
                    }
                    Log.d(TAG, "Snoozed habit $habitId for $snoozeMinutes minutes")
                } catch (e: Exception) {
                    Log.e(TAG, "Error snoozing habit $habitId", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
