package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.HabitCompletionDao
import com.example.data.local.HabitDatabase
import com.example.data.local.HabitDao
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao
) {
    val activeHabits: Flow<List<Habit>> = habitDao.getActiveHabits()
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletion>> = completionDao.getAllCompletions()

    suspend fun getHabitById(id: Long): Habit? = habitDao.getHabitById(id)

    suspend fun insertHabit(habit: Habit): Long = habitDao.insertHabit(habit)

    suspend fun updateHabit(habit: Habit) = habitDao.updateHabit(habit)

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabitById(habitId)
    }

    suspend fun markHabitDone(habitId: Long, dateStr: String, note: String = ""): Long {
        val existing = completionDao.getCompletion(habitId, dateStr)
        if (existing != null) {
            if (note != existing.note && note.isNotBlank()) {
                completionDao.updateNote(habitId, dateStr, note)
            }
            return existing.id
        }
        return completionDao.insertCompletion(
            HabitCompletion(
                habitId = habitId,
                completionDate = dateStr,
                note = note
            )
        )
    }

    suspend fun updateCompletionNote(habitId: Long, dateStr: String, note: String) {
        val existing = completionDao.getCompletion(habitId, dateStr)
        if (existing != null) {
            completionDao.updateNote(habitId, dateStr, note.trim())
        } else {
            completionDao.insertCompletion(
                HabitCompletion(
                    habitId = habitId,
                    completionDate = dateStr,
                    note = note.trim()
                )
            )
        }
    }

    suspend fun undoHabitDone(habitId: Long, dateStr: String) {
        completionDao.deleteCompletion(habitId, dateStr)
    }

    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>> {
        return completionDao.getCompletionsForHabit(habitId)
    }

    suspend fun getCompletionsListForHabit(habitId: Long): List<HabitCompletion> {
        return completionDao.getCompletionsListForHabit(habitId)
    }

    fun getCompletionsByDate(dateStr: String): Flow<List<HabitCompletion>> {
        return completionDao.getCompletionsByDate(dateStr)
    }

    suspend fun getActiveReminders(): List<Habit> = habitDao.getActiveReminders()

    suspend fun getAllHabitsSync(): List<Habit> = habitDao.getAllHabitsList()

    suspend fun getAllCompletionsSync(): List<HabitCompletion> = completionDao.getAllCompletionsList()

    suspend fun replaceAllData(habits: List<Habit>, completions: List<HabitCompletion>) {
        habitDao.replaceAllData(habits, completions, completionDao)
    }

    suspend fun mergeData(habits: List<Habit>, completions: List<HabitCompletion>) {
        habitDao.mergeData(habits, completions, completionDao)
    }
}
