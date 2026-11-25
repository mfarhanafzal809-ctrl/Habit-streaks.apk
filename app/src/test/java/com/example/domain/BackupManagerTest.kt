package com.example.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HabitDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitBackupPayload
import com.example.data.model.HabitCompletion
import com.example.data.repository.HabitRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: HabitDatabase
    private lateinit var repository: HabitRepository
    private lateinit var backupManager: BackupManager
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(HabitBackupPayload::class.java)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = HabitRepository(db.habitDao(), db.habitCompletionDao())
        backupManager = BackupManager(context, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testOversizedFileThrowsException() = runBlocking {
        val smallLimitsManager = BackupManager(
            context,
            repository,
            limits = BackupImportLimits(maxFileSizeBytes = 100)
        )
        val largeData = "A".repeat(500).toByteArray()
        val stream = ByteArrayInputStream(largeData)

        val result = smallLimitsManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("maximum allowed size") == true)
    }

    @Test
    fun testMalformedJsonThrowsException() = runBlocking {
        val malformedJson = "{ \"version\": 1, \"habits\": [ { incomplete json"
        val stream = ByteArrayInputStream(malformedJson.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Malformed backup JSON") == true)
    }

    @Test
    fun testUnsupportedVersionThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 99,
            habits = listOf(Habit(id = 1, name = "Habit 1")),
            completions = emptyList()
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported backup version") == true)
    }

    @Test
    fun testDuplicateHabitIdsThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(
                Habit(id = 1, name = "Habit A"),
                Habit(id = 1, name = "Habit B")
            ),
            completions = emptyList()
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Duplicate habit ID") == true)
    }

    @Test
    fun testInvalidHabitIdThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = -5, name = "Negative ID Habit")),
            completions = emptyList()
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Invalid habit ID") == true)
    }

    @Test
    fun testBlankHabitNameThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "   ")),
            completions = emptyList()
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("empty name") == true)
    }

    @Test
    fun testInvalidColorHexThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Color Habit", colorHex = "NOT_A_COLOR")),
            completions = emptyList()
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid color hex format") == true)
    }

    @Test
    fun testInvalidReminderTimeThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Alarm Habit", reminderHour = 26)),
            completions = emptyList()
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid reminder time") == true)
    }

    @Test
    fun testInvalidAdditionalReminderTimeThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Alarm Habit", additionalReminderTimes = "25:99")),
            completions = emptyList()
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("invalid reminder time format") == true)
    }

    @Test
    fun testOrphanCompletionThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Valid Habit")),
            completions = listOf(
                HabitCompletion(id = 1, habitId = 999, completionDate = "2026-10-11")
            )
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Orphan completion record") == true)
    }

    @Test
    fun testDuplicateCompletionRecordThrowsException() = runBlocking {
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Valid Habit")),
            completions = listOf(
                HabitCompletion(id = 1, habitId = 1, completionDate = "2026-10-11"),
                HabitCompletion(id = 2, habitId = 1, completionDate = "2026-10-11")
            )
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Duplicate completion record") == true)
    }

    @Test
    fun testMetadataLimitsEnforced() = runBlocking {
        val largeMetadata = (1..60).associate { "key_$it" to "value_$it" }
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Valid Habit")),
            completions = emptyList(),
            metadata = largeMetadata
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("metadata exceeds maximum") == true)
    }

    @Test
    fun testMergeAllocatesCollisionFreeIdsAndPreservesExistingHabits() = runBlocking {
        // 1. Insert existing local habit with id = 1
        val localHabitId = repository.insertHabit(Habit(name = "Existing Local Habit"))
        assertEquals(1L, localHabitId)

        // 2. Prepare imported payload where habit also has id = 1
        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Imported Habit")),
            completions = listOf(
                HabitCompletion(id = 10, habitId = 1, completionDate = "2026-10-01", note = "Imported Note")
            )
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        // 3. Perform merge
        val result = backupManager.importBackup(stream, replaceExisting = false)
        assertTrue(result.isSuccess)

        // 4. Assert local habit 1 was NOT overwritten
        val localHabit = repository.getHabitById(1L)
        assertNotNull(localHabit)
        assertEquals("Existing Local Habit", localHabit!!.name)

        // 5. Assert all habits count is 2 (local + imported)
        val allHabits = repository.getAllHabitsSync()
        assertEquals(2, allHabits.size)

        val importedHabit = allHabits.first { it.name == "Imported Habit" }
        assertNotEquals(1L, importedHabit.id)

        // 6. Assert completion was correctly remapped to the new habit ID
        val completions = repository.getAllCompletionsSync()
        assertEquals(1, completions.size)
        assertEquals(importedHabit.id, completions[0].habitId)
        assertEquals("Imported Note", completions[0].note)
    }

    @Test
    fun testReplaceAllDataOverwritesCompletely() = runBlocking {
        repository.insertHabit(Habit(name = "Old Habit"))
        assertEquals(1, repository.getAllHabitsSync().size)

        val payload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "New Replaced Habit")),
            completions = listOf(
                HabitCompletion(id = 1, habitId = 1, completionDate = "2026-10-05")
            )
        )
        val json = adapter.toJson(payload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream, replaceExisting = true)
        assertTrue(result.isSuccess)

        val allHabits = repository.getAllHabitsSync()
        assertEquals(1, allHabits.size)
        assertEquals("New Replaced Habit", allHabits[0].name)

        val allCompletions = repository.getAllCompletionsSync()
        assertEquals(1, allCompletions.size)
    }

    @Test
    fun testInterruptedExportCleansUpTempFilesAndAvoidsCollision() = runBlocking {
        repository.insertHabit(Habit(name = "Export Habit"))

        val file1 = backupManager.exportToFile()
        assertTrue(file1.exists())

        val file2 = backupManager.exportToFile()
        assertTrue(file2.exists())

        // Must not overwrite each other
        assertNotEquals(file1.absolutePath, file2.absolutePath)

        // Assert no lingering .tmp files
        val backupDir = File(context.filesDir, "backups")
        val tmpFiles = backupDir.listFiles { _, name -> name.endsWith(".tmp") }
        assertTrue(tmpFiles.isNullOrEmpty())
    }

    @Test
    fun testFailedValidationLeavesDatabaseUntouched() = runBlocking {
        repository.insertHabit(Habit(name = "Untouched Habit"))
        assertEquals(1, repository.getAllHabitsSync().size)

        // Payload with invalid color format fails before any database write
        val invalidPayload = HabitBackupPayload(
            version = 1,
            habits = listOf(Habit(id = 1, name = "Bad Habit", colorHex = "INVALID")),
            completions = emptyList()
        )
        val json = adapter.toJson(invalidPayload)
        val stream = ByteArrayInputStream(json.toByteArray())

        val result = backupManager.importBackup(stream, replaceExisting = true)
        assertTrue(result.isFailure)

        // Existing habit still exists
        val habits = repository.getAllHabitsSync()
        assertEquals(1, habits.size)
        assertEquals("Untouched Habit", habits[0].name)
    }
}
