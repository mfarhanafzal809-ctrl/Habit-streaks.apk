package com.example

import com.example.data.model.HabitCompletion
import com.example.domain.StreakEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakEngineTest {

    @Test
    fun calculateStreaks_emptyCompletions_returnsZero() {
        val (current, longest) = StreakEngine.calculateStreaks(emptyList())
        assertEquals(0, current)
        assertEquals(0, longest)
    }

    @Test
    fun calculateStreaks_singleTodayCompletion_returnsStreakOne() {
        val today = LocalDate.of(2026, 10, 6)
        val completions = listOf(
            HabitCompletion(habitId = 1, completionDate = "2026-10-06")
        )
        val (current, longest) = StreakEngine.calculateStreaks(completions, today)
        assertEquals(1, current)
        assertEquals(1, longest)
    }

    @Test
    fun calculateStreaks_consecutiveDays_returnsCorrectStreak() {
        val today = LocalDate.of(2026, 10, 6)
        val completions = listOf(
            HabitCompletion(habitId = 1, completionDate = "2026-10-06"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-05"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-04"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-03"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-02"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-01"),
            HabitCompletion(habitId = 1, completionDate = "2026-09-30")
        )
        val (current, longest) = StreakEngine.calculateStreaks(completions, today)
        assertEquals(7, current)
        assertEquals(7, longest)
    }

    @Test
    fun calculateStreaks_brokenStreak_returnsCurrentAndLongest() {
        val today = LocalDate.of(2026, 10, 6)
        val completions = listOf(
            HabitCompletion(habitId = 1, completionDate = "2026-10-06"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-05"),
            // Gap on 10-04
            HabitCompletion(habitId = 1, completionDate = "2026-10-03"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-02"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-01"),
            HabitCompletion(habitId = 1, completionDate = "2026-09-30")
        )
        val (current, longest) = StreakEngine.calculateStreaks(completions, today)
        assertEquals(2, current)
        assertEquals(4, longest)
    }

    @Test
    fun milestones_day1To365_definedAndOrdered() {
        val milestones = StreakEngine.getMilestones(currentBestStreak = 7, totalCompletions = 20)
        assertTrue(milestones.isNotEmpty())
        assertEquals(1, milestones.first().days)
        assertEquals(365, milestones.last().days)

        val day7 = milestones.find { it.days == 7 }
        assertNotNull(day7)
        assertTrue(day7!!.isUnlocked)

        val day30 = milestones.find { it.days == 30 }
        assertNotNull(day30)
        assertFalse(day30!!.isUnlocked)
    }

    @Test
    fun getNextMilestone_returnsNextThreshold() {
        val nextAfter0 = StreakEngine.getNextMilestone(0)
        assertNotNull(nextAfter0)
        assertEquals(1, nextAfter0!!.days)

        val nextAfter1 = StreakEngine.getNextMilestone(1)
        assertNotNull(nextAfter1)
        assertEquals(3, nextAfter1!!.days)

        val nextAfter7 = StreakEngine.getNextMilestone(7)
        assertNotNull(nextAfter7)
        assertEquals(14, nextAfter7!!.days)

        val nextAfter365 = StreakEngine.getNextMilestone(365)
        assertNull(nextAfter365)
    }

    @Test
    fun calculateStreaks_missingTodayButCompletedYesterday_currentStreakIsActiveFromYesterday() {
        val today = LocalDate.of(2026, 10, 6)
        val completions = listOf(
            HabitCompletion(habitId = 1, completionDate = "2026-10-05"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-04"),
            HabitCompletion(habitId = 1, completionDate = "2026-10-03")
        )
        val (current, longest) = StreakEngine.calculateStreaks(completions, today)
        assertEquals(3, current)
        assertEquals(3, longest)
    }

    @Test
    fun getMilestones_respectsHabitFrequenciesForAchievedCount() {
        // Mon Nov 2 .. Tue Nov 10 (7 weekdays spanning Nov 7-8 weekend)
        val weekdayDates = listOf(
            "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06",
            "2026-11-09", "2026-11-10"
        )
        val completions = weekdayDates.map { HabitCompletion(habitId = 42L, completionDate = it) }
        val milestones = StreakEngine.getMilestones(
            currentBestStreak = 7,
            totalCompletions = completions.size,
            completionsByHabit = mapOf(42L to completions),
            habitFrequencies = mapOf(42L to com.example.data.model.HabitFrequency.WEEKDAYS)
        )
        val day7 = milestones.first { it.days == 7 }
        assertTrue(day7.isUnlocked)
        assertEquals(1, day7.achievedCount)
    }

    @Test
    fun countDueDaysInRange_respectsFrequencyAndBounds() {
        // Mon 2026-11-02 through Sun 2026-11-08 = 7 days (5 weekdays, 2 weekend days)
        val start = LocalDate.of(2026, 11, 2)
        val end = LocalDate.of(2026, 11, 8)
        assertEquals(7, StreakEngine.countDueDaysInRange(com.example.data.model.HabitFrequency.DAILY, start, end))
        assertEquals(5, StreakEngine.countDueDaysInRange(com.example.data.model.HabitFrequency.WEEKDAYS, start, end))
        assertEquals(2, StreakEngine.countDueDaysInRange(com.example.data.model.HabitFrequency.WEEKENDS, start, end))
        assertEquals(0, StreakEngine.countDueDaysInRange(com.example.data.model.HabitFrequency.DAILY, end, start))
    }

    @Test
    fun buildHabitWithStats_populatesTodayNote() {
        val today = LocalDate.of(2026, 11, 10)
        val habit = com.example.data.model.Habit(id = 7L, name = "Journal")
        val completions = listOf(
            HabitCompletion(habitId = 7L, completionDate = "2026-11-10", note = "Felt energized!"),
            HabitCompletion(habitId = 7L, completionDate = "2026-11-09", note = "Yesterday note")
        )
        val stats = StreakEngine.buildHabitWithStats(habit, completions, today)
        assertTrue(stats.isCompletedToday)
        assertEquals("Felt energized!", stats.todayNote)
    }
}

