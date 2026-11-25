package com.example.domain

import android.content.Context
import com.example.data.model.Habit
import com.example.data.model.HabitBackupPayload
import com.example.data.model.HabitCompletion
import com.example.data.repository.HabitRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class BackupImportLimits(
    val maxFileSizeBytes: Long = 5 * 1024 * 1024L, // 5 MB
    val maxHabitsCount: Int = 1000,
    val maxCompletionsCount: Int = 50000,
    val maxNameLength: Int = 100,
    val maxDescriptionLength: Int = 500,
    val maxCategoryLength: Int = 50,
    val maxIconNameLength: Int = 50,
    val maxRingtoneNameLength: Int = 100,
    val maxNoteLength: Int = 500,
    val maxReminderCountPerHabit: Int = 10,
    val maxMetadataEntries: Int = 50,
    val maxMetadataKeyLength: Int = 50,
    val maxMetadataValueLength: Int = 200
)

class BackupManager(
    private val context: Context,
    private val repository: HabitRepository,
    val limits: BackupImportLimits = BackupImportLimits()
) {
    companion object {
        const val CURRENT_BACKUP_VERSION = 1
        private val COLOR_HEX_REGEX = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")
        private val TIME_REGEX = Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(HabitBackupPayload::class.java)

    /**
     * Exports all habits and completions into a validated JSON string.
     */
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val habits = repository.getAllHabitsSync()
        val completions = repository.getAllCompletionsSync()

        val payload = HabitBackupPayload(
            version = CURRENT_BACKUP_VERSION,
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.0.0",
            habits = habits,
            completions = completions,
            metadata = mapOf(
                "device" to android.os.Build.MODEL,
                "habitsCount" to habits.size.toString(),
                "completionsCount" to completions.size.toString()
            )
        )
        adapter.toJson(payload)
    }

    /**
     * Saves exported JSON to app's backup directory with temporary file atomicity,
     * timestamped naming to prevent same-date overwriting, and cleanup of partial files.
     */
    suspend fun exportToFile(): File = withContext(Dispatchers.IO) {
        val json = exportBackupJson()
        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val dateStr = LocalDate.now().toString()
        val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))

        var fileName = "habit_streaks_backup_${dateStr}_$timeStr.json"
        var targetFile = File(backupDir, fileName)
        var counter = 1
        while (targetFile.exists()) {
            fileName = "habit_streaks_backup_${dateStr}_${timeStr}_$counter.json"
            targetFile = File(backupDir, fileName)
            counter++
        }

        val tempFile = File(backupDir, "$fileName.tmp")
        try {
            tempFile.writeText(json)
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            targetFile
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        } finally {
            // Clean up any stale partial .tmp files
            backupDir.listFiles { _, name -> name.endsWith(".tmp") }?.forEach { stale ->
                stale.delete()
            }
        }
    }

    /**
     * Exports all habits and completion logs as standard CSV text compatible with Excel and Google Sheets.
     */
    suspend fun exportToCsv(): String = withContext(Dispatchers.IO) {
        val habits = repository.getAllHabitsSync()
        val completions = repository.getAllCompletionsSync()
        val habitMap = habits.associateBy { it.id }

        val sb = StringBuilder()
        sb.append("Habit Name,Category,Completion Date,Completed Timestamp,Note\n")
        fun escape(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        for (c in completions) {
            val habit = habitMap[c.habitId]
            val habitName = habit?.name ?: "Habit #${c.habitId}"
            val category = habit?.category ?: "General"
            sb.append("${escape(habitName)},${escape(category)},${escape(c.completionDate)},${c.completedAt},${escape(c.note)}\n")
        }
        sb.toString()
    }

    /**
     * Saves CSV export to app's backup directory for sharing.
     */
    suspend fun exportCsvToFile(): File = withContext(Dispatchers.IO) {
        val csv = exportToCsv()
        val backupDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
        val dateStr = LocalDate.now().toString()
        val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
        val targetFile = File(backupDir, "habit_streaks_export_${dateStr}_$timeStr.csv")
        targetFile.writeText(csv)
        targetFile
    }

    /**
     * Parses, validates, and imports a backup stream.
     * Enforces size limits, record counts, field validity, referential integrity, and uniqueness.
     * Executes merge (with ID remapping) or replace atomically within a transaction.
     */
    suspend fun importBackup(
        inputStream: InputStream,
        replaceExisting: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Enforce stream size limit
            val jsonContent = readBoundedStream(inputStream, limits.maxFileSizeBytes)
            if (jsonContent.isBlank()) {
                throw IllegalArgumentException("The backup file is empty.")
            }

            // 2. Parse JSON
            val payload = try {
                adapter.fromJson(jsonContent)
            } catch (e: Exception) {
                val detail = e.message?.let { ": $it" } ?: ""
                throw IllegalArgumentException("Malformed backup JSON$detail")
            } ?: throw IllegalArgumentException("Failed to parse backup format: Root payload is null.")

            // 3. Validate schema version
            if (payload.version !in 1..CURRENT_BACKUP_VERSION) {
                throw IllegalArgumentException("Unsupported backup version (${payload.version}). Supported versions: 1 to $CURRENT_BACKUP_VERSION.")
            }

            // 4. Validate records count and presence
            if (payload.habits.isEmpty() && payload.completions.isEmpty()) {
                throw IllegalArgumentException("Backup contains no habits or completion records.")
            }
            if (payload.habits.size > limits.maxHabitsCount) {
                throw IllegalArgumentException("Backup exceeds maximum habit count limit (${payload.habits.size} > ${limits.maxHabitsCount}).")
            }
            if (payload.completions.size > limits.maxCompletionsCount) {
                throw IllegalArgumentException("Backup exceeds maximum completion count limit (${payload.completions.size} > ${limits.maxCompletionsCount}).")
            }

            // Validate metadata limits
            if (payload.metadata.size > limits.maxMetadataEntries) {
                throw IllegalArgumentException("Backup metadata exceeds maximum allowed entries count (${payload.metadata.size} > ${limits.maxMetadataEntries}).")
            }
            for ((key, value) in payload.metadata) {
                if (key.length > limits.maxMetadataKeyLength || value.length > limits.maxMetadataValueLength) {
                    throw IllegalArgumentException("Backup metadata entry exceeds maximum allowed key/value length.")
                }
            }

            // 5. Validate habit IDs uniqueness and positive values
            val habitIdSet = mutableSetOf<Long>()
            for (habit in payload.habits) {
                if (habit.id <= 0) {
                    throw IllegalArgumentException("Invalid habit ID (${habit.id}). IDs must be positive.")
                }
                if (!habitIdSet.add(habit.id)) {
                    throw IllegalArgumentException("Duplicate habit ID found in backup: ID ${habit.id}.")
                }
                validateHabitFields(habit, limits)
            }

            // 6. Validate completion records (referential integrity, dates, note lengths, duplicate dates)
            val seenCompletions = mutableSetOf<Pair<Long, String>>()
            val seenCompletionIds = mutableSetOf<Long>()
            for (completion in payload.completions) {
                if (completion.id > 0 && !seenCompletionIds.add(completion.id)) {
                    throw IllegalArgumentException("Duplicate completion ID found in backup: ID ${completion.id}.")
                }
                if (completion.habitId !in habitIdSet) {
                    throw IllegalArgumentException("Orphan completion record: References non-existent habit ID ${completion.habitId}.")
                }
                if (StreakEngine.parseDate(completion.completionDate) == null) {
                    throw IllegalArgumentException("Completion record contains invalid date format: ${completion.completionDate}.")
                }
                if (completion.note.length > limits.maxNoteLength) {
                    throw IllegalArgumentException("Completion note exceeds maximum allowed length.")
                }
                val key = completion.habitId to completion.completionDate
                if (!seenCompletions.add(key)) {
                    throw IllegalArgumentException("Duplicate completion record for habit ID ${completion.habitId} on date ${completion.completionDate}.")
                }
            }

            // 7. Atomic transaction execution
            if (replaceExisting) {
                repository.replaceAllData(payload.habits, payload.completions)
            } else {
                repository.mergeData(payload.habits, payload.completions)
            }

            "Successfully restored ${payload.habits.size} habits and ${payload.completions.size} completion entries."
        }
    }

    private fun readBoundedStream(inputStream: InputStream, maxBytes: Long): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalBytes = 0L
        var bytesRead: Int
        inputStream.use { stream ->
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
                if (totalBytes > maxBytes) {
                    throw IllegalArgumentException("Backup file exceeds maximum allowed size of ${maxBytes / (1024 * 1024)} MB.")
                }
                out.write(buffer, 0, bytesRead)
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    private fun validateHabitFields(habit: Habit, limits: BackupImportLimits) {
        if (habit.name.isBlank()) {
            throw IllegalArgumentException("Habit ID ${habit.id} has an empty name.")
        }
        if (habit.name.length > limits.maxNameLength) {
            throw IllegalArgumentException("Habit ID ${habit.id} name exceeds maximum allowed length.")
        }
        if (habit.description.length > limits.maxDescriptionLength) {
            throw IllegalArgumentException("Habit ID ${habit.id} description exceeds maximum allowed length.")
        }
        if (habit.category.length > limits.maxCategoryLength) {
            throw IllegalArgumentException("Habit ID ${habit.id} category exceeds maximum allowed length.")
        }
        if (habit.iconName.length > limits.maxIconNameLength) {
            throw IllegalArgumentException("Habit ID ${habit.id} icon name exceeds maximum allowed length.")
        }
        if (habit.ringtoneName.length > limits.maxRingtoneNameLength) {
            throw IllegalArgumentException("Habit ID ${habit.id} ringtone name exceeds maximum allowed length.")
        }
        if (!COLOR_HEX_REGEX.matches(habit.colorHex)) {
            throw IllegalArgumentException("Habit ID ${habit.id} has invalid color hex format.")
        }
        if (habit.reminderHour !in 0..23 || habit.reminderMinute !in 0..59) {
            throw IllegalArgumentException("Habit ID ${habit.id} has invalid reminder time (${habit.reminderHour}:${habit.reminderMinute}).")
        }
        if (habit.additionalReminderTimes.isNotBlank()) {
            val times = habit.additionalReminderTimes.split(",")
            if (times.size > limits.maxReminderCountPerHabit) {
                throw IllegalArgumentException("Habit ID ${habit.id} exceeds maximum additional reminder count of ${limits.maxReminderCountPerHabit}.")
            }
            for (item in times) {
                val trimmed = item.trim()
                if (trimmed.isNotEmpty() && !TIME_REGEX.matches(trimmed)) {
                    throw IllegalArgumentException("Habit ID ${habit.id} contains invalid reminder time format.")
                }
            }
        }
    }
}
