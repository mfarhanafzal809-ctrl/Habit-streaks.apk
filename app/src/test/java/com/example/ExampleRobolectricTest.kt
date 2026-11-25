package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.HabitCompletion
import com.example.domain.StreakEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Habit Streaks", appName)
    }

    @Test
    fun `streak engine calculates correct consecutive streaks`() {
        val today = LocalDate.of(2026, 8, 23)
        val completions = listOf(
            HabitCompletion(habitId = 1, completionDate = "2026-08-23"),
            HabitCompletion(habitId = 1, completionDate = "2026-08-22"),
            HabitCompletion(habitId = 1, completionDate = "2026-08-21"),
            HabitCompletion(habitId = 1, completionDate = "2026-08-19")
        )

        val (currentStreak, longestStreak) = StreakEngine.calculateStreaks(completions, today)
        assertEquals(3, currentStreak)
        assertEquals(3, longestStreak)
    }

    @Test
    fun `streak engine handles missed day properly`() {
        val today = LocalDate.of(2026, 8, 23)
        val completions = listOf(
            HabitCompletion(habitId = 1, completionDate = "2026-08-20"),
            HabitCompletion(habitId = 1, completionDate = "2026-08-19"),
            HabitCompletion(habitId = 1, completionDate = "2026-08-18")
        )

        val (currentStreak, longestStreak) = StreakEngine.calculateStreaks(completions, today)
        assertEquals(0, currentStreak)
        assertEquals(3, longestStreak)
    }

    @Test
    fun `mainActivity launches and creates views without hanging`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume()
        org.junit.Assert.assertNotNull(controller.get())
    }
}
