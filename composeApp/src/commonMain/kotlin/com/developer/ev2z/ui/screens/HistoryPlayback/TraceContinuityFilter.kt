package com.developer.ev2z.ui.screens.HistoryPlayback

import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.approxDistanceMeters
import com.developer.ev2z.response.RoutePoint
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Keeps the fixes that form one journey and drops the ones that cannot belong to it.
 *
 * The history endpoint does not return a single vehicle's trace in travel order. A day
 * comes back with a second stream woven through it — a backlog the tracker uploaded
 * after a dead spot, re-stamped with its arrival time, or another terminal's fixes
 * altogether — so the list reads like this:
 *
 * ```
 * 08:17:58  28.494437,77.316236  44  moving   <- northbound on NH-19
 * 08:19:03  28.347475,77.347628  16  moving   <- 16 km south, 65 s later
 * 08:20:10  28.348852,77.341317  19  moving
 * 08:21:17  28.520067,77.310078  45  moving   <- back on NH-19, further north
 * ```
 *
 * Drawn as it arrives, that is a line that leaves the road every couple of minutes,
 * crosses the city, and comes back. Measured on 2026-10-09 for device
 * 0869727076942661: 1360 fixes drawing **1001 km** of line for a day the tracker itself
 * put at 27 km/h average — a cobweb over Delhi rather than a route. No amount of road
 * snapping repairs that, because the geometry handed to the Roads API is not a journey:
 * `snapToRoads` broke the same trace into 182 separate runs and the gap filler was left
 * with 565 straight chords it had no budget to route.
 *
 * ### What separates the streams
 *
 * Physics. Two fixes 16 km apart a minute apart did not come from one vehicle, whatever
 * the timestamps claim. So the filter keeps the longest chain of fixes that could
 * actually have been driven, in the order they arrived, and drops the rest. On that same
 * day it keeps 90% of the fixes and brings the drawn line from 1001 km to 209 km — a
 * day's work up and down NH-19, which is what the tracker's own average speed implies.
 *
 * ### Why a chain, and not a per-fix test
 *
 * Rejecting each fix against the one before it keeps whichever stream happens to come
 * first and then measures everything against the intruder. Scoring whole chains instead
 * means the majority stream wins on its own weight: a short excursion costs more than it
 * is worth, while the journey it interrupts is held together by every fix on it.
 *
 * Crossing an unreachable hop is priced rather than forbidden ([UNREACHABLE_HOP_COST]),
 * because a tracker that was off for an hour really does resume 40 km away. Paying for
 * that one hop is worth it to keep the rest of the day; paying for the two hops in and
 * out of a ten-fix excursion is not.
 *
 * Runs after [dropStaleBeaconFixes], which catches what this cannot: a depot coordinate
 * re-emitted a kilometre from the route is slow enough to be reachable, and is only
 * recognisable by how often the trace returns to it.
 */
internal fun keepTravelableFixes(points: List<RoutePoint>): List<RoutePoint> {
    if (points.size < MIN_TRACE_FOR_CONTINUITY_CHECK) return points

    val positions = points.map { point ->
        val lat = point.getLatDouble()
        val lng = point.getLngDouble()
        if (lat != null && lng != null) GeoPoint(lat, lng) else null
    }
    val seconds = points.map { it.epochSecondsOrNull() }

    // The best score of a chain ending at each fix, and the fix it came from. One fix is
    // worth one point, so the highest score is the longest chain that holds together.
    val score = DoubleArray(points.size)
    val cameFrom = IntArray(points.size) { -1 }
    var best = 0

    points.indices.forEach { index ->
        // A chain may also start here: a day that opens with a burst of another
        // terminal's fixes should not be measured from them.
        score[index] = 1.0

        for (previous in maxOf(0, index - CONTINUITY_LOOKBACK) until index) {
            val travelable = isTravelable(positions, seconds, previous, index)
            val candidate = score[previous] + 1.0 -
                    if (travelable) 0.0 else UNREACHABLE_HOP_COST

            if (candidate > score[index]) {
                score[index] = candidate
                cameFrom[index] = previous
            }
        }

        if (score[index] > score[best]) best = index
    }

    val keptReversed = ArrayList<Int>(points.size)
    var at = best
    while (at != -1) {
        keptReversed += at
        at = cameFrom[at]
    }

    if (keptReversed.size == points.size) return points

    println(
        "🧹 [History] trace continuity: kept ${keptReversed.size} of ${points.size} " +
                "fixes, dropped ${points.size - keptReversed.size} off the journey"
    )

    return keptReversed.asReversed().map { points[it] }
}

