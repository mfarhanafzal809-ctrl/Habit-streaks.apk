import re
with open('app/src/main/java/com/example/ui/components/ThemeCustomizationBottomSheet.kt', 'r') as f:
    content = f.read()

content = content.replace("androidx.compose.material.icons.Icons.Default.LightMode", "Icons.Default.WbSunny")
content = content.replace("androidx.compose.material.icons.Icons.Default.DarkMode", "Icons.Default.NightlightRound")

if "import androidx.compose.material.icons.filled.WbSunny" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Check", "import androidx.compose.material.icons.filled.Check\nimport androidx.compose.material.icons.filled.WbSunny\nimport androidx.compose.material.icons.filled.NightlightRound")

with open('app/src/main/java/com/example/ui/components/ThemeCustomizationBottomSheet.kt', 'w') as f:
    f.write(content)
