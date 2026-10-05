package com.developer.ev2z.ui.screens.HistoryPlayback

import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.approxDistanceMeters
import com.developer.ev2z.response.RoutePoint

/**
 * Removes the stale "parked at base" fixes the history endpoint interleaves into a
 * live trace.
 *
 * A day's route for a moving vehicle comes back looking like this:
 *
 * ```
 * 04:39:46  28.603899,77.352813  speed=0   parked   <- base
 * 04:39:47  28.601527,77.353948  speed=30  moving
 * 04:40:52  28.603899,77.352813  speed=0   parked   <- base again
 * 04:40:53  28.599920,77.350526  speed=32  moving
 * ```
 *
 * Every ~65 s the server re-emits one unchanged parked coordinate alongside the real
 * fixes, so the vehicle appears to teleport home and back out between every pair of
 * samples. Drawn as-is that is a star of straight lines radiating from the depot across
 * every building in between — 480 km of line for an 87 km trip — and no amount of road
 * snapping can repair it, because the geometry being snapped is not a journey.
 *
 * The distance figure was already immune (`calculateTotalDistance` throws out segments
 * implying thousands of km/h), but the drawn route and the playback marker read the
 * points directly, so the beacon had to come out of the list itself.
 *
 * ### Why the obvious filters do not work
 *
 * Rejecting a fix by implied speed keeps whichever of the two streams comes first, and
 * here that is the beacon. A spike test — "this fix darts out and comes straight back" —
 * inverts for the same reason: the beacon repeats one coordinate exactly, so the hop
 * *over* it measures zero and every genuine moving fix scores as the spike.
 *
 * What actually separates them is repetition. The real fixes are each distinct and
 * advance along a road; the beacon is one place the trace keeps returning to long after
 * it has driven away. Parking really does repeat a coordinate, but contiguously — a
 * vehicle does not visit the depot between every two fixes of a highway run.
 */
internal fun dropStaleBeaconFixes(points: List<RoutePoint>): List<RoutePoint> {
    if (points.size < MIN_TRACE_FOR_BEACON_CHECK) return points

    val positions = points.map { point ->
        val lat = point.getLatDouble()
        val lng = point.getLngDouble()
        if (lat != null && lng != null) GeoPoint(lat, lng) else null
    }

    val places = clusterPlaces(positions)
    val stale = HashSet<Int>()

    places.forEach { place ->
        val runs = contiguousRuns(place.indices)

        // One stay is a stop, not a beacon. It takes repeated returns to count.
        if (runs.size < 1 + MIN_BEACON_RETURNS) return@forEach

        // ...and the trace has to have genuinely driven away in between, otherwise
        // this is a vehicle idling around one spot with a fix or two of drift.
        val droveAway = runs.zipWithNext().any { (before, after) ->
            (before.last() + 1 until after.first()).any { between ->
                val position = positions[between] ?: return@any false
                approxDistanceMeters(place.centre, position) > LEFT_PLACE_METERS
            }
        }
        if (!droveAway) return@forEach

        // The first run is where the vehicle actually was. Every later reappearance is
        // the server repeating itself.
        runs.drop(1).forEach { run -> stale += run }
    }

    if (stale.isEmpty()) return points

    println(
        "🧹 [History] stale beacon: dropped ${stale.size} of ${points.size} fixes " +
                "repeating ${places.count { contiguousRuns(it.indices).size > 1 }} place(s)"
    )

    return points.filterIndexed { index, _ -> index !in stale }
}

/** One standing spot, and every fix that landed on it. */
private class Place(val centre: GeoPoint) {
    val indices = ArrayList<Int>()
}

/**
 * Greedy spatial clustering: each fix joins the first place within
 * [SAME_PLACE_METERS], or opens a new one.
 *
 * Good enough because the beacon repeats a single coordinate exactly — the cluster only
 * has to be wide enough to absorb a metre or two of jitter around it, not to partition
 * the route properly.
 */
private fun clusterPlaces(positions: List<GeoPoint?>): List<Place> {
    val places = ArrayList<Place>()

    positions.forEachIndexed { index, position ->
        if (position == null) return@forEachIndexed

        val place = places.firstOrNull { candidate ->
            approxDistanceMeters(candidate.centre, position) <= SAME_PLACE_METERS
        } ?: Place(position).also { places += it }

        place.indices += index
    }

    return places
}

/** Splits sorted indices into runs of consecutive values. */
private fun contiguousRuns(indices: List<Int>): List<List<Int>> {
    if (indices.isEmpty()) return emptyList()

    val runs = ArrayList<MutableList<Int>>()
    runs += mutableListOf(indices.first())

    indices.drop(1).forEach { index ->
        if (index == runs.last().last() + 1) runs.last() += index else runs += mutableListOf(index)
    }

    return runs
}

/** Fixes this close together are the same standing spot. */
internal const val SAME_PLACE_METERS = 25.0

/** Past this the trace has really driven away from a place, not drifted around it. */
internal const val LEFT_PLACE_METERS = 300.0

/** Returns to one place, beyond the first stay, before it reads as a beacon. */
internal const val MIN_BEACON_RETURNS = 2

/** Too short a trace to tell a beacon from an ordinary there-and-back trip. */
internal const val MIN_TRACE_FOR_BEACON_CHECK = 8