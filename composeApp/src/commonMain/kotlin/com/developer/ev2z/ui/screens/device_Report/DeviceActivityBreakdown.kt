package com.developer.ev2z.ui.screens.device_Report

import com.developer.ev2z.response.AnalyticsSegment
import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.ui.screens.HistoryPlayback.dropStaleBeaconFixes
import com.developer.ev2z.ui.screens.HistoryPlayback.keepTravelableFixes
import kotlin.math.roundToInt

/**
 * How long the vehicle spent running, stopped and parked, worked out from the fixes.
 *
 * The three segments carry the same `minutes` / `hours` / `percent` shape the server's
 * `device/analytics` answer uses, so the donut and the legend rows draw either source
 * without knowing which one they were given.
 *
 * [knownMinutes] is what the percentages are a share of, and [unknownMinutes] is the
 * time deliberately left out of them — see [MAX_ATTRIBUTABLE_GAP_SECONDS]. Reporting
 * the two separately is the point: a day where the tracker was dark for six hours has
 * to be able to say so, rather than quietly inflating whichever state happened to be
 * reported last.
 */
data class ActivityBreakdown(
    val running: AnalyticsSegment,
    val stopped: AnalyticsSegment,
    val parked: AnalyticsSegment,
    val knownMinutes: Int,
    val unknownMinutes: Int,
) {
    /** Hours across the three states — what a "total activity" figure means here. */
    val knownHours: Double get() = (knownMinutes / 6.0).roundToInt() / 10.0
}

/**
 * The longest gap between two fixes that is still credited to the earlier one's state.
 *
 * Each interval between consecutive fixes is credited to the state reported at its
 * *start*: a fix saying "running" means the vehicle was running from then until it next
 * reported. That reasoning holds while the tracker is reporting on its usual cadence —
 * around every 60 s, and the batched arrivals measured on 2026-10-09 landed 40 s apart.
 *
 * It stops holding across a dead spot. A tracker that reports "running" at 09:00, loses
 * signal, and reports again at 17:00 did not drive for eight hours, and crediting them
 * would put a figure on screen that is wrong by the width of the whole chart. Past this
 * much silence the interval is counted as unknown instead, which is the honest answer:
 * nothing was reported, so nothing is known.
 *
 * Fifteen minutes is far outside the normal cadence — so no ordinary reporting gap is
 * discarded — while being short enough that a real outage cannot be credited to a state.
 */
internal const val MAX_ATTRIBUTABLE_GAP_SECONDS = 15L * 60L

/**
 * Works out [ActivityBreakdown] from a day's (or week's, or month's) fixes.
 *
 * Null when the fixes cannot answer — fewer than two readable timestamps, or every
 * interval unknown — so the caller can fall back to the server's own figures rather
 * than draw a chart of zeroes.
 *
 * ### What the input has to be
 *
 * Raw `route-history` **will give the wrong answer here.** The endpoint weaves a second
 * stream through a day — a backlog re-stamped with its arrival time, or a stale
 * "parked at base" beacon re-emitted every ~65 s — so the status sequence alternates
 * between two vehicles' worth of states and the intervals between them are meaningless.
 * Pass the fixes through `dropStaleBeaconFixes()` and `keepTravelableFixes()` first, the
 * same way the playback screen does.
 *
 * ### Why it sorts
 *
 * Even filtered, the fixes do not arrive in time order, and an interval measured against
 * an out-of-order neighbour is negative. Sorting here rather than trusting the caller
 * keeps that from silently zeroing a state's total.
 */
/**
 * Cleans a `route-history` answer into fixes the breakdown can be trusted on.
 *
 * Runs the same two filters the playback screen does — the stale-beacon drop and the
 * continuity filter — because without them the status sequence is two vehicles' states
 * interleaved: the beacon alone re-emits "parked" every ~65 s through a day of driving,
 * which would read as a mostly-parked day.
 *
 * **Deliberately not `collapseStationaryRuns`.** Playback also applies that, and it is
 * exactly wrong here: it keeps one fix per place, which is all a *drawn route* needs and
 * throws away the very thing being measured — how long the vehicle sat there.
 *
 * ### Why it filters a day at a time
 *
 * `dropStaleBeaconFixes` clusters fixes by place, and matching each fix against the
 * clusters found so far costs the number of distinct places. Over a day that is cheap.
 * Over the month range it is not: tens of thousands of fixes against thousands of places
 * is hundreds of millions of distance calculations, and the screen would hang on it.
 *
 * A day is also the right unit on its own terms. Both filters reason about one journey —
 * "a place this trace keeps returning to", "a chain of fixes that could have been
 * driven" — and neither statement means anything stretched across three weeks.
 */
