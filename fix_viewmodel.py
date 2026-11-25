import re

file_path = "app/src/main/java/com/example/ui/HabitViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("val overallStats: StateFlow<Triple<Int, Int, Int>> = combine(", "val overallStats: StateFlow<com.example.data.model.OverallStats> = combine(")
content = content.replace("Triple(totalCompletions, bestStreakEver, activeCount)", "com.example.data.model.OverallStats(totalCompletions, bestStreakEver, statsList.maxOfOrNull { it.currentStreak } ?: 0, activeCount)")
content = content.replace("Triple(0, 0, 0)", "com.example.data.model.OverallStats(0, 0, 0, 0)")

with open(file_path, "w") as f:
    f.write(content)
