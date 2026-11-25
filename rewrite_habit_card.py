import re

with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'r') as f:
    content = f.read()

# I want to find the whole Row containing "Habit Name and Checkbox Row" and replace the checkbox
match = re.search(r'// Habit Name and Checkbox Row.*?Spacer\(modifier = Modifier\.height\(16\.dp\)\)', content, flags=re.DOTALL)
if match:
    old_code = match.group(0)
    
    new_code = """// Habit Name and Action Buttons Row
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                                    
                if (habit.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Powerful Done / Undo Action Buttons
                if (isDone) {
                    Button(
                        onClick = { onToggleDone(isDone) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("habit_toggle_button_${habit.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Undo Completed", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onToggleDone(isDone) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("habit_toggle_button_${habit.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = habitColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Mark Done")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MARK DONE", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))"""
            
    content = content.replace(old_code, new_code)
    
    with open('app/src/main/java/com/example/ui/components/HabitCard.kt', 'w') as f:
        f.write(content)
