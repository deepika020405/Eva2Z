package com.developer.ev2z.ui.screens.HistoryPlayback

import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.approxDistanceMeters
import com.developer.ev2z.response.RoutePoint

/**
 * Collapses runs of fixes taken at the same place down to one, so playback spends its
 * time travelling rather than standing still.
 *
 * A tracker reports every ~60 s whether or not the vehicle is going anywhere, so a day
 * that starts with the vehicle parked overnight begins with hundreds of fixes carrying
 * one identical coordinate:
 *
 * ```
 * 00:00:50  28.603829,77.352759  speed=0  parked
 * 00:01:51  28.603829,77.352759  speed=0  parked
 * 00:02:50  28.603829,77.352759  speed=0  parked   … ×90
 * ```
 *
 * Playback advances one fix at a time, so that run is a minute of a marker with
 * nowhere to go — indistinguishable from a Play button that does not work, which is
 * exactly how it was reported. [playbackStepMillis] cannot rescue it either: pacing
 * the leg by the real time it took would hold the marker there for the whole park.
 *
 * This is **not** [dropStaleBeaconFixes]. That removes a stale coordinate the server
 * *interleaves* into a live trace, and it deliberately keeps a contiguous park, because
 * a park is a real thing that happened. This keeps the park too — one fix of it, which
 * is all the drawn route and the marker can show anyway — and throws away the repeats.
 *
 * Distance is unaffected: `calculateTotalDistance` already ignores segments under three
 * metres, which is every segment removed here. Measuring against the last *kept* fix
 * rather than the previous one is what keeps a slow creep out of the dead zone — ten
 * two-metre steps are one twenty-metre move, not ten discarded ones.
 */
internal fun collapseStationaryRuns(points: List<RoutePoint>): List<RoutePoint> {
    if (points.size < 3) return points

    val kept = ArrayList<RoutePoint>(points.size)
    var anchor: GeoPoint? = null

    for (point in points) {
        val position = point.toGeoPointOrNull()

        // A fix with unusable coordinates is left where it is rather than silently
        // dropped: isValid() upstream decides what counts as usable, not this.
        if (position == null) {
            kept.add(point)
            continue
        }

        val previous = anchor
        if (previous != null &&
            approxDistanceMeters(previous, position) < STATIONARY_MOVE_METERS
        ) {
            continue
        }

        kept.add(point)
        anchor = position
    }

    // The last fix is where the vehicle finished the day, so playback has to end on it
    // even when it sat there long enough to be collapsed into the fix before.
    val last = points.last()
    if (kept.lastOrNull() !== last) kept.add(last)

    return kept
}

/**
 * Below this, a fix is the same place as the one before it.
 *
 * Three metres to match the minimum segment `calculateTotalDistance` will count, so
 * collapsing cannot change the distance the screen reports. It is also about the floor
 * of what consumer GPS can tell apart — a parked vehicle's fixes wander by a metre or
 * two, and drawing that wander is jitter, not travel.
 */
private const val STATIONARY_MOVE_METERS = 3.0

private fun RoutePoint.toGeoPointOrNull(): GeoPoint? {
    val latitude = getLatDouble() ?: return null
    val longitude = getLngDouble() ?: return null
    return GeoPoint(latitude, longitude)
}
