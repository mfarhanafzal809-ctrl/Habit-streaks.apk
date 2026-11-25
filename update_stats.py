import re

file_path = "app/src/main/java/com/example/ui/screens/StatsScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace the item block that uses WeeklyInsightChart
target = """        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                WeeklyInsightChart(weeklyData = weeklyData)
            }
        }"""

replacement = """        item {
            com.example.ui.components.WeeklyInsightCard(
                allCompletions = allCompletions
            )
        }"""

content = content.replace(target, replacement)

# Remove the WeeklyInsightChart function
start_index = content.find("@Composable\nfun WeeklyInsightChart")
if start_index != -1:
    content = content[:start_index]

with open(file_path, "w") as f:
    f.write(content)
