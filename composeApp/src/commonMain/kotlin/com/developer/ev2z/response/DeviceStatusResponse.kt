package com.developer.ev2z.response

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class DeviceStatusResponse(
    val success: Boolean,
    val message: String? = null,
    val data: DeviceStatusData? = null
)


@Serializable
data class DeviceStatusData(
    val status_count: StatusCount,
    val sessions: List<StatusSession>,
    /**
     * How long the day was spent in each status, which the session list cannot answer
     * at a glance: four moving sessions add up to 6h 49m, and reading that off seven
     * rows is arithmetic the summary should have done. Null on an older payload, which
     * sent counts only.
     */
    val status_duration: StatusDurations? = null,
    /**
     * The server's own count, which is not always `sessions.size` — a filtered or
     * truncated page would disagree, so the header shows this one when it is there.
     */
    val total_sessions: Int = 0,
    /**
     * The zone every stamp in this response is already expressed in ("Asia/Kolkata").
     * The clocks are therefore shown as sent — see [StatusSession.startClock].
     */
    val timezone: String? = null,
)

@Serializable
data class StatusDurations(
    val moving: StatusDuration = StatusDuration(),
    val idle: StatusDuration = StatusDuration(),
    val parked: StatusDuration = StatusDuration(),
    val offline: StatusDuration = StatusDuration(),
)

@Serializable
data class StatusDuration(
    val count: Int = 0,
    /** The exact total. The minutes and hours beside it are this one, rounded. */
    val total_seconds: Int = 0,
    val total_minutes: Double = 0.0,
    val total_hours: Double = 0.0,
    /** "6 hours 49 minutes 56 seconds" — see [formatDurationSeconds] for the short form. */
    val formatted: String? = null,
)

fun StatusDurations.of(status: String): StatusDuration = when (status.lowercase()) {
    "moving", "running" -> moving
    "idle" -> idle
    "parked" -> parked
    "offline" -> offline
    else -> StatusDuration()
}

/**
 * Everything the day accounts for, in seconds.
 *
 * Not a full twenty-four hours: the report covers what the tracker reported, and a
 * device that was off the air for an hour has that hour in none of the four buckets.
 */
val StatusDurations.trackedSeconds: Int
    get() = moving.total_seconds + idle.total_seconds +
            parked.total_seconds + offline.total_seconds

/** The server's count, with `sessions.size` behind it for an older payload. */
val DeviceStatusData.sessionCount: Int
    get() = if (total_sessions > 0) total_sessions else sessions.size

@Serializable
data class StatusCount(
    val offline: Int = 0,
    val moving: Int = 0,
    val idle: Int = 0,
    val parked: Int = 0
)

/** How many of [status] this day holds, for the summary tiles and the filter row. */
fun StatusCount.of(status: String): Int = when (status.lowercase()) {
    "moving", "running" -> moving
    "idle" -> idle
    "parked" -> parked
    "offline" -> offline
    else -> 0
}

val StatusCount.total: Int get() = moving + idle + parked + offline

@Serializable
data class StatusSession(
    val status: String,
    /**
     * Already in [DeviceStatusData.timezone] and in twelve-hour form
     * ("2026-10-15 12:00:47 AM") — not UTC. Passing these through a UTC→IST converter
     * added five and a half hours to a local time, and failed to parse the meridiem
     * besides.
     */
    val start_time: String = "",
    val end_time: String = "",
    /**
     * Where the session began and ended. A moving session covers ground, so the one
     * coordinate the payload used to carry described only half of it; [latitude] and
     * [longitude] are still sent, and are the end of the session.
     */
    val start_latitude: String = "",
    val start_longitude: String = "",
    val end_latitude: String = "",
    val end_longitude: String = "",
    val latitude: String = "",
    val longitude: String = "",
    /** The exact length. Minutes and hours below are this, rounded, and lose seconds. */
    val duration_seconds: Int = 0,
    /**
     * Fractional, not whole: the endpoint divides [duration_seconds] by sixty and sends
     * the result as it comes out — a 152-second session arrives as `2.53`. Declared as
     * `Int`, that crashed the whole response on the first session shorter than a round
     * minute.
     *
     * Defaulted, like every field here: this endpoint nulls fields per device rather
     * than omitting them, and `coerceInputValues` can only fall back to a default that
     * exists. A session with no fix used to take the response down with it.
     */
    val duration_minutes: Double = 0.0,
    val duration_hours: Double = 0.0,
    /** "10 hours 28 minutes 28 seconds" — too long for a row, see [durationLabel]. */
    val duration_formatted: String? = null,
    val duration_detail: DurationDetail? = null,
    /**
     * True when the server made this session up out of a gap rather than reading it
     * off the tracker: the twelve-second holes between two moving sessions are the
     * signal dropping, not the vehicle stopping, and the card says so instead of
     * reporting a journey the driver never took.
     */
    val offline_inferred: Boolean = false,
)

