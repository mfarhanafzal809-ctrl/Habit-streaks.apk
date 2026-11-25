import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Replace everything from "@Composable\nfun HabitStreaksApp" to the end of the file.
new_content = re.sub(r'@Composable\nfun HabitStreaksApp.*', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(new_content)
