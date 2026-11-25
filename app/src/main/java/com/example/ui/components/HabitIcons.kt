package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class HabitIconOption(
    val name: String,
    val label: String,
    val icon: ImageVector
)

val habitIconOptions: List<HabitIconOption> = listOf(
    HabitIconOption("Flame", "Streak", Icons.Default.LocalFireDepartment),
    HabitIconOption("FitnessCenter", "Fitness", Icons.Default.FitnessCenter),
    HabitIconOption("WaterDrop", "Hydrate", Icons.Default.WaterDrop),
    HabitIconOption("Book", "Read", Icons.Default.Book),
    HabitIconOption("SelfImprovement", "Mindful", Icons.Default.SelfImprovement),
    HabitIconOption("Alarm", "Routine", Icons.Default.Alarm),
    HabitIconOption("Bolt", "Energy", Icons.Default.Bolt),
    HabitIconOption("Spa", "Calm", Icons.Default.Spa),
    HabitIconOption("Work", "Focus", Icons.Default.Work),
    HabitIconOption("Favorite", "Health", Icons.Default.Favorite),
    HabitIconOption("MusicNote", "Practice", Icons.Default.MusicNote),
    HabitIconOption("Star", "Goal", Icons.Default.Star)
)

private val templateIconAliases: Map<String, String> = mapOf(
    "bedtime" to "Alarm",
    "donotdisturb" to "Bolt",
    "healthandsafety" to "Favorite",
    "directionswalk" to "FitnessCenter",
    "editnote" to "Book",
    "phoneandroid" to "Work",
    "menubook" to "Book",
    "translate" to "MusicNote",
    "psychology" to "SelfImprovement",
    "hourglasstop" to "Work",
    "checklist" to "Alarm",
    "cleaningservices" to "Spa"
)

fun normalizeHabitIconName(iconName: String?): String {
    val key = iconName?.trim()?.lowercase().orEmpty()
    val canonical = templateIconAliases[key] ?: iconName
    return habitIconOptions.firstOrNull { it.name.equals(canonical, ignoreCase = true) }?.name ?: "Flame"
}

fun getHabitIconVector(iconName: String?): ImageVector {
    val resolved = normalizeHabitIconName(iconName)
    return habitIconOptions.firstOrNull { it.name.equals(resolved, ignoreCase = true) }?.icon
        ?: Icons.Default.LocalFireDepartment
}

fun getHabitIconLabel(iconName: String?): String {
    val resolved = normalizeHabitIconName(iconName)
    return habitIconOptions.firstOrNull { it.name.equals(resolved, ignoreCase = true) }?.label
        ?: "Streak"
}
