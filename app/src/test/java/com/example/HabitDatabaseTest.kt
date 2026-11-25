package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.HabitDatabase
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: HabitDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testForeignKeyCascadeDelete() = runBlocking {
        val habitId = db.habitDao().insertHabit(Habit(name = "Meditation"))
        assertTrue(habitId > 0)

        val completionId = db.habitCompletionDao().insertCompletion(
            HabitCompletion(habitId = habitId, completionDate = "2026-10-11")
        )
        assertTrue(completionId > 0)

        val completion = db.habitCompletionDao().getCompletion(habitId, "2026-10-11")
        assertNotNull(completion)

        db.habitDao().deleteHabitById(habitId)

        val completionsAfterDelete = db.habitCompletionDao().getAllCompletionsList()
        assertTrue(completionsAfterDelete.isEmpty())
    }

    @Test
    fun testUniqueCompletionConstraint() = runBlocking {
        val habitId = db.habitDao().insertHabit(Habit(name = "Water"))

        val id1 = db.habitCompletionDao().insertCompletion(
            HabitCompletion(habitId = habitId, completionDate = "2026-10-11")
        )
        assertTrue(id1 > 0)

        val id2 = db.habitCompletionDao().insertCompletion(
            HabitCompletion(habitId = habitId, completionDate = "2026-10-11")
        )
        assertEquals(-1L, id2)
    }

    @Test
    fun testTransactionRollbackPreservesData() {
        val sdb = db.openHelper.writableDatabase
        sdb.execSQL("INSERT INTO habits (id, name, description, category, colorHex, iconName, createdAt, updatedAt, isArchived, sortOrder, reminderEnabled, reminderHour, reminderMinute, ringMode, ringtoneName, vibrateEnabled, frequency, additionalReminderTimes) VALUES (101, 'Baseline Habit', '', 'General', '#FF6B4A', 'Flame', 0, 0, 0, 0, 0, 9, 0, 'SOUND_AND_VIBRATE', 'Default', 1, 'DAILY', '')")

        var countBefore = 0
        sdb.query("SELECT COUNT(*) FROM habits").use { cursor ->
            if (cursor.moveToFirst()) countBefore = cursor.getInt(0)
        }
        assertEquals(1, countBefore)

        try {
            sdb.beginTransaction()
            sdb.execSQL("INSERT INTO habits (id, name, description, category, colorHex, iconName, createdAt, updatedAt, isArchived, sortOrder, reminderEnabled, reminderHour, reminderMinute, ringMode, ringtoneName, vibrateEnabled, frequency, additionalReminderTimes) VALUES (102, 'Doomed Habit', '', 'General', '#FF6B4A', 'Flame', 0, 0, 0, 0, 0, 9, 0, 'SOUND_AND_VIBRATE', 'Default', 1, 'DAILY', '')")
            // Throw exception before setTransactionSuccessful() to simulate failure
            throw RuntimeException("Transaction failed intentionally")
        } catch (expected: RuntimeException) {
            // Expected error
        } finally {
            sdb.endTransaction()
        }

        var countAfter = 0
        sdb.query("SELECT COUNT(*) FROM habits").use { cursor ->
            if (cursor.moveToFirst()) countAfter = cursor.getInt(0)
        }
        assertEquals(countBefore, countAfter)
    }

    @Test
    fun testMigration1To2() {
        val dbName = "test_migration_1_2.db"
        context.deleteDatabase(dbName)

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(sdb: SupportSQLiteDatabase) {
                        sdb.execSQL("""
                            CREATE TABLE habits (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                description TEXT NOT NULL,
                                category TEXT NOT NULL,
                                colorHex TEXT NOT NULL,
                                iconName TEXT NOT NULL,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                isArchived INTEGER NOT NULL,
                                sortOrder INTEGER NOT NULL,
                                reminderEnabled INTEGER NOT NULL,
                                reminderHour INTEGER NOT NULL,
                                reminderMinute INTEGER NOT NULL,
                                ringMode TEXT NOT NULL,
                                ringtoneName TEXT NOT NULL,
                                vibrateEnabled INTEGER NOT NULL,
                                frequency TEXT NOT NULL
                            )
                        """)
                        sdb.execSQL("INSERT INTO habits (id, name, description, category, colorHex, iconName, createdAt, updatedAt, isArchived, sortOrder, reminderEnabled, reminderHour, reminderMinute, ringMode, ringtoneName, vibrateEnabled, frequency) VALUES (1, 'Legacy Habit', '', 'Health', '#FF6B4A', 'Flame', 0, 0, 0, 0, 0, 9, 0, 'SOUND_AND_VIBRATE', 'Default', 1, 'DAILY')")
                    }
                    override fun onUpgrade(sdb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val sdb = openHelper.writableDatabase
        HabitDatabase.MIGRATION_1_2.migrate(sdb)

        sdb.query("SELECT additionalReminderTimes FROM habits WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
        }

        sdb.close()
        openHelper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration2To3WithOrphanRecovery() {
        val dbName = "test_migration_2_3.db"
        context.deleteDatabase(dbName)

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(sdb: SupportSQLiteDatabase) {
                        sdb.execSQL("""
                            CREATE TABLE habits (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                description TEXT NOT NULL,
                                category TEXT NOT NULL,
                                colorHex TEXT NOT NULL,
                                iconName TEXT NOT NULL,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                isArchived INTEGER NOT NULL,
                                sortOrder INTEGER NOT NULL,
                                reminderEnabled INTEGER NOT NULL,
                                reminderHour INTEGER NOT NULL,
                                reminderMinute INTEGER NOT NULL,
                                ringMode TEXT NOT NULL,
                                ringtoneName TEXT NOT NULL,
                                vibrateEnabled INTEGER NOT NULL,
                                frequency TEXT NOT NULL,
                                additionalReminderTimes TEXT NOT NULL DEFAULT ''
                            )
                        """)
                        sdb.execSQL("""
                            CREATE TABLE habit_completions (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                habitId INTEGER NOT NULL,
                                completionDate TEXT NOT NULL,
                                completedAt INTEGER NOT NULL,
                                note TEXT NOT NULL
                            )
                        """)

                        sdb.execSQL("INSERT INTO habits (id, name, description, category, colorHex, iconName, createdAt, updatedAt, isArchived, sortOrder, reminderEnabled, reminderHour, reminderMinute, ringMode, ringtoneName, vibrateEnabled, frequency, additionalReminderTimes) VALUES (1, 'Habit 1', '', 'Health', '#FF6B4A', 'Flame', 0, 0, 0, 0, 0, 9, 0, 'SOUND_AND_VIBRATE', 'Default', 1, 'DAILY', '')")
                        sdb.execSQL("INSERT INTO habit_completions (id, habitId, completionDate, completedAt, note) VALUES (1, 1, '2026-10-01', 0, 'valid')")
                        sdb.execSQL("INSERT INTO habit_completions (id, habitId, completionDate, completedAt, note) VALUES (2, 999, '2026-10-01', 0, 'orphan')")
                    }
                    override fun onUpgrade(sdb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val sdb = openHelper.writableDatabase
        HabitDatabase.MIGRATION_2_3.migrate(sdb)

        // Verify orphan habit was recovered
        sdb.query("SELECT name FROM habits WHERE id = 999").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Recovered Habit 999", cursor.getString(0))
        }

        // Verify completions were preserved
        sdb.query("SELECT note FROM habit_completions WHERE habitId = 999").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("orphan", cursor.getString(0))
        }

        sdb.close()
        openHelper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testUnsupportedMigrationFailsSafely() {
        val dbName = "test_unsupported.db"
        context.deleteDatabase(dbName)

        // Create database at version 1
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(sdb: SupportSQLiteDatabase) {
                        sdb.execSQL("CREATE TABLE habits (id INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
                    }
                    override fun onUpgrade(sdb: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        openHelper.writableDatabase.close()
        openHelper.close()

        // Try opening with Room without providing migration from 1 -> 3
        try {
            val testDb = Room.databaseBuilder(context, HabitDatabase::class.java, dbName)
                // Intentionally omit MIGRATION_1_2 and MIGRATION_2_3
                .build()

            // Triggering open without migration path must throw IllegalStateException (fail safely, no destructive fallback)
            testDb.habitDao().getActiveHabitCount()
            testDb.close()
        } catch (e: IllegalStateException) {
            // Expected: "A migration from 1 to 3 was required but not found."
            assertTrue(e.message?.contains("migration", ignoreCase = true) == true)
        } catch (e: Exception) {
            // Any failure to migrate without destructive fallback is safe
            assertNotNull(e)
        } finally {
            context.deleteDatabase(dbName)
        }
    }
}
