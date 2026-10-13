package com.developer.ev2z.ui.components

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMakeRotation
import kotlin.math.PI

/**
 * The angle to draw a vehicle at on screen: it is heading [headingDegrees] and the map
 * itself is turned to [mapHeadingDegrees]. In **radians**, which is what
 * `CGAffineTransform` wants.
 *
 * Two things are going on, and only one of them is MapKit's fault:
 *
 * * The artwork is drawn from above, facing up, so the heading needs no correcting —
 *   see `VehicleTopView`. The pose arithmetic this used to carry (a mirror flag, a
 *   quarter-turn offset, a tilt cap) was all for the side-profile emoji, and went with
 *   it.
 * * An annotation view *is* screen-space. Unlike a flat Google Maps marker it does not
 *   turn with the map, so the map's own rotation has to come out of the heading — or
 *   the vehicle points across the buildings the moment the user turns the map with two
 *   fingers.
 */
internal fun vehicleScreenAngleRadians(
    headingDegrees: Double,
    mapHeadingDegrees: Double,
): Double = (markerRotationDegrees(headingDegrees) - mapHeadingDegrees) * PI / 180.0

/** [angleRadians] as a view transform. */
@OptIn(ExperimentalForeignApi::class)
internal fun vehicleViewTransform(angleRadians: Double): CValue<CGAffineTransform> =
    CGAffineTransformMakeRotation(angleRadians)
