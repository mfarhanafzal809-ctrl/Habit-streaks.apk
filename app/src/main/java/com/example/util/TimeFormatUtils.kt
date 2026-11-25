package com.example.util

import java.util.Locale

/**
 * Universal utility for unified 12-hour and 24-hour time formatting across the app and widgets.
 */
object TimeFormatUtils {

    /**
     * Formats an hour (0-23) and minute (0-59) into either 12-hour AM/PM or 24-hour format.
     */
    fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        return if (is24Hour) {
            String.format(Locale.getDefault(), "%02d:%02d", safeHour, safeMinute)
        } else {
            val amPm = if (safeHour >= 12) "PM" else "AM"
            val hour12 = when (val h = safeHour % 12) {
                0 -> 12
                else -> h
            }
            String.format(Locale.getDefault(), "%d:%02d %s", hour12, safeMinute, amPm)
        }
    }

    /**
     * Formats a raw "HH:mm" time string according to the user's 12/24 hour preference.
     */
    fun formatTimeString(timeStr: String, is24Hour: Boolean): String {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return timeStr
        val h = parts[0].toIntOrNull() ?: return timeStr
        val m = parts[1].toIntOrNull() ?: return timeStr
        return formatTime(h, m, is24Hour)
    }

    /**
     * Formats multiple reminder times (e.g. "08:00,14:30") into a comma-separated list
     * formatted with the selected 12/24-hour preference.
     */
    fun formatAdditionalTimes(rawString: String, is24Hour: Boolean): List<String> {
        if (rawString.isBlank()) return emptyList()
        return rawString.split(",")
            .map { it.trim() }
            .filter { it.contains(":") }
            .map { formatTimeString(it, is24Hour) }
    }
}
