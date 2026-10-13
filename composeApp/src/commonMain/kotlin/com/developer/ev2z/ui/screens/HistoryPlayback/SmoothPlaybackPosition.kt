
// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/SmoothPlaybackPosition.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.SnappedRoute
import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.util.PathPosition
import com.developer.ev2z.util.asGeoPoint
import com.developer.ev2z.util.bearingBetween
import com.developer.ev2z.util.cumulativeDistances
import com.developer.ev2z.util.eachFrameUntilDone
import com.developer.ev2z.util.positionAlong
import com.developer.ev2z.util.samePlace
import com.developer.ev2z.util.stepBearing

/**
 * A point on the route as it is drawn, somewhere between two recorded samples.
 *
 * The same type the live map walks along — see [PathPosition]. Kept under this name
 * because that is what the playback screen and its map call it.
 */
typealias PlaybackPosition = PathPosition

/**
 * Walks the playback marker along the route instead of letting it hop from one
 * recorded point to the next.
 *
 * Playback advances `currentIndex` once per recorded fix — see [playbackStepMillis]
 * for how long that takes — so applying each new point directly means the marker
 * stands still and then teleports, which at highway sampling intervals is a hundred
 * metres at a time.
 *
 * Given a road-snapped [snappedRoute], the marker follows the road geometry between
 * two samples — the same vertices the drawn line uses — so it stays on the line and
 * goes around corners rather than cutting across them. Without one it falls back to a
 * straight glide between samples, which is still honest: consecutive route points are
 * samples taken along a path the vehicle actually drove. Positions stay in [Double] the
 * whole way, because a Float's precision at Indian longitudes is roughly a metre and
 * shows as jitter once the map is zoomed in.
 *
 * Returned [State] updates every animation frame. Read `.value` as deep in the tree
 * as possible — reading it in a map's content lambda rather than beside the map keeps
 * the per-frame recomposition down to the marker.
 */
@Composable
fun rememberSmoothPlaybackPosition(
    routePoints: List<RoutePoint>,
    currentIndex: Int,
    snappedRoute: SnappedRoute = SnappedRoute.EMPTY,
    /**
     * How long the glide into [currentIndex] takes. Defaults to the real duration of
     * that leg, compressed — the same figure the playback loop waits, from the same
     * function, so the marker arrives exactly as the index moves on.
     *
     * Pass a constant to glide every leg at one rate, which draws the marker's speed
     * as a function of fix spacing rather than of how fast the vehicle was going.
     */
    stepDurationMs: Double = playbackStepMillis(routePoints, currentIndex).toDouble(),
): State<PlaybackPosition?> {

    // Usable only while it describes this exact route: a mapping left over from the
    // previous date would walk the marker down last trip's roads.
    val road = remember(snappedRoute, routePoints) {
        snappedRoute.takeIf {
            it.path.size >= 2 && it.pathIndexOfPoint.size == routePoints.size
        }
    }

    // Keyed on the route so loading a different date snaps rather than glides: sliding
    // the marker from yesterday's last point to today's first would draw a journey
    // that never happened.
    val drawn = remember(routePoints) {
        mutableStateOf(routePoints.getOrNull(currentIndex)?.toPlaybackPosition())
    }

    // Which sample the marker is standing on, so the next step knows which stretch of
    // road it has to travel. `drawn` alone cannot say — mid-corner it is on a vertex
    // that belongs to no sample.
    val drawnIndex = remember(routePoints) { mutableStateOf(currentIndex) }

    LaunchedEffect(routePoints, road, currentIndex) {
        val from = drawn.value
        val fromIndex = drawnIndex.value
        drawnIndex.value = currentIndex

        // The heading already on screen. Each frame eases towards the direction of the
        // stretch being travelled rather than snapping to it, so a corner reads as the
        // vehicle turning through it instead of pivoting on the spot.
        var bearing = from?.bearingDegrees ?: Double.NaN

        if (road == null) {
            val to = routePoints.getOrNull(currentIndex)?.toPlaybackPosition()
                ?: return@LaunchedEffect

            // Nothing to travel from — first point of a route, or already there.
            if (from == null || samePlace(from.asGeoPoint(), to.asGeoPoint())) {
                drawn.value = to.copy(bearingDegrees = bearing)
                return@LaunchedEffect
            }

            val legBearing = bearingBetween(
                GeoPoint(from.latitude, from.longitude),
                GeoPoint(to.latitude, to.longitude),
            )

            eachFrameUntilDone(stepDurationMs) { fraction ->
                bearing = stepBearing(bearing, legBearing)
                drawn.value = PlaybackPosition(
                    latitude = from.latitude + (to.latitude - from.latitude) * fraction,
                    longitude = from.longitude + (to.longitude - from.longitude) * fraction,
                    bearingDegrees = bearing,
                )
            }
            return@LaunchedEffect
        }

        val target = road.positionOfPoint(currentIndex) ?: return@LaunchedEffect

        // Stretch of road between the two samples. One vertex means the vehicle did
        // not move (or the fix was thinned out of the snap request) — nothing to walk.
        val stretch = road.pathBetween(fromIndex, currentIndex)
        if (from == null || stretch.size < 2) {
            drawn.value = target.copy(bearingDegrees = bearing)
            return@LaunchedEffect
        }

        // The marker normally ends a step exactly on the vertex this one starts from.
        // It does not the first time a snapped route replaces the straight one, so the
        // walk begins from wherever it actually is and joins the road.
        val walk = if (samePlace(from.asGeoPoint(), stretch.first())) stretch
        else listOf(GeoPoint(from.latitude, from.longitude)) + stretch

        val travelled = cumulativeDistances(walk)
        val total = travelled.last()
        if (total <= 0.0) {
            drawn.value = target.copy(bearingDegrees = bearing)
            return@LaunchedEffect
        }

        eachFrameUntilDone(stepDurationMs) { fraction ->
            val at = positionAlong(walk, travelled, total * fraction)
            // Where the road goes next (see positionAt), eased into. That is what
            // brings the marker round a roundabout facing the way it is going instead
            // of holding the straight-line heading between the two fixes either side
            // of it — and what keeps it dead straight down a straight road, where the
            // snapped polyline's own vertices wander by a metre or two.
            bearing = stepBearing(bearing, at.bearingDegrees)
            drawn.value = at.copy(bearingDegrees = bearing)
        }
    }

    return drawn
}

/** Where the drawn line puts sample [pointIndex]. */
private fun SnappedRoute.positionOfPoint(pointIndex: Int): PlaybackPosition? {
    val pathIndex = pathIndexOfPoint.getOrNull(pointIndex) ?: return null
    val point = path.getOrNull(pathIndex) ?: return null
    return PlaybackPosition(point.latitude, point.longitude)
}

/**
 * The road vertices from sample [fromIndex] to sample [toIndex] inclusive, in travel
 * order — reversed when playback moved backwards.
 */
private fun SnappedRoute.pathBetween(fromIndex: Int, toIndex: Int): List<GeoPoint> {
    val from = pathIndexOfPoint.getOrNull(fromIndex) ?: return emptyList()
    val to = pathIndexOfPoint.getOrNull(toIndex) ?: return emptyList()
    return if (to >= from) path.subList(from, to + 1)
    else path.subList(to, from + 1).asReversed()
}

private fun RoutePoint.toPlaybackPosition(): PlaybackPosition? {
    val lat = getLatDouble() ?: return null
    val lng = getLngDouble() ?: return null
    return PlaybackPosition(lat, lng)
}