@Serializable
data class DurationDetail(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val formatted: String? = null,
)

/**
 * The session's length in seconds, from whichever field the payload filled in.
 *
 * [duration_seconds] first because it is the only exact one; the detail block next;
 * [duration_minutes] last, which is rounded to two decimals and so is a second or two
 * out on a long session.
 */
fun StatusSession.durationSeconds(): Int = when {
    duration_seconds > 0 -> duration_seconds
    duration_detail != null -> with(duration_detail) { hours * 3600 + minutes * 60 + seconds }
    else -> (duration_minutes * 60).roundToInt()
}

/**
 * How long the session lasted, short enough to sit at the end of a row.
 *
 * The server's own `duration_formatted` is "0 hours 1 minutes 5 seconds" — three units
 * wide whatever the length, with the zero ones included. This drops what rounds to
 * nothing: hours and minutes once there is an hour, minutes and seconds below that,
 * and bare seconds for the signal gaps, which are twelve seconds long and would
 * otherwise all read "0 min".
 */
fun StatusSession.durationLabel(): String = formatDurationSeconds(durationSeconds())

internal fun formatDurationSeconds(total: Int): String {
    if (total <= 0) return "0s"
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/**
 * The clock out of a server stamp, shown as sent.
 *
 * "2026-10-15 12:00:47 AM" → "12:00 AM". Seconds are dropped — they belong to the
 * duration, which is exact, not to a time the eye is scanning down a list. A stamp in
 * the older twenty-four-hour form comes back as "09:36", and anything unrecognised is
 * returned whole rather than blanked.
 */
fun StatusSession.startClock(): String = clockLabel(start_time)

fun StatusSession.endClock(): String = clockLabel(end_time)

internal fun clockLabel(stamp: String): String {
    val parts = stamp.trim().split(" ").filter { it.isNotEmpty() }
    val time = parts.getOrNull(1) ?: return stamp
    val pieces = time.split(":")
    val hour = pieces.getOrNull(0)?.toIntOrNull() ?: return stamp
    val minute = pieces.getOrNull(1)?.takeIf { it.length == 2 } ?: return stamp

    val declared = parts.getOrNull(2)?.uppercase()?.takeIf { it == "AM" || it == "PM" }
    if (declared != null) return "${twoDigits(hour)}:$minute $declared"

    // A twenty-four-hour stamp, which some days of this endpoint send instead
    // ("2026-10-15 09:36:57"). The meridiem is worked out rather than left off: "09:36"
    // beside "04:59" gives the reader no way to tell a morning from an evening, and
    // half the report is the overnight session.
    val meridiem = if (hour < 12) "AM" else "PM"
    val twelve = if (hour % 12 == 0) 12 else hour % 12
    return "${twoDigits(twelve)}:$minute $meridiem"
}

/** Two digits, so a column of clock times lines up under one another. */
private fun twoDigits(value: Int): String = if (value < 10) "0$value" else "$value"

/** The day out of a server stamp ("2026-10-15"), for a session that crosses midnight. */
internal fun dateLabel(stamp: String): String? =
    stamp.trim().split(" ").firstOrNull()?.takeIf { it.contains("-") }

/** Whether the session ended on a later day than it started. */
fun StatusSession.spansDays(): Boolean {
    val from = dateLabel(start_time) ?: return false
    val to = dateLabel(end_time) ?: return false
    return from != to
}

/**
 * Where the session began, or null if the tracker had no fix for it.
 *
 * Falls back to the single coordinate the payload always sends. `"0"`/`"0"` is dropped
 * rather than shown: that is what a tracker reports while it is searching for
 * satellites, and geocoding it names a spot in the Gulf of Guinea.
 */
fun StatusSession.startPoint(): Pair<Double, Double>? =
    declaredStartPoint() ?: pointOf(latitude, longitude)

/** Where the session ended — the pair the payload also sends as plain lat/lng. */
fun StatusSession.endPoint(): Pair<Double, Double>? =
    declaredEndPoint() ?: pointOf(latitude, longitude)

/**
 * The start the payload stated outright, with no falling back to the single
 * coordinate — which is the *end* of the session, not its start.
 *
 * Some days carry `start_latitude`/`start_longitude` per session and some do not, and
 * the difference matters: told apart, a missing start can be filled in from the session
 * before it (see [asLegs]); conflated, every journey appears to have begun where it
 * finished.
 */
fun StatusSession.declaredStartPoint(): Pair<Double, Double>? =
    pointOf(start_latitude, start_longitude)

fun StatusSession.declaredEndPoint(): Pair<Double, Double>? =
    pointOf(end_latitude, end_longitude)

/** Whether this status is one the vehicle covers ground in. */
fun StatusSession.isTravelling(): Boolean = status.lowercase() in setOf("moving", "running")

/** Where a leg's [SessionLeg.start] came from, which decides how much it can be trusted. */
enum class LegStart {
    /** The payload stated it outright. */
    DECLARED,

    /** Taken from the session before it: the drive began where the last one ended. */
    PREVIOUS_SESSION,

    /** A status that does not travel — the one coordinate is both of its ends. */
    SAME_PLACE,

    /**
     * A journey with nothing in front of it — the first session of the day, whose
     * predecessor belongs to yesterday's report. Its start is genuinely unknown, and
     * saying so beats repeating the end and calling it both.
     */
    UNKNOWN,
}

/**
 * A session with both of its ends pinned down as far as the payload allows.
 *
 * [start] and [end] are what the card shows beside the two clock times; they are equal
 * for a parked or idle session, which does not go anywhere, and [start] is null for the
 * one journey a day that has no predecessor to inherit from.
 */
data class SessionLeg(
    val session: StatusSession,
    val start: Pair<Double, Double>?,
    val end: Pair<Double, Double>?,
    val startSource: LegStart,
) {
    /** Whether the vehicle finished somewhere other than where it started. */
    val coveredGround: Boolean get() = start != null && end != null && start != end

    /**
     * Whether the vehicle demonstrably did not move — both ends known, and the same.
     *
     * Not merely `!coveredGround`: a leg whose start was never reported has equal ends
     * only because one of them is missing, and the first drive of the day — five hours
     * of it — would otherwise be labelled as having gone nowhere.
     */
    val stayedPut: Boolean
        get() = startSource != LegStart.UNKNOWN && start != null && start == end
}

/**
 * The day's sessions with a start for each, derived where the payload omits one.
 *
 * A session carries one coordinate — where it ended — so the start of a journey has to
 * come from somewhere. It comes from the session before it: the day is a chain, and the
 * drive that follows a park begins at that park. Only travelling sessions are filled in
 * this way; a parked vehicle is in one place, and handing it the previous end would
 * have it drift eighty metres while standing still.
 *
 * Must be called on the **whole** day in the order the server sent it — the chain is
 * what supplies the starts, and the predecessor of a filtered list is the wrong
 * session. The first journey of the day has no predecessor and keeps its own
 * coordinate at both ends.
 */
fun List<StatusSession>.asLegs(): List<SessionLeg> {
    var previousEnd: Pair<Double, Double>? = null

    return map { session ->
        val end = session.endPoint()
        val declared = session.declaredStartPoint()

        val source = when {
            declared != null -> LegStart.DECLARED
            !session.isTravelling() -> LegStart.SAME_PLACE
            previousEnd != null -> LegStart.PREVIOUS_SESSION
            else -> LegStart.UNKNOWN
        }

        val start = when (source) {
            LegStart.DECLARED -> declared
            LegStart.SAME_PLACE -> end
            LegStart.PREVIOUS_SESSION -> previousEnd
            LegStart.UNKNOWN -> null
        }

        if (end != null) previousEnd = end
        SessionLeg(session, start, end, source)
    }
}

/** Whether the vehicle finished the session somewhere other than where it began. */
fun StatusSession.coveredGround(): Boolean {
    val from = startPoint() ?: return false
    val to = endPoint() ?: return false
    return from != to
}

private fun pointOf(latitude: String, longitude: String): Pair<Double, Double>? {
    val lat = latitude.toDoubleOrNull() ?: return null
    val lng = longitude.toDoubleOrNull() ?: return null
    if (lat == 0.0 && lng == 0.0) return null
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
    return lat to lng
}
