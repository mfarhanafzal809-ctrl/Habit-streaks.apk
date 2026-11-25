import re

file_path = "app/src/main/java/com/example/ui/screens/StatsScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Add the calculation block right before `val currentMonth = YearMonth.now()`
target_calc = "val currentMonth = YearMonth.now()"
replacement_calc = """val today = LocalDate.now()
    val weeklyData = (6 downTo 0).map { i ->
        val d = today.minusDays(i.toLong())
        val dateStr = StreakEngine.formatDate(d)
        val count = allCompletions.count { it.completionDate == dateStr }
        Pair(d.dayOfWeek.name.take(3), count)
    }
    val currentMonth = YearMonth.now()"""
content = content.replace(target_calc, replacement_calc)

# Add the UI section right after the 2x2 grid
target_ui = """        // Section Title: Milestones"""
replacement_ui = """        // Section Title: Weekly Insight
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Weekly Insight",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                WeeklyInsightChart(weeklyData = weeklyData)
            }
        }

        // Section Title: Milestones"""
content = content.replace(target_ui, replacement_ui)

# Add the custom Composable at the end of the file
weekly_insight_composable = """
@Composable
fun WeeklyInsightChart(
    weeklyData: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxCount = weeklyData.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyData.forEachIndexed { index, data ->
            val (dayName, count) = data
            val heightFraction = count.toFloat() / maxCount.toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight(heightFraction.coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(if (index == 6) PrimaryIndigo else PrimaryIndigo.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Label
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (index == 6) FontWeight.Bold else FontWeight.Normal),
                    color = if (index == 6) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
"""
content = content + weekly_insight_composable

with open(file_path, "w") as f:
    f.write(content)
