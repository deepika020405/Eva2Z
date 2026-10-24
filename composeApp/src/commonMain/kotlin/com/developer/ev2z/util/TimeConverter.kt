package com.developer.ev2z.util

import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The zone every stamp out of the status endpoints is already expressed in, which they
 * say so themselves: `status-details` sends `"timezone":"Asia/Kolkata"` with each day.
 */
private val REPORT_ZONE = TimeZone.of("Asia/Kolkata")

/**
 * Today as the reports count it — ISO `yyyy-MM-dd`, the form `?start_date=` wants.
 *
 * The server's day, not the phone's: `status-details` buckets its sessions in
 * Asia/Kolkata whatever zone the handset is set to, so a phone a few hours behind would
 * otherwise ask for yesterday and be told the device has done nothing since midnight.
 */
@OptIn(ExperimentalTime::class)
fun todayInReportZone(): String =
    Clock.System.now().toLocalDateTime(REPORT_ZONE).date.toString()

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

//    return "$day $month $year, $hour12:$minute $amPm"
    return "$day $month, $hour12:$minute $amPm"
}