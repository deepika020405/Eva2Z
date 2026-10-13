package com.developer.ev2z.ui.components

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import kotlin.math.PI

/**
 * The vehicle markers every map draws, in one place.
 *
 * The live map and playback used to show different things at the same vehicle — a
 * tinted glyph on one and MapKit's default pin on the other — which reads as two
 * different vehicles rather than one seen twice.
 *
 * The shapes themselves are shared with Android rather than drawn here: see
 * `VehicleTopView`, which also explains why the vehicle is seen from above.
 */

/** Side of the vehicle marker image, in points — Android's equivalent is in pixels. */
internal const val VEHICLE_MARKER_POINTS = 34.0

/** Glass, dark enough to read as a window against every body colour. */
private const val GLASS_ARGB = 0xFF26323CL

/** A tyre. Not pure black — black reads as a hole in the vehicle at marker size. */
private const val TYRE_ARGB = 0xFF1F2124L

/**
 * A marking painted on the body — a cross, a roof sign, a ladder. Off-white, so it
 * stays a shade apart from the outline around the whole vehicle.
 */
private const val LIGHT_ARGB = 0xFFF2F4F5L

/** How far [PartPaint.BODY_DARK] is taken towards black from the body colour. */
private const val BODY_DARK_FRACTION = 0.26

/**
 * The vehicle marker: [vehicle] seen from above, facing up, its body in the colour out
 * of [bodyArgb].
 *
 * Facing **up** is the whole point — see `VehicleTopView`. The caller turns the view by
 * the heading (less the map's own) and nothing else, which is right at every angle,
 * where the side-profile emoji this replaces could not be turned at all without ending
 * up sideways, upside down, or stood on its back wheels.
 *
 * [bodyArgb] is the packed colour out of `statusColorOf`, so the marker always agrees
 * with the badge beside it and with the Android marker: green while the vehicle is
 * running, yellow idling, red stopped or parked. The emoji before it could not report a
 * state at all (🚗 is red whatever the vehicle is doing), which is why a stopped vehicle
 * used to be flattened into a silhouette and a moving one drawn in the font's own
 * colours — two different-looking markers for the same vehicle.
 *
 * No disc behind it: a ring read as a pin dropped on the map rather than as the vehicle,
 * and it hid the road underneath.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun vehicleMarkerImage(vehicle: VehicleTopView, bodyArgb: Long): UIImage? {
    val side = VEHICLE_MARKER_POINTS
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(side, side), false, 0.0)

    // The unit box of vehicleTopViewParts, inset so the outline has room to sit in.
    val inset = side * MARKER_OUTLINE_FRACTION
    val span = side - inset * 2.0
    val parts = vehicleTopViewParts(vehicle)
    fun pathOf(part: MarkerPart) = UIBezierPath.bezierPathWithRoundedRect(
        CGRectMake(
            inset + part.left * span,
            inset + part.top * span,
            (part.right - part.left) * span,
            (part.bottom - part.top) * span,
        ),
        cornerRadius = part.radius * span
    )

    // The whole outline first, under every part. A white vehicle-shaped edge is what
    // keeps a red vehicle off a tiled roof and a green one out of a park; stroking each
    // part as it is filled would instead leave white seams across the middle of the
    // vehicle, wherever two parts overlap.
    UIColor.whiteColor.setStroke()
    parts.forEach { part ->
        if (part.paint == PartPaint.BODY || part.paint == PartPaint.TYRE) {
            val path = pathOf(part)
            path.lineWidth = inset * 2.0
            path.stroke()
        }
    }

    // Then the parts themselves, in their own order: tyres, the body over them, the
    // darker upper surfaces, the glass on top.
    parts.forEach { part ->
        uiColorOf(
            when (part.paint) {
                PartPaint.BODY -> bodyArgb
                PartPaint.BODY_DARK -> darkenedArgb(bodyArgb, BODY_DARK_FRACTION)
                PartPaint.GLASS -> GLASS_ARGB
                PartPaint.TYRE -> TYRE_ARGB
                PartPaint.LIGHT -> LIGHT_ARGB
            }
        ).setFill()
        pathOf(part).fill()
    }

    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return image
}

/**
 * The heading arrow: a disc in the status colour with a white arrow on it — the marker
 * for a device the server names no vehicle for.
 *
 * [argb] is the packed colour out of `statusColorOf`, so this agrees with the badge
 * beside it and with the Android arrow — a stopped vehicle is red on both platforms
 * and every screen, rather than the fixed blue this used to draw whatever the vehicle
 * was doing.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun vehicleArrowImage(argb: Long): UIImage? {
    val side = VEHICLE_MARKER_POINTS
    // The geometry below was drawn for a 42 pt marker; scaling it keeps the arrow's
    // proportions when the marker size changes.
    val s = side / 42.0
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(side, side), false, 0.0)
    val white = UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 1.0)
    uiColorOf(argb).setFill()
    UIBezierPath.bezierPathWithOvalInRect(
        CGRectMake(1.0 * s, 1.0 * s, 40.0 * s, 40.0 * s)
    ).fill()
    white.setFill()
    val arrow = UIBezierPath()
    arrow.moveToPoint(CGPointMake(21.0 * s, 8.0 * s))
    arrow.addLineToPoint(CGPointMake(32.0 * s, 32.0 * s))
    arrow.addLineToPoint(CGPointMake(21.0 * s, 26.0 * s))
    arrow.addLineToPoint(CGPointMake(10.0 * s, 32.0 * s))
    arrow.closePath()
    arrow.fill()
    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return image
}

// ─────────────────────────────────────────────────────────────────────────────
//  The phone's own compass cone
// ─────────────────────────────────────────────────────────────────────────────

/** Side of the cone image, in points. */
private const val FACING_CONE_POINTS = 72.0

