import re

file_path = "app/src/main/java/com/example/ui/HabitViewModel.kt"
with open(file_path, "r") as f:
    content = f.read()

content = content.replace("import com.example.domain.StreakEngine", "import com.example.domain.StreakEngine\nimport androidx.glance.appwidget.updateAll\nimport com.example.widget.HabitWidget")

target_toggle = """            if (isDone) {
                repository.undoHabitDone(habitId, todayStr)
            } else {
                repository.markHabitDone(habitId, todayStr)
                soundPlayer.triggerVibration()
                AdManager.recordMeaningfulAction()
            }"""

replacement_toggle = """            if (isDone) {
                repository.undoHabitDone(habitId, todayStr)
            } else {
                repository.markHabitDone(habitId, todayStr)
                soundPlayer.triggerVibration()
                AdManager.recordMeaningfulAction()
            }
            try {
                HabitWidget().updateAll(application)
            } catch(e: Exception) {
                e.printStackTrace()
            }"""

content = content.replace(target_toggle, replacement_toggle)

with open(file_path, "w") as f:
    f.write(content)
