with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith('import androidx.compose.material3.Buttonimport'):
        new_lines.append('import androidx.compose.material3.Button\n')
        new_lines.append('import androidx.compose.material3.ButtonDefaults\n')
        new_lines.append('import androidx.compose.material3.IconButton\n')
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'w') as f:
    f.writelines(new_lines)
