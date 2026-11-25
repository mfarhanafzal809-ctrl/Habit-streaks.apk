import re

file_path = "app/src/main/java/com/example/ui/components/StreakMilestoneCard.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("currentBestStreak: Int,", "progressStreak: Int,")
content = content.replace("currentBestStreak.toFloat()", "progressStreak.toFloat()")
content = content.replace("$currentBestStreak /", "$progressStreak /")

with open(file_path, "w") as f:
    f.write(content)

file_path2 = "app/src/main/java/com/example/ui/screens/StatsScreen.kt"
with open(file_path2, "r") as f:
    content2 = f.read()

content2 = content2.replace("currentBestStreak = currentBestStreak", "progressStreak = bestStreak")

with open(file_path2, "w") as f:
    f.write(content2)
