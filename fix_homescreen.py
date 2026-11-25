import re

file_path = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("overallStats: Triple<Int, Int, Int>,", "overallStats: com.example.data.model.OverallStats,")
content = content.replace("val (totalCompletions, bestStreak, _) = overallStats", "val totalCompletions = overallStats.totalCompletions\n    val bestStreak = overallStats.bestStreakEver")

with open(file_path, "w") as f:
    f.write(content)
