package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completionDate DESC")
    fun getCompletionsForHabit(habitId: Long): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completionDate DESC")
    suspend fun getCompletionsListForHabit(habitId: Long): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions ORDER BY completionDate DESC")
    fun getAllCompletions(): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions ORDER BY completionDate DESC")
    suspend fun getAllCompletionsList(): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE completionDate = :date")
    fun getCompletionsByDate(date: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE completionDate = :date")
    suspend fun getCompletionsListByDate(date: String): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE completionDate LIKE :yearMonth || '%'")
    fun getCompletionsByMonth(yearMonth: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completionDate = :date LIMIT 1")
    suspend fun getCompletion(habitId: Long, date: String): HabitCompletion?

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completionDate = :date LIMIT 1")
    fun getCompletionSync(habitId: Long, date: String): HabitCompletion?

    @Query("SELECT COUNT(*) > 0 FROM habit_completions WHERE habitId = :habitId AND completionDate = :date")
    fun isHabitCompletedOnDate(habitId: Long, date: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletion(completion: HabitCompletion): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletions(completions: List<HabitCompletion>): List<Long>

    @Query("UPDATE habit_completions SET note = :note WHERE habitId = :habitId AND completionDate = :date")
    suspend fun updateNote(habitId: Long, date: String, note: String): Int

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND completionDate = :date")
    suspend fun deleteCompletion(habitId: Long, date: String): Int

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsForHabit(habitId: Long): Int

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAllCompletions()
}
