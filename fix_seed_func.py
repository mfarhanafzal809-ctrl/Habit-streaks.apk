import re

file_path = "app/src/main/java/com/example/ui/HabitViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("private fun // seedSampleDataIfEmpty() // disabled by request {", "private fun seedSampleDataIfEmpty() {")

with open(file_path, "w") as f:
    f.write(content)