internal fun prepareFixesForBreakdown(points: List<RoutePoint>): List<RoutePoint> =
    points
        .filter { it.isValid() }
        .groupBy { point ->
            // Whole days since the epoch. Read from the parsed timestamp rather than
            // sliced off the string, because this endpoint sends the field under three
            // names and not always in the same shape.
            point.epochSecondsOrNull()?.div(SECONDS_PER_DAY)
        }
        .flatMap { (day, fixes) ->
            // Fixes with an unreadable timestamp are kept but not filtered — the filters
            // need times to judge anything, and the breakdown drops them anyway.
            if (day == null) fixes else keepTravelableFixes(dropStaleBeaconFixes(fixes))
        }

private const val SECONDS_PER_DAY = 86_400L

internal fun computeActivityBreakdown(points: List<RoutePoint>): ActivityBreakdown? {
    val timed = points
        .mapNotNull { point ->
            val at = point.epochSecondsOrNull() ?: return@mapNotNull null
            at to point.statusKind()
        }
        .sortedBy { (at, _) -> at }

    if (timed.size < 2) return null

    var running = 0L
    var stopped = 0L
    var parked = 0L
    var unknown = 0L

    for (index in 0 until timed.size - 1) {
        val (at, kind) = timed[index]
        val seconds = timed[index + 1].first - at

        // Two fixes sharing a timestamp carry no interval between them. Common: a batch
        // upload stamps everything in it with the same arrival second.
        if (seconds <= 0L) continue

        if (seconds > MAX_ATTRIBUTABLE_GAP_SECONDS) {
            unknown += seconds
            continue
        }

        when (kind) {
            "running" -> running += seconds
            // Idle — ignition on, not moving — is time the vehicle spent stopped, and
            // the screen offers no fourth bucket to put it in. Folded rather than
            // dropped: left out it would vanish from a total the percentages are a
            // share of, and idling is exactly the time a fleet owner is looking for.
            "stopped", "idle" -> stopped += seconds
            "parked" -> parked += seconds
            // "offline", "no_data", "unknown" — these say the tracker was not reporting,
            // not what the vehicle was doing, so they are not a state to credit.
            else -> unknown += seconds
        }
    }

    val known = running + stopped + parked
    if (known <= 0L) return null

    val percents = sharesOf(listOf(running, stopped, parked), known)

    return ActivityBreakdown(
        running = segmentOf(running, percents[0]),
        stopped = segmentOf(stopped, percents[1]),
        parked = segmentOf(parked, percents[2]),
        knownMinutes = (known / 60L).toInt(),
        unknownMinutes = (unknown / 60L).toInt(),
    )
}

private fun segmentOf(seconds: Long, percent: Int) = AnalyticsSegment(
    minutes = (seconds / 60L).toInt(),
    // One decimal, which is all the legend row shows.
    hours = (seconds / 360.0).roundToInt() / 10.0,
    percent = percent,
)

/**
 * [parts] as whole percentages of [total], adding up to exactly 100.
 *
 * Rounding each share on its own does not: three states at 33.3% each round to 99, and a
 * donut whose arcs total 99% leaves a visible notch in the ring. The leftover points go
 * to the largest fractions, so the figure that gains one is the one that was closest to
 * deserving it anyway.
 */
private fun sharesOf(parts: List<Long>, total: Long): List<Int> {
    val exact = parts.map { it * 100.0 / total }
    val whole = exact.map { it.toInt() }.toMutableList()

    val byFraction = exact.indices.sortedByDescending { exact[it] - whole[it] }
    var leftover = 100 - whole.sum()
    var next = 0

    while (leftover > 0 && next < byFraction.size) {
        whole[byFraction[next]]++
        leftover--
        next++
    }

    return whole
}
