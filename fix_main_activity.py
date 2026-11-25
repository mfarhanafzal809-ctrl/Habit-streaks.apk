import re

file_path = "app/src/main/java/com/example/MainActivity.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("val habitsWithStats by viewModel.habitsWithStats.collectAsStateWithLifecycle()", "val habitsWithStats by viewModel.habitsWithStats.collectAsStateWithLifecycle()\n    val allCompletions by viewModel.allCompletions.collectAsStateWithLifecycle()")

content = content.replace("""                            StatsScreen(
                                habitsWithStats = habitsWithStats,
                                overallStats = overallStats
                            )""", """                            StatsScreen(
                                habitsWithStats = habitsWithStats,
                                overallStats = overallStats,
                                allCompletions = allCompletions
                            )""")

with open(file_path, "w") as f:
    f.write(content)
