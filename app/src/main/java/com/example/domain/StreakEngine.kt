package com.example.domain

import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitWithStats
import com.example.data.model.StreakMilestone
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object StreakEngine {

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE // "YYYY-MM-DD"
    val MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    private val dateCache = java.util.concurrent.ConcurrentHashMap<String, LocalDate>()

    fun getTodayString(): String {
        return LocalDate.now().format(DATE_FORMATTER)
    }

    fun getYesterdayString(): String {
        return LocalDate.now().minusDays(1).format(DATE_FORMATTER)
    }

    fun formatDate(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }

    fun parseDate(dateStr: String): LocalDate? {
        val cached = dateCache[dateStr]
        if (cached != null) return cached
        val parsed = runCatching { LocalDate.parse(dateStr, DATE_FORMATTER) }.getOrNull()
        if (parsed != null) {
            dateCache[dateStr] = parsed
        }
        return parsed
    }

    fun isDueOn(frequency: com.example.data.model.HabitFrequency, date: LocalDate): Boolean {
        val dayOfWeek = date.dayOfWeek.value // 1 = Monday, ..., 7 = Sunday
        return when (frequency) {
            com.example.data.model.HabitFrequency.DAILY -> true
            com.example.data.model.HabitFrequency.WEEKDAYS -> dayOfWeek in 1..5
            com.example.data.model.HabitFrequency.WEEKENDS -> dayOfWeek in 6..7
        }
    }

    fun previousExpectedDate(frequency: com.example.data.model.HabitFrequency, date: LocalDate): LocalDate {
        var check = date.minusDays(1)
        while (!isDueOn(frequency, check)) {
            check = check.minusDays(1)
        }
        return check
    }

    fun nextExpectedDate(frequency: com.example.data.model.HabitFrequency, date: LocalDate): LocalDate {
        var check = date.plusDays(1)
        while (!isDueOn(frequency, check)) {
            check = check.plusDays(1)
        }
        return check
    }

    /**
     * Calculates the current streak and longest streak for a list of completions
     * accounting for DAILY, WEEKDAYS, or WEEKENDS habit frequencies.
     */
    fun calculateStreaks(
        completions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now(),
        frequency: com.example.data.model.HabitFrequency = com.example.data.model.HabitFrequency.DAILY
    ): Pair<Int, Int> {
        if (completions.isEmpty()) return Pair(0, 0)

        val uniqueDates = completions
            .mapNotNull { parseDate(it.completionDate) }
            .distinct()
            .sortedDescending()

        if (uniqueDates.isEmpty()) return Pair(0, 0)

        val dateSet = uniqueDates.toSet()

        // 1. Calculate Current Streak
        var currentStreak = 0
        val isDueToday = isDueOn(frequency, today)
        val prevExpected = previousExpectedDate(frequency, today)

        val isCompletedToday = dateSet.contains(today)
        val isDonePrev = dateSet.contains(prevExpected)

        if (isCompletedToday) {
            currentStreak = 1
            var checkDate = prevExpected
            while (dateSet.contains(checkDate)) {
                currentStreak++
                checkDate = previousExpectedDate(frequency, checkDate)
            }
        } else if (!isDueToday) {
            // Today is not a due day (e.g. weekend for weekday habit).
            // If the last due date was completed, the streak remains intact.
            if (isDonePrev) {
                var checkDate = prevExpected
                while (dateSet.contains(checkDate)) {
                    currentStreak++
                    checkDate = previousExpectedDate(frequency, checkDate)
                }
            }
        } else {
            // Today is due, but not completed yet. Streak accumulated up to previous expected day holds.
            if (isDonePrev) {
                var checkDate = prevExpected
                while (dateSet.contains(checkDate)) {
                    currentStreak++
                    checkDate = previousExpectedDate(frequency, checkDate)
                }
            }
        }

        // 2. Calculate Longest Streak
        val validAscendingDates = uniqueDates.asReversed().filter { isDueOn(frequency, it) }
        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (date in validAscendingDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else {
                val expectedNext = nextExpectedDate(frequency, prevDate)
                if (date == expectedNext) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
            }
            prevDate = date
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
        }

        return Pair(currentStreak, maxOf(currentStreak, longestStreak))
    }

    fun buildHabitWithStats(
        habit: Habit,
        habitCompletions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now(),
        todayStr: String = formatDate(today),
        currentMonthStr: String = today.format(MONTH_FORMATTER)
    ): HabitWithStats {
        val (currentStreak, longestStreak) = calculateStreaks(habitCompletions, today, habit.frequency)

        val todayCompletion = habitCompletions.firstOrNull { it.completionDate == todayStr }
        val isCompletedToday = todayCompletion != null
        val todayNote = todayCompletion?.note.orEmpty()
        val monthlyCompletions = habitCompletions.count { it.completionDate.startsWith(currentMonthStr) }
        val recentCompletions = habitCompletions.map { it.completionDate }.toSet()

        return HabitWithStats(
            habit = habit,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletions = habitCompletions.size,
            monthlyCompletions = monthlyCompletions,
            isCompletedToday = isCompletedToday,
            recentCompletions = recentCompletions,
            todayNote = todayNote
        )
    }

    fun buildHabitWithStats(
        habit: Habit,
        allCompletions: List<HabitCompletion>,
        today: LocalDate = LocalDate.now()
    ): HabitWithStats {
        val habitCompletions = allCompletions.filter { it.habitId == habit.id }
        return buildHabitWithStats(
            habit = habit,
            habitCompletions = habitCompletions,
            today = today,
            todayStr = formatDate(today),
            currentMonthStr = today.format(MONTH_FORMATTER)
        )
    }

    fun countDueDaysInRange(
        frequency: com.example.data.model.HabitFrequency,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        if (endDate.isBefore(startDate)) return 0
        var count = 0
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            if (isDueOn(frequency, cursor)) count++
            cursor = cursor.plusDays(1)
        }
        return count
    }

    fun getMilestones(
        currentBestStreak: Int,
        totalCompletions: Int,
        completionsByHabit: Map<Long, List<HabitCompletion>> = emptyMap(),
        habitFrequencies: Map<Long, com.example.data.model.HabitFrequency> = emptyMap()
    ): List<StreakMilestone> {
        val maxStreaksByHabit = if (completionsByHabit.isNotEmpty()) {
            completionsByHabit.map { (habitId, completions) ->
                val freq = habitFrequencies[habitId] ?: com.example.data.model.HabitFrequency.DAILY
                calculateStreaks(completions, frequency = freq).second
            }
        } else emptyList()

        fun getAchievedCount(targetDays: Int): Int {
            if (maxStreaksByHabit.isEmpty()) {
                return if (currentBestStreak >= targetDays) 1 else 0
            }
            val count = maxStreaksByHabit.count { it >= targetDays }
            return if (currentBestStreak >= targetDays) count.coerceAtLeast(1) else count
        }

        return listOf(
            StreakMilestone(
                days = 1,
                title = "First Spark",
                description = "Complete your very first day of consistency",
                isUnlocked = currentBestStreak >= 1,
                achievedCount = getAchievedCount(1),
                badgeTier = "INITIATE",
                badgeIconName = "Bolt",
                scienceAffirmation = "The journey has begun! Behavioral science confirms the hardest friction is crossing from zero to one. You did it!",
                primaryColorHex = "#F59E0B",
                secondaryColorHex = "#D97706"
            ),
            StreakMilestone(
                days = 3,
                title = "Spark Starter",
                description = "Complete a 3-day continuous streak",
                isUnlocked = currentBestStreak >= 3,
                achievedCount = getAchievedCount(3),
                badgeTier = "BRONZE",
                badgeIconName = "Flame",
                scienceAffirmation = "The spark is ignited! Overcoming day 3 breaks initial friction and starts conditioning your mental baseline.",
                primaryColorHex = "#F97316",
                secondaryColorHex = "#EA580C"
            ),
            StreakMilestone(
                days = 7,
                title = "Week Warrior",
                description = "Maintain your habit for 7 straight days",
                isUnlocked = currentBestStreak >= 7,
                achievedCount = getAchievedCount(7),
                badgeTier = "SILVER",
                badgeIconName = "Bolt",
                scienceAffirmation = "1 Full Week! Completing an entire 7-day calendar loop establishes solid baseline momentum. You are ahead of 90% of people!",
                primaryColorHex = "#06B6D4",
                secondaryColorHex = "#0284C7"
            ),
            StreakMilestone(
                days = 14,
                title = "Fortnight Focus",
                description = "Hit a solid 14-day unbroken streak",
                isUnlocked = currentBestStreak >= 14,
                achievedCount = getAchievedCount(14),
                badgeTier = "RUBY",
                badgeIconName = "Target",
                scienceAffirmation = "2 Continuous Weeks! You have eliminated 80% of subconscious reluctance. Doing this habit is now second nature.",
                primaryColorHex = "#F43F5E",
                secondaryColorHex = "#BE123C"
            ),
            StreakMilestone(
                days = 21,
                title = "Habit Formed",
                description = "21 days — the scientific neuroplastic threshold",
                isUnlocked = currentBestStreak >= 21,
                achievedCount = getAchievedCount(21),
                badgeTier = "EMERALD",
                badgeIconName = "Eco",
                scienceAffirmation = "The 21-Day Phenomenon! Research shows it takes 21 days for neural synapses to rewire automatic loops. This habit is now part of who you are!",
                primaryColorHex = "#10B981",
                secondaryColorHex = "#047857"
            ),
            StreakMilestone(
                days = 30,
                title = "Monthly Master",
                description = "A full 30-day unstoppable streak",
                isUnlocked = currentBestStreak >= 30,
                achievedCount = getAchievedCount(30),
                badgeTier = "GOLD",
                badgeIconName = "Crown",
                scienceAffirmation = "30 Days of Unstoppable Mastery! An entire calendar month without a single break. You have achieved elite consistency.",
                primaryColorHex = "#FBBF24",
                secondaryColorHex = "#D97706"
            ),
            StreakMilestone(
                days = 50,
                title = "Titan of Habit",
                description = "50 consecutive days of iron dedication",
                isUnlocked = currentBestStreak >= 50,
                achievedCount = getAchievedCount(50),
                badgeTier = "TITANIUM",
                badgeIconName = "Shield",
                scienceAffirmation = "50 Days of Dominance! At 50 days, the habit is effortless and automatic. It defines your daily identity.",
                primaryColorHex = "#6366F1",
                secondaryColorHex = "#4338CA"
            ),
            StreakMilestone(
                days = 75,
                title = "Unstoppable Force",
                description = "75 days of sheer discipline",
                isUnlocked = currentBestStreak >= 75,
                achievedCount = getAchievedCount(75),
                badgeTier = "DIAMOND",
                badgeIconName = "Rocket",
                scienceAffirmation = "75 Days Unstoppable! Less than 2% of people sustain a continuous habit for 75 consecutive days. You are relentless.",
                primaryColorHex = "#A855F7",
                secondaryColorHex = "#7E22CE"
            ),
            StreakMilestone(
                days = 100,
                title = "Century Legend",
                description = "100-day century streak of pure excellence",
                isUnlocked = (currentBestStreak >= 100 || totalCompletions >= 100),
                achievedCount = getAchievedCount(100).coerceAtLeast(totalCompletions / 100),
                badgeTier = "LEGENDARY",
                badgeIconName = "Trophy",
                scienceAffirmation = "The Century Hall of Fame! 100 days of perfection. You have transformed this behavior into a permanent standard of excellence.",
                primaryColorHex = "#EAB308",
                secondaryColorHex = "#B45309"
            ),
            StreakMilestone(
                days = 365,
                title = "The Immortal 365",
                description = "365 consecutive days — 1 entire year",
                isUnlocked = currentBestStreak >= 365,
                achievedCount = getAchievedCount(365),
                badgeTier = "MYTHIC",
                badgeIconName = "Star",
                scienceAffirmation = "Mythic Legend! 365 unbroken days of triumph. A full year of non-stop dedication. You are truly invincible.",
                primaryColorHex = "#EC4899",
                secondaryColorHex = "#8B5CF6"
            )
        )
    }

    fun findMilestoneForStreak(streakDays: Int): StreakMilestone? {
        val all = getMilestones(streakDays, 0)
        return all.find { it.days == streakDays }
    }

    fun getNextMilestone(currentStreak: Int): StreakMilestone? {
        val all = getMilestones(0, 0)
        return all.firstOrNull { it.days > currentStreak }
    }

    fun getMonthDays(yearMonth: YearMonth): List<LocalDate> {
        val daysInMonth = yearMonth.lengthOfMonth()
        return (1..daysInMonth).map { day -> yearMonth.atDay(day) }
    }

    fun calculateMastery(totalCompletions: Int, bestStreak: Int): com.example.data.model.HabitMasteryInfo {
        val xp = (totalCompletions * 25) + (bestStreak * 15)
        val level = (xp / 200).coerceAtLeast(1)
        val title = when (level) {
            in 1..2 -> "Novice Explorer 🌱"
            in 3..5 -> "Habit Apprentice 🌿"
            in 6..10 -> "Habit Enthusiast ⚡"
            in 11..20 -> "Habit Expert 🏅"
            else -> "Habit Master 🚀"
        }
        val xpForNextLevel = 200
        val currentLevelXp = xp % xpForNextLevel
        val progress = currentLevelXp.toFloat() / xpForNextLevel.toFloat()

        return com.example.data.model.HabitMasteryInfo(
            level = level,
            title = title,
            totalXp = xp,
            currentLevelXp = currentLevelXp,
            xpToNextLevel = xpForNextLevel,
            progress = progress
        )
    }
}