/** Half the cone's spread. 30° each way is wide enough to read as a direction. */
private const val FACING_CONE_HALF_ANGLE = 30.0

/**
 * Which way the phone is pointing: a translucent wedge, drawn facing **up** so the
 * only rotation applied is the compass heading (less the map's own).
 *
 * The wedge and nothing else. MapKit's own user-location view is blanked out on every
 * map — it draws a large translucent accuracy circle along with its dot and the two
 * cannot be separated — and a dot of our own was drawn here for a while and then
 * dropped as clutter. The apex of the wedge is the position; it needs nothing marking
 * it. Matches the Android cone.
 *
 * Three flat rings rather than a gradient — Core Graphics gradients through cinterop
 * cost far more code than they are worth for something this small, and stacked arcs
 * of falling opacity fade well enough at this size.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun userFacingImage(side: Double = FACING_CONE_POINTS): UIImage? {
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(side, side), false, 0.0)

    val centre = side / 2.0
    // UIKit's y grows downwards, so "up" is -90°, and arc angles are in radians.
    val start = (-90.0 - FACING_CONE_HALF_ANGLE) * PI / 180.0
    val end = (-90.0 + FACING_CONE_HALF_ANGLE) * PI / 180.0

    // Outermost and faintest first, so each ring lays over the one behind it and the
    // wedge comes out brightest at the apex, where the person actually is.
    //
    // White, and with more alpha than the blue it replaced: white has nothing to
    // contrast against on a pale road or a bright patch of satellite imagery, so a
    // wedge faint enough to work over dark tiles disappears entirely over light ones.
    listOf(1.0 to 0.24, 0.72 to 0.22, 0.42 to 0.24).forEach { (scale, alpha) ->
        UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = alpha).setFill()
        val wedge = UIBezierPath()
        wedge.moveToPoint(CGPointMake(centre, centre))
        wedge.addArcWithCenter(
            CGPointMake(centre, centre),
            radius = (centre - 1.0) * scale,
            startAngle = start,
            endAngle = end,
            clockwise = true
        )
        wedge.closePath()
        wedge.fill()
    }

    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return image
}

/** [argb] taken [fraction] of the way to black, per channel — the Android blend. */
private fun darkenedArgb(argb: Long, fraction: Double): Long {
    fun channel(shift: Int): Long =
        (((argb shr shift) and 0xFF) * (1.0 - fraction)).toLong().coerceIn(0L, 255L)
    return (argb and 0xFF000000L) or
            (channel(16) shl 16) or
            (channel(8) shl 8) or
            channel(0)
}

/** The packed colour out of `statusColorOf` as a UIColor. */
internal fun uiColorOf(argb: Long): UIColor = UIColor(
    red = ((argb shr 16) and 0xFF) / 255.0,
    green = ((argb shr 8) and 0xFF) / 255.0,
    blue = (argb and 0xFF) / 255.0,
    alpha = ((argb shr 24) and 0xFF).coerceAtLeast(1) / 255.0
)
