import re

file_path = "app/src/main/java/com/example/ui/screens/StatsScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace(
    "import androidx.compose.foundation.layout.fillMaxWidth",
    "import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.fillMaxHeight"
)

with open(file_path, "w") as f:
    f.write(content)
