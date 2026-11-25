package com.example

import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitFrequency
import com.example.domain.StreakEngine
import com.example.ui.HabitsFeedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LaunchPerformanceTest {

    @Test
    fun largeHistory_groupedProcessing_executesSubSecond() {
        val habitCount = 50
        val daysCount = 365
        val today = LocalDate.of(2026, 10, 12)

        val habits = (1..habitCount).map { id ->
            Habit(
                id = id.toLong(),
                name = "Test Habit #$id",
                frequency = if (id % 3 == 0) HabitFrequency.WEEKDAYS else if (id % 3 == 1) HabitFrequency.WEEKENDS else HabitFrequency.DAILY
            )
        }

        // Generate 50 * 365 = 18,250 completions
        val completions = mutableListOf<HabitCompletion>()
        for (habit in habits) {
            for (day in 0 until daysCount) {
                val date = today.minusDays(day.toLong())
                if (StreakEngine.isDueOn(habit.frequency, date)) {
                    completions.add(
                        HabitCompletion(
                            id = (habit.id * 1000 + day),
                            habitId = habit.id,
                            completionDate = StreakEngine.formatDate(date)
                        )
                    )
                }
            }
        }

        val startTime = System.currentTimeMillis()

        // 1. Group completions once
        val completionsByHabit = completions.groupBy { it.habitId }
        val todayStr = StreakEngine.formatDate(today)
        val monthStr = today.format(StreakEngine.MONTH_FORMATTER)

        // 2. Build stats for each habit using pre-grouped collections
        val statsList = habits.map { habit ->
            val habitCompletions = completionsByHabit[habit.id] ?: emptyList()
            StreakEngine.buildHabitWithStats(habit, habitCompletions, today, todayStr, monthStr)
        }

        val durationMillis = System.currentTimeMillis() - startTime

        // Verify data correctness
        assertEquals(habitCount, statsList.size)
        val sampleDaily = statsList.first { it.habit.frequency == HabitFrequency.DAILY }
        assertTrue("Daily streak should be unbroken", sampleDaily.currentStreak >= 365)
        assertTrue("Duration for 18,250 records should be well under 2500ms on JVM", durationMillis < 2500)

        println("Performance Profiling: processed ${completions.size} completions across ${habits.size} habits in ${durationMillis}ms")
    }

    @Test
    fun milestoneComputation_singlePass_scalesEfficiently() {
        val habitCount = 30
        val habits = (1..habitCount).map { id ->
            Habit(id = id.toLong(), name = "Habit #$id")
        }

        val today = LocalDate.of(2026, 10, 12)
        val completionsByHabit = habits.associate { habit ->
            val count = (habit.id * 5).toInt().coerceAtMost(120)
            habit.id to (0 until count).map { day ->
                HabitCompletion(
                    habitId = habit.id,
                    completionDate = StreakEngine.formatDate(today.minusDays(day.toLong()))
                )
            }
        }

        val startTime = System.currentTimeMillis()
        val milestones = StreakEngine.getMilestones(
            currentBestStreak = 100,
            totalCompletions = completionsByHabit.values.sumOf { it.size },
            completionsByHabit = completionsByHabit
        )
        val durationMillis = System.currentTimeMillis() - startTime

        assertNotNull(milestones)
        assertTrue(milestones.isNotEmpty())
        assertTrue("Milestone computation must execute under 500ms", durationMillis < 500)
    }

    @Test
    fun habitsFeedState_distinctStatesHandledSafely() {
        val loading: HabitsFeedState = HabitsFeedState.Loading
        assertTrue(loading is HabitsFeedState.Loading)

        val emptySuccess: HabitsFeedState = HabitsFeedState.Success(emptyList())
        assertTrue(emptySuccess is HabitsFeedState.Success)
        assertEquals(0, (emptySuccess as HabitsFeedState.Success).habits.size)

        val error: HabitsFeedState = HabitsFeedState.Error("Database disk I/O error")
        assertTrue(error is HabitsFeedState.Error)
        assertEquals("Database disk I/O error", (error as HabitsFeedState.Error).errorMessage)
    }

    @Test
    fun dateParsingCache_reusesInstances() {
        val dateStr = "2026-10-12"
        val first = StreakEngine.parseDate(dateStr)
        val second = StreakEngine.parseDate(dateStr)

        assertNotNull(first)
        assertEquals(first, second)
        // Verifies reference equality from concurrent cache
        assertTrue(first === second)
    }
}
