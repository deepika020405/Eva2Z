package com.developer.ev2z.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/**
 * The vehicle markers every map draws, in one place.
 *
 * The live map and playback used to draw different things at the same vehicle — a
 * tinted glyph on one and Google's default red pin on the other — which reads as two
 * different vehicles rather than one seen twice.
 *
 * The shapes themselves are shared with iOS rather than drawn here: see
 * [VehicleTopView], which also explains why the vehicle is seen from above.
 */

/**
 * Side of the marker bitmap, in pixels — not dp: `fromBitmap` takes the bitmap as it
 * is, so on a 3x screen this is about 32 dp across.
 */
internal const val VEHICLE_MARKER_PX = 96

/** Glass, dark enough to read as a window against every body colour. */
private const val GLASS_COLOR = 0xFF26323C.toInt()

/** A tyre. Not pure black — black reads as a hole in the vehicle at marker size. */
private const val TYRE_COLOR = 0xFF1F2124.toInt()

/**
 * A marking painted on the body — a cross, a roof sign, a ladder.
 *
 * Off-white rather than pure white, so it is still a shade apart from the outline that
 * surrounds the whole vehicle.
 */
private const val LIGHT_COLOR = 0xFFF2F4F5.toInt()

/** How far [PartPaint.BODY_DARK] is taken towards black from the body colour. */
private const val BODY_DARK_FRACTION = 0.26f

/**
 * The vehicle marker: [vehicle] seen from above, facing up, its body in [bodyColor].
 *
 * Facing **up** is the whole point — see [VehicleTopView]. The caller turns the marker
 * by the heading and nothing else, which is right at every angle, where the side-profile
 * emoji this replaces could not be turned at all without ending up sideways, upside
 * down, or stood on its back wheels.
 *
 * [bodyColor] carries the state, so the marker and the badge beside it always agree:
 * green while the vehicle is running, yellow idling, red stopped or parked — see
 * `statusColorOf`. The emoji before it could not do this at all (🚗 is red whatever the
 * vehicle is doing), which is why a stopped vehicle used to be flattened into a
 * silhouette and a moving one drawn in the font's own colours — two different-looking
 * markers for the same vehicle.
 *
 * No disc behind it: a ring read as a pin dropped on the map rather than as the vehicle,
 * and it hid the road underneath.
 *
 * Null when the Maps SDK cannot supply a descriptor — see [toMarkerIcon]; the caller
 * falls back to the default pin rather than taking the screen down with it.
 */
internal fun vehicleMarkerIcon(
    context: Context,
    vehicle: VehicleTopView,
    bodyColor: Int,
    size: Int = VEHICLE_MARKER_PX
): BitmapDescriptor? {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // The unit box of vehicleTopViewParts, inset so the outline has room to sit in.
    val inset = size * MARKER_OUTLINE_FRACTION
    val span = size - inset * 2f
    val parts = vehicleTopViewParts(vehicle)
    fun rectOf(part: MarkerPart) = RectF(
        inset + part.left * span,
        inset + part.top * span,
        inset + part.right * span,
        inset + part.bottom * span,
    )

    // The whole outline first, under every part. A white vehicle-shaped edge is what
    // keeps a red vehicle off a tiled roof and a green one out of a park; stroking each
    // part as it is filled would instead leave white seams across the middle of the
    // vehicle, wherever two parts overlap.
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = inset * 2f
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    parts.forEach { part ->
        if (part.paint == PartPaint.BODY || part.paint == PartPaint.TYRE) {
            val rect = rectOf(part)
            canvas.drawRoundRect(rect, part.radius * span, part.radius * span, outline)
        }
    }

    // Then the parts themselves, in their own order: tyres, the body over them, the
    // darker upper surfaces, the glass on top.
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    parts.forEach { part ->
        fill.color = when (part.paint) {
            PartPaint.BODY -> bodyColor
            PartPaint.BODY_DARK -> blendToward(bodyColor, Color.BLACK, BODY_DARK_FRACTION)
            PartPaint.GLASS -> GLASS_COLOR
            PartPaint.TYRE -> TYRE_COLOR
            PartPaint.LIGHT -> LIGHT_COLOR
        }
        val rect = rectOf(part)
        canvas.drawRoundRect(rect, part.radius * span, part.radius * span, fill)
    }

    return bitmap.toMarkerIcon(context)
}

/**
 * The heading arrow, drawn in [color] — the marker for a device the server names no
 * vehicle for.
 *
 * The PNG it replaces was a fixed blue, so a stopped vehicle and a running one looked
 * identical on the map while the badge beside it said otherwise. Tinting that PNG
 * would flatten it to one solid colour, so the same two-tone chevron is drawn here
 * instead: the lit half in [color], the shaded half darkened, which keeps the sense of
 * a direction the arrow is pointing in.
 */
