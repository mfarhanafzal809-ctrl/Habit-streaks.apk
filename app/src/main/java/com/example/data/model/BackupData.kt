package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HabitBackupPayload(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
    val metadata: Map<String, String> = emptyMap()
)

data class HabitWithStats(
    val habit: Habit,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val monthlyCompletions: Int,
    val isCompletedToday: Boolean,
    val recentCompletions: Set<String>, // Set of YYYY-MM-DD
    val todayNote: String = ""
)

data class DailyProgress(
    val date: String, // YYYY-MM-DD
    val totalHabits: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class StreakMilestone(
    val days: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val unlockedAtDate: String? = null,
    val achievedCount: Int = 0,
    val badgeTier: String = "BRONZE",
    val badgeIconName: String = "Flame",
    val scienceAffirmation: String = "",
    val primaryColorHex: String = "#FF9800",
    val secondaryColorHex: String = "#FF5722"
)

data class MilestoneCelebration(
    val habitName: String,
    val streakDays: Int,
    val milestone: StreakMilestone,
    val isNewUnlock: Boolean = true,
    val nextMilestoneDays: Int? = null,
    val nextMilestoneTitle: String? = null
)
