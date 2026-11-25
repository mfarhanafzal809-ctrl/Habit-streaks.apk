package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AppSetting
import com.example.data.model.Habit
import com.example.data.model.HabitCompletion
import com.example.data.model.HabitFrequency
import com.example.data.model.RingMode

class Converters {
    @TypeConverter
    fun fromRingMode(mode: RingMode): String = mode.name

    @TypeConverter
    fun toRingMode(value: String): RingMode = runCatching { RingMode.valueOf(value) }.getOrDefault(RingMode.SOUND_AND_VIBRATE)

    @TypeConverter
    fun fromHabitFrequency(freq: HabitFrequency): String = freq.name

    @TypeConverter
    fun toHabitFrequency(value: String): HabitFrequency = runCatching { HabitFrequency.valueOf(value) }.getOrDefault(HabitFrequency.DAILY)
}

@Database(
    entities = [
        Habit::class,
        HabitCompletion::class,
        AppSetting::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN additionalReminderTimes TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Recover any orphan completions without destroying history by inserting placeholder habits
                db.execSQL("""
                    INSERT INTO habits (id, name, description, category, colorHex, iconName, createdAt, updatedAt, isArchived, sortOrder, reminderEnabled, reminderHour, reminderMinute, ringMode, ringtoneName, vibrateEnabled, frequency, additionalReminderTimes)
                    SELECT DISTINCT c.habitId, 'Recovered Habit ' || c.habitId, '', 'General', '#FF6B4A', 'Flame', 0, 0, 0, 0, 0, 9, 0, 'SOUND_AND_VIBRATE', 'Default Bell', 1, 'DAILY', ''
                    FROM habit_completions c
                    WHERE c.habitId NOT IN (SELECT id FROM habits)
                """)

                db.execSQL("CREATE TABLE habit_completions_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, habitId INTEGER NOT NULL, completionDate TEXT NOT NULL, completedAt INTEGER NOT NULL, note TEXT NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON DELETE CASCADE)")
                db.execSQL("INSERT INTO habit_completions_new (id, habitId, completionDate, completedAt, note) SELECT id, habitId, completionDate, completedAt, note FROM habit_completions")
                db.execSQL("DROP TABLE habit_completions")
                db.execSQL("ALTER TABLE habit_completions_new RENAME TO habit_completions")
                db.execSQL("CREATE UNIQUE INDEX index_habit_completions_habitId_completionDate ON habit_completions(habitId, completionDate)")
            }
        }

        fun getInstance(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_streaks_production.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys=ON;")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
