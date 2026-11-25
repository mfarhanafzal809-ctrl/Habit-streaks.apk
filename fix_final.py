import os

main_activity_path = "app/src/main/java/com/example/MainActivity.kt"
with open(main_activity_path, "r") as f:
    main_activity = f.read()

# Fix StatsScreen parameter
main_activity = main_activity.replace("habitsWithStats = habitsWithStats,\n                                overallStats = overallStats", "allHabits = allHabits,\n                                overallStats = overallStats")

# Fix LocalContext in lambda
main_activity = main_activity.replace("val context = LocalContext.current\n                context?.contentResolver", "val contentResolver = context.contentResolver\n                contentResolver.openInputStream")

# I need to capture context outside the lambda.
# Where is context defined? It was replaced. Let me define it inside HabitStreaksApp at the top.
# And inside onCustomRingtonePicked, I just use it.
if "val context = LocalContext.current" not in main_activity:
    main_activity = main_activity.replace("val scope = rememberCoroutineScope()", "val scope = rememberCoroutineScope()\n    val context = LocalContext.current")

main_activity = main_activity.replace("val context = LocalContext.current\n                context?.contentResolver?.openInputStream(uri)", "context.contentResolver.openInputStream(uri)")

with open(main_activity_path, "w") as f:
    f.write(main_activity)
