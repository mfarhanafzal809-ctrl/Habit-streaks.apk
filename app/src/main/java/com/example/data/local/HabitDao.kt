package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY sortOrder ASC, createdAt DESC")
    fun getActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY isArchived ASC, sortOrder ASC, createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY isArchived ASC, sortOrder ASC, createdAt DESC")
    suspend fun getAllHabitsList(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): Habit?

    @Query("SELECT * FROM habits WHERE id = :id")
    fun getHabitFlowById(id: Long): Flow<Habit?>

    @Query("SELECT * FROM habits WHERE reminderEnabled = 1 AND isArchived = 0")
    suspend fun getActiveReminders(): List<Habit>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: Habit): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabits(habits: List<Habit>): List<Long>

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("SELECT COUNT(*) FROM habits WHERE isArchived = 0")
    fun getActiveHabitCount(): Flow<Int>

    @Transaction
    suspend fun replaceAllData(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        completionDao: HabitCompletionDao
    ) {
        completionDao.deleteAllCompletions()
        deleteAllHabits()
        val idMap = mutableMapOf<Long, Long>()
        for (habit in habits) {
            val oldId = habit.id
            val newId = insertHabit(habit.copy(id = 0))
            idMap[oldId] = newId
        }
        val remappedCompletions = completions.map { completion ->
            val mappedHabitId = idMap[completion.habitId] ?: completion.habitId
            completion.copy(id = 0, habitId = mappedHabitId)
        }
        completionDao.insertCompletions(remappedCompletions)
    }

    @Transaction
    suspend fun mergeData(
        habits: List<Habit>,
        completions: List<HabitCompletion>,
        completionDao: HabitCompletionDao
    ) {
        val idMap = mutableMapOf<Long, Long>()
        for (habit in habits) {
            val oldId = habit.id
            val newId = insertHabit(habit.copy(id = 0))
            idMap[oldId] = newId
        }
        val remappedCompletions = completions.map { completion ->
            val mappedHabitId = idMap[completion.habitId] ?: completion.habitId
            completion.copy(id = 0, habitId = mappedHabitId)
        }
        completionDao.insertCompletions(remappedCompletions)
    }
}
