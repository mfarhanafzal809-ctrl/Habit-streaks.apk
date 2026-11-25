package com.example.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.example.data.local.HabitDatabase
import com.example.data.repository.HabitRepository
import com.example.domain.StreakEngine

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[ActionParameters.Key<Long>("habitId")] ?: return
        val isDone = parameters[ActionParameters.Key<Boolean>("isDone")] ?: return
        
        val db = HabitDatabase.getInstance(context)
        val repository = HabitRepository(db.habitDao(), db.habitCompletionDao())
        val todayStr = StreakEngine.getTodayString()

        val habit = db.habitDao().getHabitById(habitId)
        if (isDone) {
            repository.undoHabitDone(habitId, todayStr)
        } else {
            repository.markHabitDone(habitId, todayStr)
            com.example.native.AlarmRingingService.stop(context, habitId)
            com.example.native.NotificationHelper(context).cancelNotification(habitId)
        }

        if (habit != null && habit.reminderEnabled && !habit.isArchived) {
            com.example.native.AlarmScheduler(context).scheduleHabitReminder(
                habit,
                isDoneTodayOverride = !isDone
            )
        }

        HabitWidget().updateAll(context)
    }
}

class HabitControlAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[ActionParameters.Key<Long>("habitId")] ?: return
        val toggleType = parameters[ActionParameters.Key<String>("toggleType")] ?: return
        
        val db = HabitDatabase.getInstance(context)
        val habit = db.habitDao().getHabitById(habitId) ?: return
        
        val updatedHabit = when (toggleType) {
            "reminder" -> habit.copy(reminderEnabled = !habit.reminderEnabled)
            "sound" -> {
                val newMode = if (habit.ringMode == com.example.data.model.RingMode.SOUND_AND_VIBRATE) {
                    com.example.data.model.RingMode.VIBRATE_ONLY
                } else {
                    com.example.data.model.RingMode.SOUND_AND_VIBRATE
                }
                habit.copy(ringMode = newMode)
            }
            "vibrate" -> habit.copy(vibrateEnabled = !habit.vibrateEnabled)
            else -> habit
        }
        
        db.habitDao().updateHabit(updatedHabit)
        
        // Update alarm
        val scheduler = com.example.native.AlarmScheduler(context)
        if (updatedHabit.reminderEnabled && !updatedHabit.isArchived) {
            scheduler.scheduleHabitReminder(updatedHabit)
        } else {
            com.example.native.AlarmRingingService.stop(context, updatedHabit.id)
            com.example.native.NotificationHelper(context).cancelNotification(updatedHabit.id)
            scheduler.cancelHabitReminder(updatedHabit.id)
        }
        
        HabitWidget().updateAll(context)
    }
}

