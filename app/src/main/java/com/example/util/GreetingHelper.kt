package com.example.util

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object GreetingHelper {

    private val TIME_FORMATTER: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }

    private val DATE_FORMATTER: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    }

    enum class TimePeriod(val label: String, val emoji: String) {
        MORNING("Good Morning", "☀️"),
        AFTERNOON("Good Afternoon", "🌤️"),
        EVENING("Good Evening", "🌆"),
        NIGHT("Good Night", "🌙")
    }

    /**
     * Exact scheduled greeting windows:
     * - Good Morning: Starts at 3:00 AM (3:00 AM – 11:59 AM).
     * - Good Afternoon: Starts after 12:00 PM (12:00 PM – 4:59 PM).
     * - Good Evening: 5:00 PM – 8:59 PM.
     * - Good Night: 9:00 PM – 2:59 AM.
     */
    fun getTimePeriod(hour: Int): TimePeriod {
        return when (hour) {
            in 3..11 -> TimePeriod.MORNING
            in 12..16 -> TimePeriod.AFTERNOON
            in 17..20 -> TimePeriod.EVENING
            else -> TimePeriod.NIGHT // 21, 22, 23, 0, 1, 2
        }
    }

    fun getGreetingText(hour: Int, userName: String = "Champion", withEmoji: Boolean = true): String {
        val period = getTimePeriod(hour)
        val cleanName = if (userName.isNotBlank()) userName else "Champion"
        return if (withEmoji) {
            "${period.label}, $cleanName! ${period.emoji}"
        } else {
            "${period.label}, $cleanName!"
        }
    }

    /**
     * Determines the effective ZoneId using either an explicit user override or the device's real timezone.
     */
    fun getEffectiveZoneId(userCustomZone: String? = null): ZoneId {
        return runCatching {
            if (!userCustomZone.isNullOrBlank()) {
                ZoneId.of(userCustomZone)
            } else {
                ZoneId.systemDefault()
            }
        }.getOrElse {
            ZoneId.systemDefault()
        }
    }

    /**
     * Obtains the real current date & time using the effective timezone.
     */
    fun getNow(customZone: String? = null): ZonedDateTime {
        return ZonedDateTime.now(getEffectiveZoneId(customZone))
    }

    fun formatTimeWithAmPm(dateTime: ZonedDateTime = getNow()): String {
        return runCatching {
            dateTime.format(TIME_FORMATTER)
        }.getOrElse {
            val hour = dateTime.hour
            val minute = dateTime.minute
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
        }
    }

    fun formatFullHeaderDate(dateTime: ZonedDateTime = getNow()): String {
        return runCatching {
            dateTime.format(DATE_FORMATTER)
        }.getOrElse {
            val dayName = dateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val monthName = dateTime.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            "$dayName, $monthName ${dateTime.dayOfMonth}"
        }
    }
}
