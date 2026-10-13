package com.developer.ev2z.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.ui.components.glideDurationMs
import com.developer.ev2z.ui.components.isFarJump
import io.ktor.util.date.getTimeMillis

/**
 * Walks the live marker along the road between the last two fixes, instead of sliding
 * it along the straight line between them.
 *
 * A tracker reports every few tens of seconds, so the straight line between two fixes
 * can be hundreds of metres of chord — and a chord cuts across the blocks, the central
 * reservation and the island of every roundabout on it. The vehicle visibly drove
 * through buildings and came out facing the wrong way. [path] is the same road geometry
 * the history map walks (snapped by `RoadSnapService`), so the marker follows the
 * street and comes round a roundabout the way it was driven.
 *
 * The walk restarts whenever [path] changes, which happens twice per fix: once with the
 * straight chord the moment the fix lands, and again a moment later when the snapped
 * road arrives. The second one resumes from wherever the marker already is
 * ([nearestDistanceAlong]) rather than from the start, so swapping the chord for the
 * road is not a jump backwards.
 *
 * Pacing is measured, not assumed: each walk takes slightly longer than the gap
 * actually observed between the last two fixes, so the next fix lands while the marker
 * is still moving. A flat duration only matches reality when the network does.
 */
@Composable
fun rememberRoadWalk(path: List<GeoPoint>): State<PathPosition?> {
    val drawn = remember { mutableStateOf<PathPosition?>(null) }

    // Arrival time of the destination currently being walked towards, so the next walk
    // can be paced by a real measurement. 0 until the first one lands.
    val lastArrivalMs = remember { longArrayOf(0L) }
    val walkMillis = remember { doubleArrayOf(FIRST_WALK_MS) }
    // The destination the current pacing was measured for. A path that only changed
    // shape — chord replaced by road — must not be timed as if a new fix had arrived.
    val pacedFor = remember { arrayOfNulls<GeoPoint>(1) }

    LaunchedEffect(path) {
        val destination = path.lastOrNull() ?: return@LaunchedEffect

        // One point is a fix with nothing to travel from: the first of a session, or a
        // vehicle that has not moved since the last one.
        if (path.size < 2) {
            drawn.value = PathPosition(
                destination.latitude,
                destination.longitude,
                drawn.value?.bearingDegrees ?: Double.NaN,
            )
            return@LaunchedEffect
        }

        if (pacedFor[0]?.let { samePlace(it, destination) } != true) {
            val now = getTimeMillis()
            if (lastArrivalMs[0] != 0L) {
                walkMillis[0] = glideDurationMs(now - lastArrivalMs[0]).toDouble()
            }
            lastArrivalMs[0] = now
            pacedFor[0] = destination
        }

        val here = drawn.value
        val travelled = cumulativeDistances(path)
        val total = travelled.last()
        if (total <= 0.0) {
            drawn.value = positionAlong(path, travelled, 0.0)
            return@LaunchedEffect
        }

        // A tracker that was off the air comes back somewhere else entirely; walking
        // there would drive the marker across the city at an impossible speed, so that
        // one case snaps.
        if (here != null && isFarJump(here.latitude, here.longitude, destination.latitude, destination.longitude)) {
            drawn.value = positionAlong(path, travelled, total)
            return@LaunchedEffect
        }

        val from = here
            ?.let { nearestDistanceAlong(path, travelled, it.asGeoPoint()) }
            ?: 0.0

        var bearing = here?.bearingDegrees ?: Double.NaN

        eachFrameUntilDone(walkMillis[0]) { fraction ->
            val at = positionAlong(path, travelled, from + (total - from) * fraction)
            // Eased, not assigned: a corner's worth of turn applied in one frame reads
            // as the vehicle pivoting on the spot.
            bearing = stepBearing(bearing, at.bearingDegrees)
            drawn.value = at.copy(bearingDegrees = bearing)
        }
    }

    return drawn
}

/**
 * Seeds the very first walk, before there are two fixes to measure a real gap from.
 *
 * The nominal live poll interval. Replaced by a measurement as soon as one exists.
 */
private const val FIRST_WALK_MS = 10_000.0
