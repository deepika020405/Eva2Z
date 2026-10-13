package com.developer.ev2z.ui.components

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapView

/**
 * How a map follows a moving marker without pinning it to the middle of the screen.
 *
 * Re-centring on every frame — which both maps used to do — slides the map underneath
 * the marker and leaves it on the centre pixel forever. The marker is travelling the
 * whole time, but it never moves on screen, so the whole thing reads as a marker that
 * does not move at all. Panning only once it nears an edge is what makes the movement
 * visible. Mirrors the Android helper, including the margin.
 */

/** How much of the viewport around the edge counts as "nearly off screen", per side. */
private const val CAMERA_EDGE_MARGIN = 0.22

/**
 * Keeps the marker at [latitude], [longitude] on screen, panning only when it comes
 * within [CAMERA_EDGE_MARGIN] of an edge.
 *
 * Animated, because an edge pan happens about once per screen-width of travel rather
 * than sixty times a second: there is never an earlier animation left running for this
 * one to cut short, which is what made the old per-frame animation drag behind.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun MKMapView.followWithDeadzone(latitude: Double, longitude: Double) {
    val holdsComfortably = region.useContents {
        val latitudePad = span.latitudeDelta / 2.0 * (1.0 - CAMERA_EDGE_MARGIN * 2.0)
        val longitudePad = span.longitudeDelta / 2.0 * (1.0 - CAMERA_EDGE_MARGIN * 2.0)

        // A span of zero is a map that has not laid out yet; recentring is the right
        // answer there rather than deciding the marker is comfortably inside nothing.
        if (span.latitudeDelta <= 0.0 || span.longitudeDelta <= 0.0) {
            false
        } else {
            latitude in (center.latitude - latitudePad)..(center.latitude + latitudePad) &&
                    longitude in
                    (center.longitude - longitudePad)..(center.longitude + longitudePad)
        }
    }

    if (holdsComfortably) return
    setCenterCoordinate(CLLocationCoordinate2DMake(latitude, longitude), animated = true)
}
