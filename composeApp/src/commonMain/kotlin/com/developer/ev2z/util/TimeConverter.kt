package com.developer.ev2z.util

import kotlinx.datetime.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun formatToIST(dateUtc: String): String {
    return try {
        val instant = Instant.parse(dateUtc)

        val istTimeZone = TimeZone.of("Asia/Kolkata")
        val localDateTime = instant.toLocalDateTime(istTimeZone)

        val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
        val month = localDateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val year = localDateTime.year

        val hour12 = when {
            localDateTime.hour == 0 -> 12
            localDateTime.hour > 12 -> localDateTime.hour - 12
            else -> localDateTime.hour
        }.toString().padStart(2, '0')

        val minute = localDateTime.minute.toString().padStart(2, '0')
        val amPm = if (localDateTime.hour < 12) "AM" else "PM"

        "$day $month $year, $hour12:$minute $amPm"
    } catch (e: Exception) {
        ""
    }
}

@OptIn(ExperimentalTime::class)
fun formatUtcToISTSafe(dateTime: String): String {
    return try {
        val instant = when {
            // Case 1: Proper ISO with Z
            dateTime.contains("T") && dateTime.endsWith("Z") -> {
                Instant.parse(dateTime)
            }

            // Case 2: "yyyy-MM-dd HH:mm:ss" (your backend)
            dateTime.contains(" ") -> {
                val fixed = dateTime.replace(" ", "T") + "Z"
                Instant.parse(fixed)
            }

            // Fallback
            else -> Instant.parse(dateTime)
        }

        val istZone = TimeZone.of("Asia/Kolkata")
        val istDateTime = instant.toLocalDateTime(istZone)

        formatReadableIST(istDateTime)

    } catch (e: Exception) {
        dateTime // fallback: show raw value instead of blank
    }
}

private fun formatReadableIST(dt: LocalDateTime): String {
    val day = dt.dayOfMonth.toString().padStart(2, '0')
    val month = dt.month.name.lowercase()
        .replaceFirstChar { it.uppercase() }
        .take(3)
    val year = dt.year

    val hour12 = when {
        dt.hour == 0 -> 12
        dt.hour > 12 -> dt.hour - 12
        else -> dt.hour
    }.toString().padStart(2, '0')

    val minute = dt.minute.toString().padStart(2, '0')
    val amPm = if (dt.hour < 12) "AM" else "PM"

    return "$day $month $year, $hour12:$minute $amPm"
}