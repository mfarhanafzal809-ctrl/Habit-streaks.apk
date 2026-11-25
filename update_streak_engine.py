import re

file_path = "app/src/main/java/com/example/domain/StreakEngine.kt"
with open(file_path, "r") as f:
    content = f.read()

target = """    fun getMilestones(longestStreak: Int, totalCompletions: Int): List<StreakMilestone> {
        val milestoneList = listOf(
            StreakMilestone(3, "Spark Starter", "Complete a 3-day continuous streak", longestStreak >= 3),
            StreakMilestone(7, "Week Warrior", "Maintain your habit for 7 straight days", longestStreak >= 7),
            StreakMilestone(14, "Fortnight Focus", "Hit a solid 14-day streak", longestStreak >= 14),
            StreakMilestone(21, "Habit Formed", "21 days - science of building a real habit", longestStreak >= 21),
            StreakMilestone(30, "Monthly Master", "A full 30-day unstoppable streak", longestStreak >= 30),
            StreakMilestone(60, "Iron Discipline", "60 consecutive days of pure dedication", longestStreak >= 60),
            StreakMilestone(100, "Century Legend", "100 total habit completions recorded", totalCompletions >= 100)
        )
        return milestoneList
    }"""

replacement = """    fun countMilestoneAchievements(completionsByHabit: Map<Long, List<HabitCompletion>>, targetDays: Int): Int {
        var totalAchievements = 0
        for ((_, completions) in completionsByHabit) {
            val uniqueDates = completions.mapNotNull { parseDate(it.completionDate) }.distinct().sorted()
            var tempStreak = 0
            var prevDate: LocalDate? = null
            for (date in uniqueDates) {
                if (prevDate == null) {
                    tempStreak = 1
                } else {
                    val diffDays = ChronoUnit.DAYS.between(prevDate, date)
                    if (diffDays == 1L) {
                        tempStreak++
                    } else if (diffDays > 1L) {
                        tempStreak = 1
                    }
                }
                if (tempStreak == targetDays) {
                    totalAchievements++
                }
                prevDate = date
            }
        }
        return totalAchievements
    }

    fun getMilestones(currentBestStreak: Int, totalCompletions: Int, completionsByHabit: Map<Long, List<HabitCompletion>> = emptyMap()): List<StreakMilestone> {
        val milestoneList = listOf(
            StreakMilestone(3, "Spark Starter", "Complete a 3-day continuous streak", currentBestStreak >= 3, null, countMilestoneAchievements(completionsByHabit, 3)),
            StreakMilestone(7, "Week Warrior", "Maintain your habit for 7 straight days", currentBestStreak >= 7, null, countMilestoneAchievements(completionsByHabit, 7)),
            StreakMilestone(14, "Fortnight Focus", "Hit a solid 14-day streak", currentBestStreak >= 14, null, countMilestoneAchievements(completionsByHabit, 14)),
            StreakMilestone(21, "Habit Formed", "21 days - science of building a real habit", currentBestStreak >= 21, null, countMilestoneAchievements(completionsByHabit, 21)),
            StreakMilestone(30, "Monthly Master", "A full 30-day unstoppable streak", currentBestStreak >= 30, null, countMilestoneAchievements(completionsByHabit, 30)),
            StreakMilestone(60, "Iron Discipline", "60 consecutive days of pure dedication", currentBestStreak >= 60, null, countMilestoneAchievements(completionsByHabit, 60)),
            StreakMilestone(100, "Century Legend", "100 total habit completions recorded", totalCompletions >= 100, null, totalCompletions / 100)
        )
        return milestoneList
    }"""

content = content.replace(target, replacement)
with open(file_path, "w") as f:
    f.write(content)