internal fun vehicleArrowIcon(
    context: Context,
    color: Int,
    size: Int = VEHICLE_MARKER_PX
): BitmapDescriptor? {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val w = size.toFloat()
    val tipY = w * 0.03f
    val baseY = w * 0.97f
    val notchY = w * 0.74f
    val mid = w * 0.5f

    // Left half, lighter: the chevron reads as a solid shape without it, and the
    // crease down the middle is what shows which way it is facing at a glance.
    paint.color = blendToward(color, Color.WHITE, 0.45f)
    canvas.drawPath(
        Path().apply {
            moveTo(mid, tipY)
            lineTo(mid, notchY)
            lineTo(w * 0.1f, baseY)
            close()
        },
        paint
    )

    paint.color = color
    canvas.drawPath(
        Path().apply {
            moveTo(mid, tipY)
            lineTo(w * 0.9f, baseY)
            lineTo(mid, notchY)
            close()
        },
        paint
    )

    return bitmap.toMarkerIcon(context)
}

// ─────────────────────────────────────────────────────────────────────────────
//  The phone's own compass cone
// ─────────────────────────────────────────────────────────────────────────────

/** Side of the cone bitmap, in pixels: about 36 dp of reach on a 3x screen. */
private const val FACING_CONE_PX = 220

/** Half the cone's spread. 30° each way is wide enough to read as a direction. */
private const val FACING_CONE_HALF_ANGLE = 30f

/**
 * White, so the wedge reads as a light being shone rather than as another coloured
 * thing on a map that already uses green, red, yellow and grey to mean something.
 *
 * Carries more alpha than the blue it replaced: white has nothing to contrast against
 * on a pale road or a bright patch of satellite imagery, so a wedge faint enough to
 * work over dark map tiles disappears entirely over light ones.
 */
private const val FACING_CONE_COLOR = 0xFFFFFFFF.toInt()

/**
 * Which way the phone is pointing: a translucent wedge, drawn facing **up** so the
 * marker's rotation can be the compass heading as it is.
 *
 * The wedge and nothing else. The map's own my-location layer is off on every map —
 * it draws a large translucent accuracy circle along with its dot and the two cannot
 * be separated (`isMyLocationEnabled` is all or nothing) — and a dot of our own was
 * drawn here for a while and then dropped as clutter. The apex of the wedge is the
 * position; it needs nothing marking it.
 *
 * The wedge fades outwards rather than filling flat, because a hard edge reads as a
 * claim about how far away something is, and this says nothing about distance — only
 * about which way the person holding the phone is looking.
 */
internal fun userFacingIcon(
    context: Context,
    size: Int = FACING_CONE_PX
): BitmapDescriptor? {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centre = size / 2f
    // The apex sits at the centre of the bitmap, which is where the marker is
    // anchored, so the wedge opens from the user's own position.
    val radius = centre - 1f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.RadialGradient(
            centre,
            centre,
            radius,
            intArrayOf(
                withAlpha(FACING_CONE_COLOR, 0.70f),
                withAlpha(FACING_CONE_COLOR, 0.34f),
                withAlpha(FACING_CONE_COLOR, 0f),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
    }

    // drawArc measures from 3 o'clock, clockwise, so "up" is -90.
    val start = -90f - FACING_CONE_HALF_ANGLE
    canvas.drawPath(
        Path().apply {
            moveTo(centre, centre)
            arcTo(
                RectF(centre - radius, centre - radius, centre + radius, centre + radius),
                start,
                FACING_CONE_HALF_ANGLE * 2f
            )
            close()
        },
        paint
    )

    return bitmap.toMarkerIcon(context)
}

/** [color] at [alpha] of its opacity. */
private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
    (alpha * 255).toInt().coerceIn(0, 255),
    Color.red(color),
    Color.green(color),
    Color.blue(color)
)

/**
 * This bitmap as a marker icon, or null if the Maps SDK cannot make one.
 *
 * [MapsInitializer] is the whole point of this function. `BitmapDescriptorFactory`
 * throws `IBitmapDescriptorFactory is not initialized` until the SDK has loaded, and
 * an icon built in a `remember {}` runs *before* the `GoogleMap`/`MapView` beside it
 * has loaded anything — which took the playback screen down with a fatal NPE the first
 * time it drew a vehicle. Screens that happened to open after the home map worked only
 * because that map had initialised the SDK for them.
 *
 * Initialising is idempotent and cheap after the first call, so every builder here
 * does it rather than trusting a caller to remember.
 */
private fun Bitmap.toMarkerIcon(context: Context): BitmapDescriptor? = runCatching {
    MapsInitializer.initialize(context)
    BitmapDescriptorFactory.fromBitmap(this)
}.getOrNull()

/** [fraction] of the way from [from] to [to], per channel. */
private fun blendToward(from: Int, to: Int, fraction: Float): Int = Color.rgb(
    (Color.red(from) + (Color.red(to) - Color.red(from)) * fraction).toInt(),
    (Color.green(from) + (Color.green(to) - Color.green(from)) * fraction).toInt(),
    (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * fraction).toInt()
)
