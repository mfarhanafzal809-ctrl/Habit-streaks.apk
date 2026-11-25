import os
import re

# Fix AppDrawer.kt
app_drawer_path = "app/src/main/java/com/example/ui/components/AppDrawer.kt"
with open(app_drawer_path, "r") as f:
    drawer = f.read()

drawer = drawer.replace("letterSpacing = 2.dp", "letterSpacing = 2.sp")
drawer = drawer.replace("import androidx.compose.foundation.clickable", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.border\nimport androidx.compose.ui.unit.sp")
with open(app_drawer_path, "w") as f:
    f.write(drawer)

# Fix MainActivity.kt
main_activity_path = "app/src/main/java/com/example/MainActivity.kt"
with open(main_activity_path, "r") as f:
    main_activity = f.read()

main_activity = main_activity.replace("onToggleHabitDone = { habitId, date, isDone ->", "onToggleHabitOnDate = { habitId, date, isDone ->")
main_activity = main_activity.replace("allHabits = allHabits,", "habitsWithStats = habitsWithStats,")
main_activity = main_activity.replace("onBackupClick =", "onOpenBackupDialog =")
main_activity = main_activity.replace("onPrivacyPolicyClick =", "onOpenPrivacyPolicy =")
main_activity = main_activity.replace("habitToEdit = editingHabit,", "initialHabit = editingHabit,")
main_activity = main_activity.replace("val context = MainActivity.appContext", "val context = LocalContext.current")

# Add Alignment and LocalContext imports if missing
if "import androidx.compose.ui.Alignment" not in main_activity:
    main_activity = main_activity.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.Alignment")

# There might be a missing parameter for onOpenAbout in SettingsScreen
if "onOpenAbout =" not in main_activity and "onOpenPrivacyPolicy =" in main_activity:
    main_activity = main_activity.replace("onOpenPrivacyPolicy = { currentSubScreen = SubScreen.PRIVACY_POLICY }", "onOpenPrivacyPolicy = { currentSubScreen = SubScreen.PRIVACY_POLICY },\n                                onOpenAbout = { currentSubScreen = SubScreen.ABOUT },\n                                onTestSound = { viewModel.previewSound(null, true) }")

with open(main_activity_path, "w") as f:
    f.write(main_activity)
