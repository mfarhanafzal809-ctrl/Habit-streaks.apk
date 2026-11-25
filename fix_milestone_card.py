import re

file_path = "app/src/main/java/com/example/ui/components/StreakMilestoneCard.kt"
with open(file_path, "r") as f:
    content = f.read()

target = """                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )"""

replacement = """                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (milestone.achievedCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents, 
                            contentDescription = null, 
                            tint = HabitAmber, 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Achieved ${milestone.achievedCount} times",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = HabitAmber
                        )
                    }
                }"""

content = content.replace(target, replacement)

with open(file_path, "w") as f:
    f.write(content)
