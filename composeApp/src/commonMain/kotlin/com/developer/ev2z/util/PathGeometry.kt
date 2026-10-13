package com.developer.ev2z.util

import androidx.compose.runtime.withFrameNanos
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.approxDistanceMeters
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Walking a marker along a road, and working out which way it is facing while it does.
 *
 * Shared by the two maps that do it: history playback, which walks the snapped route
 * between two recorded fixes, and the live map, which walks the snapped road between
 * the last two live fixes. They were the same arithmetic written twice, which is how
 * the live marker ended up cutting corners the playback marker went round.
 */

/** A point on a path as it is drawn, with the direction of travel there. */
data class PathPosition(
    val latitude: Double,
    val longitude: Double,
    /**
     * Which way the vehicle is facing here, in degrees clockwise from north, or NaN
     * until it has travelled far enough for a direction to mean anything.
     *
     * Carried alongside the position rather than worked out by the map, because the
     * two have to come from the same walk: on a roundabout the chord between two fixes
     * points across the island, while the road vertices the marker is actually
     * following curve around it. A heading computed from the fixes would have the
     * vehicle facing the wrong way for the whole turn.
     */
    val bearingDegrees: Double = Double.NaN,
)

/**
 * Running distance along [walk], so a marker crosses a stretch at a steady speed.
 *
 * Spreading a step evenly over the *vertices* instead would race the marker through a
 * long straight and crawl it around a corner, where the road geometry is dense.
 */
internal fun cumulativeDistances(walk: List<GeoPoint>): List<Double> {
    var running = 0.0
    return List(walk.size) { index ->
        if (index > 0) running += approxDistanceMeters(walk[index - 1], walk[index])
        running
    }
}

/** The point [distance] metres along [walk], interpolated inside its segment. */
internal fun pointAlong(
    walk: List<GeoPoint>,
    travelled: List<Double>,
    distance: Double,
): GeoPoint {
    // The vertex the marker has just passed. Linear scan is fine: one stretch is a
    // handful of vertices, and it is walked a few times per frame.
    var segment = 0
    while (segment < walk.size - 2 && travelled[segment + 1] < distance) segment++

    val from = walk[segment]
    val to = walk[segment + 1]
    val segmentLength = travelled[segment + 1] - travelled[segment]
    val fraction = if (segmentLength <= 0.0) 1.0
    else ((distance - travelled[segment]) / segmentLength).coerceIn(0.0, 1.0)

    return GeoPoint(
        latitude = from.latitude + (to.latitude - from.latitude) * fraction,
        longitude = from.longitude + (to.longitude - from.longitude) * fraction,
    )
}

/** Where the marker is, and which way it is facing, [distance] metres along [walk]. */
internal fun positionAlong(
    walk: List<GeoPoint>,
    travelled: List<Double>,
    distance: Double,
): PathPosition {
    val here = pointAlong(walk, travelled, distance)
    val total = travelled.last()

    // Where the road goes next, not which way the current segment happens to point.
    //
    // A snapped road is a dense polyline — consecutive vertices can be a metre or two
    // apart — and the angle of a two-metre segment is survey noise, not a direction.
    // Taking it per frame had the marker twitching left and right the whole way down a
    // straight road. Aiming at a point [HEADING_LOOK_AHEAD_METERS] further along is
    // what a driver does, and it still comes round a roundabout, because by then the
    // road ahead genuinely is round the corner.
    var bearing = bearingBetween(
        here,
        pointAlong(walk, travelled, min(distance + HEADING_LOOK_AHEAD_METERS, total))
    )

    // At the end of the stretch there is nothing ahead to aim at, so face the way the
    // marker arrived rather than snapping upright for the last few frames.
    if (bearing.isNaN()) {
        bearing = bearingBetween(
            pointAlong(walk, travelled, max(distance - HEADING_LOOK_AHEAD_METERS, 0.0)),
            here
        )
    }

    return PathPosition(here.latitude, here.longitude, bearing)
}

/**
 * How far along [walk] the point closest to [to] lies, in metres.
 *
 * What lets a walk be replaced mid-glide without the marker jumping: the live map
 * starts a leg on the straight chord and swaps in the snapped road a moment later, and
 * the new walk has to resume from wherever the marker already is rather than from the
 * beginning. Returns 0 when nothing is close, which is the honest answer for a path
 * that belongs to a different journey.
 */
