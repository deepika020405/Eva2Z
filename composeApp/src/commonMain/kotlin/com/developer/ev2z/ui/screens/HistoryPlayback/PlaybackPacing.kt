package com.developer.ev2z.ui.screens.HistoryPlayback

import com.developer.ev2z.response.RoutePoint
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * How long playback takes to travel the leg that *ends* at [index], in milliseconds.
 *
 * ### Why this is not a constant
 *
 * Playback used to hold every fix for a flat 500 ms, which makes the marker's speed on
 * screen a function of how far apart two fixes happen to be rather than of how fast the
 * vehicle was going. A tracker reports on a clock, not per kilometre, so after
 * [collapseStationaryRuns] has taken out the standstills one leg can be 40 m of car
 * park and the next 7 km of expressway — and both were drawn in half a second. The
 * vehicle appeared to crawl where it was quickest and leap where it was slowest, which
 * is the opposite of the truth.
 *
 * Pacing each leg by the real time it took, compressed by [PLAYBACK_SPEEDUP], makes
 * distance-over-time on screen proportional to distance-over-time on the road: the
 * marker accelerates onto the highway and slows through traffic because that is what
 * the timestamps say happened.
 *
 * ### The clamp
 *
 * [MIN_STEP_MS] and [MAX_STEP_MS] bound it. Without the ceiling an overnight park —
 * one leg of eight hours after collapsing — would freeze playback for minutes; without
 * the floor two fixes a second apart would flash past unreadably. Inside the clamp the
 * pacing is honest; a leg pressed against either end is not, which is the price of a
 * day being watchable in a few minutes.
 *
 * Falls back to [DEFAULT_STEP_MS] for the first fix, for a leg whose timestamps will
 * not parse, and for one the server stamped out of order — this decides how fast to
 * draw, so it must always answer with something sane.
 */
internal fun playbackStepMillis(points: List<RoutePoint>, index: Int): Long {
    if (index <= 0 || index >= points.size) return DEFAULT_STEP_MS

    val startedAt = points[index - 1].epochSecondsOrNull() ?: return DEFAULT_STEP_MS
    val endedAt = points[index].epochSecondsOrNull() ?: return DEFAULT_STEP_MS

    val elapsedSeconds = endedAt - startedAt
    if (elapsedSeconds <= 0L) return DEFAULT_STEP_MS

    val millis = elapsedSeconds * 1000.0 / PLAYBACK_SPEEDUP
    return millis.toLong().coerceIn(MIN_STEP_MS, MAX_STEP_MS)
}

/**
 * How much faster than real life playback runs.
 *
 * 90× puts a ten-hour working day at about seven minutes of watching, and a vehicle
 * doing 50 km/h crosses a 15-zoom screen in a couple of seconds — fast enough to read
 * as driving, slow enough to follow.
 */
private const val PLAYBACK_SPEEDUP = 90.0

/** Floor, so a burst of fixes a second apart does not flicker past. */
private const val MIN_STEP_MS = 90L

/** Ceiling, so a long stop does not look like playback has hung. */
private const val MAX_STEP_MS = 1_200L

/** Used wherever the timestamps cannot answer. The old flat rate. */
internal const val DEFAULT_STEP_MS = 500L

/** `created_at` as epoch seconds, or null if it is missing or malformed. */
private fun RoutePoint.epochSecondsOrNull(): Long? =
    try {
        // The zone is irrelevant: only differences between two of these are ever used.
        LocalDateTime.parse(createdAt.trim().replace(" ", "T"))
            .toInstant(TimeZone.UTC)
            .epochSeconds
    } catch (e: Exception) {
        null
    }
