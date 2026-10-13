package com.developer.ev2z.ui.components

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * How a map follows a moving marker without pinning it to the middle of the screen.
 *
 * Re-centring on every frame — which both maps used to do — slides the map underneath
 * the marker and leaves it on the centre pixel forever. The marker is travelling the
 * whole time, but it never moves on screen, so the whole thing reads as a marker that
 * does not move at all. Panning only once it nears an edge is what makes the movement
 * visible.
 */

/**
 * How much of the viewport around the edge counts as "nearly off screen", per side.
 *
 * 0.22 keeps the marker inside the middle ~56% of the screen: wide enough that the
 * camera sits still for most of a stretch — which is what makes the marker look like
 * it is moving — and tight enough that the vehicle never slips off the edge before the
 * camera catches up.
 */
internal const val CAMERA_EDGE_MARGIN = 0.22

/** Long enough to read as a pan rather than a jump cut, short enough to keep up. */
internal const val CAMERA_PAN_MS = 450

/**
 * Whether [point] sits well inside these bounds — not within [CAMERA_EDGE_MARGIN] of
 * any edge.
 *
 * False for bounds that wrap the antimeridian, whose longitude span comes out
 * negative; recentring is the right answer there rather than trying to reason about
 * the wrap.
 */
internal fun LatLngBounds.holdsComfortably(point: LatLng): Boolean {
    val latitudeSpan = northeast.latitude - southwest.latitude
    val longitudeSpan = northeast.longitude - southwest.longitude
    if (latitudeSpan <= 0.0 || longitudeSpan <= 0.0) return false

    val latitudePad = latitudeSpan * CAMERA_EDGE_MARGIN
    val longitudePad = longitudeSpan * CAMERA_EDGE_MARGIN

    return point.latitude in
            (southwest.latitude + latitudePad)..(northeast.latitude - latitudePad) &&
            point.longitude in
            (southwest.longitude + longitudePad)..(northeast.longitude - longitudePad)
}