internal fun nearestDistanceAlong(
    walk: List<GeoPoint>,
    travelled: List<Double>,
    to: GeoPoint,
): Double {
    var best = 0.0
    var bestDistance = Double.MAX_VALUE

    // Vertex resolution is enough: a snapped road's vertices are metres apart, and
    // being a vertex out costs the marker a metre of position it then glides off.
    walk.forEachIndexed { index, vertex ->
        val gap = approxDistanceMeters(vertex, to)
        if (gap < bestDistance) {
            bestDistance = gap
            best = travelled[index]
        }
    }

    return if (bestDistance > OFF_PATH_METERS) 0.0 else best
}

/**
 * Compass bearing from [from] to [to], in degrees clockwise from north.
 *
 * NaN for two points at effectively the same place: the angle between them is noise,
 * and letting noise through is what made a parked marker spin.
 */
internal fun bearingBetween(from: GeoPoint, to: GeoPoint): Double {
    if (approxDistanceMeters(from, to) < BEARING_MIN_MOVE_METERS) return Double.NaN

    val dLng = (to.longitude - from.longitude) * PI / 180
    val fromLat = from.latitude * PI / 180
    val toLat = to.latitude * PI / 180
    val y = sin(dLng) * cos(toLat)
    val x = cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(dLng)
    return (atan2(y, x) * 180 / PI + 360) % 360
}

/**
 * Eases [drawn] towards [target] along the shorter arc.
 *
 * A NaN target leaves the heading alone — the vehicle has not moved far enough to have
 * one — and a NaN current heading takes the target whole, so the first direction of a
 * route is drawn rather than eased into from north.
 */
internal fun stepBearing(drawn: Double, target: Double): Double =
    easeBearing(drawn, target, BEARING_EASE)

/**
 * Runs [onFrame] on the frame clock with how far through [durationMs] it is, ending on
 * exactly 1.0.
 *
 * Tying a glide to the real frame clock is what keeps it from drifting, or from burning
 * frames the display is not going to show.
 */
internal suspend fun eachFrameUntilDone(
    durationMs: Double,
    onFrame: (fraction: Double) -> Unit,
) {
    val startedAt = withFrameNanos { it }
    while (true) {
        val elapsedMs = (withFrameNanos { it } - startedAt) / 1_000_000.0
        val fraction = (elapsedMs / durationMs).coerceAtMost(1.0)
        onFrame(fraction)
        if (fraction >= 1.0) return
    }
}

/** Same spot to well under a metre, for deciding there is nothing to glide. */
internal fun samePlace(a: GeoPoint, b: GeoPoint): Boolean =
    approxDistanceMeters(a, b) < 0.5

/**
 * How far along the road the marker looks to decide which way it is facing.
 *
 * Far enough that the vertex-to-vertex noise of a snapped road averages out, short
 * enough that a corner is still a corner: at 25 m the marker starts turning into a
 * junction about when a driver would, and holds a straight line dead straight.
 */
private const val HEADING_LOOK_AHEAD_METERS = 25.0

/**
 * Below this, two points are the same place as far as a heading goes — see
 * [bearingBetween].
 *
 * Three metres: consumer GPS wanders by a metre or two standing still, and a
 * one-metre threshold let that wander through as a direction, which is half of why the
 * marker used to spin.
 */
private const val BEARING_MIN_MOVE_METERS = 3.0

/**
 * Fraction of the remaining turn applied per frame. At ~60 fps a turn settles in
 * roughly a fifth of a second: quick enough to agree with where the vehicle is going,
 * slow enough to read as a turn rather than a snap.
 */
private const val BEARING_EASE = 0.18

/**
 * Past this, a point is not on the path at all — see [nearestDistanceAlong].
 *
 * Generous, because the thing being matched is a marker mid-glide along a straight
 * chord whose snapped road can bow a long way off it.
 */
private const val OFF_PATH_METERS = 150.0

/** This position as a plain coordinate, for the distance helpers. */
internal fun PathPosition.asGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
