import re

file_path = "app/src/main/java/com/example/widget/HabitWidget.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("ColorProvider(Color.White)", "ColorProvider(day = Color.White, night = Color.White)")
content = content.replace("ColorProvider(Color.LightGray)", "ColorProvider(day = Color.LightGray, night = Color.LightGray)")
content = content.replace("ColorProvider(if (isDone) Color.Gray else Color.White)", "ColorProvider(day = if (isDone) Color.Gray else Color.White, night = if (isDone) Color.Gray else Color.White)")
content = content.replace("ColorProvider(day = day = ", "ColorProvider(day = ") # just in case

with open(file_path, "w") as f:
    f.write(content)