/**
 * Whether a vehicle could have got from fix [from] to fix [to] in the time between them.
 *
 * Unjudgeable hops — a fix with no position, a timestamp that will not parse — count as
 * travelable. The filter is here to throw out what is provably impossible, not to drop
 * everything it cannot measure.
 */
private fun isTravelable(
    positions: List<GeoPoint?>,
    seconds: List<Long?>,
    from: Int,
    to: Int,
): Boolean {
    val start = positions[from] ?: return true
    val end = positions[to] ?: return true
    val startedAt = seconds[from] ?: return true
    val endedAt = seconds[to] ?: return true

    // Clamped: the backlog stream arrives stamped out of order, and a negative interval
    // must not hand out an unlimited allowance.
    val elapsed = (endedAt - startedAt).coerceAtLeast(0L)
    val reach = maxOf(FIX_JITTER_METERS, MAX_PLAUSIBLE_SPEED_MPS * elapsed)

    return approxDistanceMeters(start, end) <= reach
}

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

/**
 * The fastest a tracked vehicle is taken to travel, in metres per second (90 km/h).
 *
 * Generous on purpose: this has to clear a car on NH-19 without question, because a
 * genuine fix wrongly thrown out is a piece of route missing from the map. The streams
 * it has to separate are not close to the line — the intruders measured on 2026-10-09
 * implied 450–68,000 km/h.
 */
internal const val MAX_PLAUSIBLE_SPEED_MPS = 25.0

/**
 * Distance always allowed between two fixes, however little time separates them.
 *
 * `created_at` is when the server took the fix, not when the tracker took it, and the
 * tracker uploads in batches — so a stretch really driven at 44 km/h arrives stamped
 * forty seconds apart over 1.3 km, and the speed limit above would call it impossible.
 * Measured against the live endpoint (2026-10-09), compressed intervals reach about
 * 1.4 km, while the streams this has to separate sit a median 2.2 km apart and reach
 * 24 km — so there is room for a generous allowance between the two. Tightening it
 * below this throws away real fixes without shortening the drawn line at all.
 */
internal const val FIX_JITTER_METERS = 1_500.0

/**
 * What crossing one unreachable hop costs, counted in fixes.
 *
 * A tracker that loses signal resumes somewhere it could not have driven to, and that
 * hop is the only thing holding the two halves of the day together — worth paying for
 * when the far half is a real stretch of route. An excursion into another stream pays it
 * twice, going out and coming back, which is more than a short run of fixes is worth.
 *
 * Set from both sides of that trade: the longest interleaved run measured on 2026-10-09
 * was 22 fixes, so 30 is enough to turn down every excursion in the day, while a trace
 * resuming after an outage only has to carry 30 fixes to be worth reconnecting. The
 * day's result is unchanged anywhere from 20 upwards.
 */
internal const val UNREACHABLE_HOP_COST = 30.0

/**
 * How far back a chain may reach for its previous fix.
 *
 * Long enough to step over an interleaved run — the longest measured was 83 fixes — and
 * short enough to keep the search linear in the length of the day.
 */
internal const val CONTINUITY_LOOKBACK = 200

/** Too short a trace to tell a journey from an intruder. */
internal const val MIN_TRACE_FOR_CONTINUITY_CHECK = 8
