import re

file_path = "app/src/main/java/com/example/ui/HabitViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

# Replace seedSampleDataIfEmpty() call in init block
content = content.replace("seedSampleDataIfEmpty()", "// seedSampleDataIfEmpty() // disabled by request")

with open(file_path, "w") as f:
    f.write(content)
