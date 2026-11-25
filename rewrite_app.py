import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# I will find the @Composable fun HabitStreaksApp... and replace it entirely.
match = re.search(r'@Composable\nfun HabitStreaksApp.*', content, flags=re.DOTALL)
if match:
    content = content[:match.start()]

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
