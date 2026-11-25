package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

/**
 * Ring mode for habit reminders
 */
enum class RingMode {
    SOUND_AND_VIBRATE,
    VIBRATE_ONLY,
    SILENT
}

/**
 * Frequency of the habit
 */
enum class HabitFrequency {
    DAILY,
    WEEKDAYS,
    WEEKENDS
}

@Entity(tableName = "habits")
@JsonClass(generateAdapter = true)
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Health",
    val colorHex: String = "#FF6B4A", // Vibrant orange
    val iconName: String = "Flame",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    
    // Reminder Settings
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,    // 0-23
    val reminderMinute: Int = 0,  // 0-59
    val ringMode: RingMode = RingMode.SOUND_AND_VIBRATE,
    val ringtoneUri: String? = null, // null = default system/bundled ringtone
    val ringtoneName: String = "Default Bell",
    val vibrateEnabled: Boolean = true,
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val additionalReminderTimes: String = "" // Comma-separated extra times "HH:MM,HH:MM"
) {
    val safeReminderHour: Int get() = reminderHour.coerceIn(0, 23)
    val safeReminderMinute: Int get() = reminderMinute.coerceIn(0, 59)

    fun getAllReminderTimes(): List<Pair<Int, Int>> {
        val list = mutableListOf(Pair(safeReminderHour, safeReminderMinute))
        if (additionalReminderTimes.isNotBlank()) {
            additionalReminderTimes.split(",").forEach { item ->
                val parts = item.trim().split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    if (h != null && m != null && h in 0..23 && m in 0..59) {
                        list.add(Pair(h, m))
                    }
                }
            }
        }
        return list.distinct()
    }
}

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "completionDate"], unique = true)
    ]
)
@JsonClass(generateAdapter = true)
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val completionDate: String, // Format: "YYYY-MM-DD" in user's local timezone
    val completedAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey
    val key: String,
    val value: String
)

data class HabitMasteryInfo(
    val level: Int = 1,
    val title: String = "Novice Explorer 🌱",
    val totalXp: Int = 0,
    val currentLevelXp: Int = 0,
    val xpToNextLevel: Int = 200,
    val progress: Float = 0f
)
