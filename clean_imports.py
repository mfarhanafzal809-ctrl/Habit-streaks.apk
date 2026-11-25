import re

with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'r') as f:
    content = f.read()

# Replace any occurrence of 'import ...import ...' with newlines
content = re.sub(r'(import [a-zA-Z0-9_.]+)(import)', r'\1\n\2', content)
content = re.sub(r'(import [a-zA-Z0-9_.]+)(import)', r'\1\n\2', content)
content = re.sub(r'(import [a-zA-Z0-9_.]+)(import)', r'\1\n\2', content)

with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'w') as f:
    f.write(content)
