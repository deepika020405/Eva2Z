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
