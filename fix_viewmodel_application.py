import re

file_path = "app/src/main/java/com/example/ui/HabitViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("HabitWidget().updateAll(application)", "HabitWidget().updateAll(getApplication<Application>())")

with open(file_path, "w") as f:
    f.write(content)
