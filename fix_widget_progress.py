import re

file_path = "app/src/main/java/com/example/widget/HabitWidget.kt"
with open(file_path, "r") as f:
    content = f.read()

target = """        val todayStr = StreakEngine.getTodayString()
        
        val habitsWithStats = habits.map { habit ->
            val isDone = allCompletions.any { it.habitId == habit.id && it.completionDate == todayStr }
            Pair(habit, isDone)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(habitsWithStats)
            }
        }
    }

    @Composable
    private fun WidgetContent(habits: List<Pair<Habit, Boolean>>) {"""

replacement = """        val todayStr = StreakEngine.getTodayString()
        val currentMonthPrefix = todayStr.substring(0, 7)
        val thisMonthCompletions = allCompletions.count { it.completionDate.startsWith(currentMonthPrefix) }
        val daysInMonth = java.time.YearMonth.now().lengthOfMonth()
        val totalExpectedThisMonth = habits.size * daysInMonth
        val monthlyProgress = if (totalExpectedThisMonth > 0) thisMonthCompletions.toFloat() / totalExpectedThisMonth else 0f
        
        val habitsWithStats = habits.map { habit ->
            val isDone = allCompletions.any { it.habitId == habit.id && it.completionDate == todayStr }
            Pair(habit, isDone)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(habitsWithStats, monthlyProgress)
            }
        }
    }

    @Composable
    private fun WidgetContent(habits: List<Pair<Habit, Boolean>>, monthlyProgress: Float) {"""

content = content.replace(target, replacement)

target2 = """            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Habit Streaks",
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }"""

replacement2 = """            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Habit Streaks",
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
            
            // Custom Progress Bar via Glance Layout Weights
            val safeProgress = if (monthlyProgress < 0.01f) 0.01f else if (monthlyProgress > 0.99f) 0.99f else monthlyProgress
            val inverseProgress = 1f - safeProgress
            
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Progress",
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(day = Color.LightGray, night = Color.LightGray),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "${(monthlyProgress * 100).toInt()}%",
                    style = TextStyle(
                        color = androidx.glance.color.ColorProvider(day = Color(0xFF4F46E5), night = Color(0xFF4F46E5)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            androidx.glance.appwidget.LinearProgressIndicator(
                progress = monthlyProgress,
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 12.dp),
                color = androidx.glance.color.ColorProvider(day = Color(0xFF4F46E5), night = Color(0xFF4F46E5)),
                backgroundColor = androidx.glance.color.ColorProvider(day = Color(0xFF3A3A4C), night = Color(0xFF3A3A4C))
            )
"""
content = content.replace(target2, replacement2)

with open(file_path, "w") as f:
    f.write(content)
