package com.developer.ev2z.ui.components

import com.developer.ev2z.data.local.model.vehicleSlugForId

/**
 * The vehicle a map marker draws, seen **from above** and facing **up**.
 *
 * Why from above, and why drawn here rather than taken from the server or from an emoji
 * font — the three things tried before this, in order:
 *
 * * The server's own artwork 404s as given (`icon_url` spells the slug with underscores
 *   while the files are served with hyphens) and every file that *does* resolve is the
 *   same SVG — a grey square wrapping one car emoji, only its `<title>` differing — so
 *   a truck, a bus and a scooter all download as a car. The app also has no SVG decoder
 *   on either platform.
 * * An emoji vehicle (🚗 🚌 🛵) is drawn in **side profile**, and side profile cannot be
 *   rotated. Turned by the compass heading it crabs sideways down the road; corrected
 *   for where its nose points it goes upside down on every eastward heading; mirrored
 *   to keep it upright it is still stood on its back wheels at north and south, where a
 *   glyph that much wider than it is tall reads as a sliver. Capping the turn kept it
 *   legible but then it no longer pointed along the road.
 * * Seen from above there is no up or down to get wrong. The artwork is drawn facing up
 *   and turned by the heading — nothing else — which is what every cab-tracking map
 *   does, and it is right at all 360 degrees.
 *
 * So each vehicle is a handful of rounded rectangles in a **unit box**: `0..1` across
 * and down, with `0` at the front of the vehicle. The parts are shared rather than
 * drawn twice because the two platforms rasterise differently — an Android
 * `BitmapDescriptor` through `Canvas`, a MapKit `UIImage` through `UIBezierPath` — and
 * two hand-drawn copies of the same car drift apart. Each renderer walks [parts] in
 * order; see `vehicleMarkerIcon` and `vehicleMarkerImage`.
 */
internal enum class VehicleTopView {
    CAR,
    SUV,
    TAXI,
    POLICE_CAR,
    BIKE,
    SCOOTER,
    BICYCLE,
    AUTO,
    VAN,
    AMBULANCE,
    PICKUP,
    TRUCK,
    FIRE_TRUCK,
    TANKER,
    BUS,
    TRACTOR,
    CONSTRUCTION,
    GOLF_CART,
}

/**
 * How much of the marker the white outline takes, as a fraction of its side.
 *
 * Shared so the two platforms draw the same weight of outline. It is also the margin
 * the artwork is inset by: the outline is a stroke centred on each edge, so half of it
 * falls outside the vehicle, and a vehicle drawn right up to the edge of the bitmap
 * would have that half clipped off.
 */
internal const val MARKER_OUTLINE_FRACTION = 0.035f

/** What a [MarkerPart] is painted with — resolved to real colours by each renderer. */
internal enum class PartPaint {
    /** The vehicle's own colour, which carries its status — see `statusColorOf`. */
    BODY,

    /** The body colour darkened: a roof, a canopy, a load bed. */
    BODY_DARK,

    /** Glass — a windscreen, a rear window, a row of side windows. */
    GLASS,

    /** A tyre, which in a top view only peeks out past the body. */
    TYRE,

    /**
     * A near-white marking on top of the body: an ambulance's cross, a taxi's roof
     * sign, a fire engine's ladder.
     *
     * What lets one silhouette serve several vehicles that really are the same shape
     * from above. A police car *is* a car; painting a light bar on its roof is how a
     * top view says so, and it costs one rectangle instead of a whole new outline.
     */
    LIGHT,
}

/**
 * One rounded rectangle of a vehicle, in the unit box described on [VehicleTopView]:
 * `0..1` left to right and front to back, so a renderer only has to multiply by the
 * marker size it is drawing at.
 */
internal data class MarkerPart(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    /** Corner radius, in the same units — `0.5f` of the shorter side is a capsule. */
    val radius: Float,
    val paint: PartPaint,
)

/**
 * The parts of [vehicle], **in paint order**: tyres, then the body over them, then the
 * darker upper surfaces, then the glass on top.
 *
 * Order matters twice over. The body is filled after the tyres so the half of each tyre
 * that sits under the vehicle disappears, and both renderers trace the white outline
 * under every part *before* filling any of them — stroking part by part would leave
 * white seams across the middle of the vehicle where the parts overlap.
 */
internal fun vehicleTopViewParts(vehicle: VehicleTopView): List<MarkerPart> = when (vehicle) {

    // ── Cars ────────────────────────────────────────────────────────────────
    //
    // A saloon: four tyres at the corners, a cabin between the windscreen and the rear
    // window.
    //
    // Which end is the front has to be readable at a glance, or a vehicle driving north
    // and one reversing south look the same. Three things say it, and all three point
    // the same way: the nose is the narrower of the two body rectangles, the windscreen
    // is wide and sits right behind it, and the rear window is narrower and shorter.
    VehicleTopView.CAR -> car()

    // The same car with a sign on its roof. A taxi seen from above *is* a saloon —
    // inventing a different outline for it would be a lie the eye can check.
    VehicleTopView.TAXI -> car() + listOf(
        MarkerPart(0.39f, 0.50f, 0.61f, 0.60f, 0.03f, PartPaint.LIGHT),
    )

    // And again with a light bar: full width where the taxi's sign is a short box, so
    // the two read apart at marker size.
    VehicleTopView.POLICE_CAR -> car() + listOf(
        MarkerPart(0.29f, 0.45f, 0.71f, 0.53f, 0.02f, PartPaint.LIGHT),
    )

    // Squarer and wider than the saloon, with its tyres pushed to the corners and a
    // tailgate as wide as the windscreen — which is what a 4x4 looks like from above.
    VehicleTopView.SUV -> listOf(
        MarkerPart(0.14f, 0.18f, 0.28f, 0.35f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.18f, 0.86f, 0.35f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.14f, 0.65f, 0.28f, 0.82f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.65f, 0.86f, 0.82f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.20f, 0.05f, 0.80f, 0.95f, 0.11f, PartPaint.BODY),
        MarkerPart(0.25f, 0.30f, 0.75f, 0.80f, 0.08f, PartPaint.BODY_DARK),
        MarkerPart(0.26f, 0.19f, 0.74f, 0.33f, 0.05f, PartPaint.GLASS),
        MarkerPart(0.27f, 0.77f, 0.73f, 0.89f, 0.05f, PartPaint.GLASS),
    )

    // ── Two- and three-wheelers ─────────────────────────────────────────────
    //
    // Narrow, and read by its handlebar: at marker size a two-wheeler seen from above
    // is little more than a line, and the bar across the front is what says which end
    // is the front.
    VehicleTopView.BIKE -> listOf(
        MarkerPart(0.44f, 0.08f, 0.56f, 0.30f, 0.05f, PartPaint.TYRE),
        MarkerPart(0.44f, 0.70f, 0.56f, 0.92f, 0.05f, PartPaint.TYRE),
        MarkerPart(0.40f, 0.22f, 0.60f, 0.78f, 0.09f, PartPaint.BODY),
        MarkerPart(0.24f, 0.30f, 0.76f, 0.38f, 0.04f, PartPaint.BODY_DARK),
        MarkerPart(0.42f, 0.54f, 0.58f, 0.68f, 0.05f, PartPaint.GLASS),
    )

    // A step-through, which is the one thing that separates a scooter from a
    // motorcycle from directly overhead: the body is in two pieces with the flat
    // footboard between them, where the bike's is one continuous mass.
    VehicleTopView.SCOOTER -> listOf(
        MarkerPart(0.45f, 0.05f, 0.55f, 0.24f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.44f, 0.74f, 0.56f, 0.94f, 0.05f, PartPaint.TYRE),
        MarkerPart(0.44f, 0.18f, 0.56f, 0.44f, 0.05f, PartPaint.BODY),
        MarkerPart(0.38f, 0.40f, 0.62f, 0.60f, 0.05f, PartPaint.BODY),
        MarkerPart(0.40f, 0.56f, 0.60f, 0.84f, 0.09f, PartPaint.BODY),
        MarkerPart(0.25f, 0.21f, 0.75f, 0.29f, 0.04f, PartPaint.BODY_DARK),
        MarkerPart(0.43f, 0.62f, 0.57f, 0.76f, 0.06f, PartPaint.GLASS),
    )

    // Thinner again, and with no engine between the wheels — a frame, a bar and a
    // saddle is all there is to see.
    VehicleTopView.BICYCLE -> listOf(
        MarkerPart(0.46f, 0.04f, 0.54f, 0.28f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.46f, 0.72f, 0.54f, 0.96f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.46f, 0.24f, 0.54f, 0.76f, 0.03f, PartPaint.BODY),
        MarkerPart(0.28f, 0.26f, 0.72f, 0.32f, 0.03f, PartPaint.BODY_DARK),
        MarkerPart(0.43f, 0.62f, 0.57f, 0.72f, 0.04f, PartPaint.GLASS),
    )

    // Three wheels — one at the front, two at the back — under a cowl much narrower
    // than the cabin behind it. That difference in width is the whole tell: an auto is
    // the one vehicle here that is not the same width end to end.
    VehicleTopView.AUTO -> listOf(
        MarkerPart(0.45f, 0.07f, 0.55f, 0.23f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.15f, 0.60f, 0.28f, 0.79f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.60f, 0.85f, 0.79f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.35f, 0.05f, 0.65f, 0.46f, 0.15f, PartPaint.BODY),
        MarkerPart(0.22f, 0.36f, 0.78f, 0.94f, 0.13f, PartPaint.BODY),
        MarkerPart(0.26f, 0.48f, 0.74f, 0.90f, 0.10f, PartPaint.BODY_DARK),
        MarkerPart(0.37f, 0.11f, 0.63f, 0.30f, 0.08f, PartPaint.GLASS),
    )

    // ── Vans and trucks ─────────────────────────────────────────────────────
    //
    // One unbroken box from nose to tail — no gap between cab and body, which is what
    // tells a van from the truck below it.
    VehicleTopView.VAN -> van()

    // The same van with a cross on its roof, which is how an ambulance announces
    // itself from the air and how it announces itself here.
    VehicleTopView.AMBULANCE -> van() + listOf(
        MarkerPart(0.47f, 0.47f, 0.53f, 0.71f, 0.01f, PartPaint.LIGHT),
        MarkerPart(0.38f, 0.56f, 0.62f, 0.62f, 0.01f, PartPaint.LIGHT),
    )

    // A cab and a visible gap and then a load bed: the gap is the point, and it is what
    // the van above deliberately does not have.
    VehicleTopView.TRUCK -> truck()

    // A truck carrying its ladder — two rails down the bed, which is the one marking
    // you can actually pick out on a fire engine from overhead.
    VehicleTopView.FIRE_TRUCK -> truck() + listOf(
        MarkerPart(0.41f, 0.40f, 0.46f, 0.93f, 0.01f, PartPaint.LIGHT),
        MarkerPart(0.54f, 0.40f, 0.59f, 0.93f, 0.01f, PartPaint.LIGHT),
    )

    // Shorter than the truck and open behind the cab: the bed stops well before the
    // tail, where the truck's runs to the very end.
    VehicleTopView.PICKUP -> listOf(
        MarkerPart(0.16f, 0.20f, 0.28f, 0.36f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.20f, 0.84f, 0.36f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.16f, 0.62f, 0.28f, 0.78f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.62f, 0.84f, 0.78f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.23f, 0.12f, 0.77f, 0.86f, 0.09f, PartPaint.BODY),
        MarkerPart(0.27f, 0.46f, 0.73f, 0.81f, 0.05f, PartPaint.BODY_DARK),
        MarkerPart(0.28f, 0.17f, 0.72f, 0.30f, 0.05f, PartPaint.GLASS),
    )

    // A cylinder on wheels. The capsule radius is the whole idea — nothing else in the
    // catalogue has round ends, so a tanker is recognisable even at arm's length.
    VehicleTopView.TANKER -> listOf(
        MarkerPart(0.15f, 0.12f, 0.28f, 0.29f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.12f, 0.85f, 0.29f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.15f, 0.70f, 0.28f, 0.87f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.70f, 0.85f, 0.87f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.24f, 0.03f, 0.76f, 0.28f, 0.08f, PartPaint.BODY),
        MarkerPart(0.22f, 0.30f, 0.78f, 0.97f, 0.28f, PartPaint.BODY),
        MarkerPart(0.22f, 0.57f, 0.78f, 0.64f, 0.02f, PartPaint.BODY_DARK),
        MarkerPart(0.29f, 0.06f, 0.71f, 0.18f, 0.04f, PartPaint.GLASS),
    )

    // ── Buses ───────────────────────────────────────────────────────────────
    //
    // Long, square-ended, and the only one with windows down its sides — which is what
    // separates a bus from a truck from above.
    VehicleTopView.BUS -> listOf(
        MarkerPart(0.16f, 0.16f, 0.28f, 0.32f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.16f, 0.84f, 0.32f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.16f, 0.70f, 0.28f, 0.86f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.70f, 0.84f, 0.86f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.22f, 0.03f, 0.78f, 0.97f, 0.13f, PartPaint.BODY),
        MarkerPart(0.36f, 0.26f, 0.64f, 0.88f, 0.08f, PartPaint.BODY_DARK),
        MarkerPart(0.28f, 0.07f, 0.72f, 0.20f, 0.06f, PartPaint.GLASS),
        MarkerPart(0.24f, 0.28f, 0.33f, 0.82f, 0.04f, PartPaint.GLASS),
        MarkerPart(0.67f, 0.28f, 0.76f, 0.82f, 0.04f, PartPaint.GLASS),
    )

    // ── Working vehicles ────────────────────────────────────────────────────
    //
    // Small wheels at the front, very large ones at the back. No other vehicle here is
    // lopsided like that, so it needs no other marking.
    VehicleTopView.TRACTOR -> listOf(
        MarkerPart(0.23f, 0.10f, 0.34f, 0.26f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.66f, 0.10f, 0.77f, 0.26f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.09f, 0.52f, 0.30f, 0.92f, 0.05f, PartPaint.TYRE),
        MarkerPart(0.70f, 0.52f, 0.91f, 0.92f, 0.05f, PartPaint.TYRE),
        MarkerPart(0.34f, 0.06f, 0.66f, 0.90f, 0.08f, PartPaint.BODY),
        MarkerPart(0.30f, 0.46f, 0.70f, 0.84f, 0.07f, PartPaint.BODY_DARK),
        MarkerPart(0.37f, 0.13f, 0.63f, 0.27f, 0.05f, PartPaint.GLASS),
    )

    // An arm out the front — a JCB's boom, a crane's jib, a forklift's mast. From
    // above they are the same animal: a cab with something reaching past the nose.
    VehicleTopView.CONSTRUCTION -> listOf(
        MarkerPart(0.14f, 0.26f, 0.28f, 0.44f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.26f, 0.86f, 0.44f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.14f, 0.64f, 0.28f, 0.84f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.72f, 0.64f, 0.86f, 0.84f, 0.04f, PartPaint.TYRE),
        MarkerPart(0.24f, 0.24f, 0.76f, 0.94f, 0.08f, PartPaint.BODY),
        MarkerPart(0.44f, 0.02f, 0.56f, 0.38f, 0.04f, PartPaint.BODY_DARK),
        MarkerPart(0.30f, 0.48f, 0.70f, 0.86f, 0.06f, PartPaint.BODY_DARK),
        MarkerPart(0.33f, 0.28f, 0.67f, 0.44f, 0.05f, PartPaint.GLASS),
    )

    // Short, open and canopied — it does not fill its box, which at marker size is
    // what makes it read as the small thing it is.
    VehicleTopView.GOLF_CART -> listOf(
        MarkerPart(0.21f, 0.27f, 0.31f, 0.40f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.69f, 0.27f, 0.79f, 0.40f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.21f, 0.59f, 0.31f, 0.72f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.69f, 0.59f, 0.79f, 0.72f, 0.03f, PartPaint.TYRE),
        MarkerPart(0.27f, 0.23f, 0.73f, 0.79f, 0.10f, PartPaint.BODY),
        MarkerPart(0.30f, 0.31f, 0.70f, 0.74f, 0.08f, PartPaint.BODY_DARK),
    )
}

/**
 * The saloon, shared by every vehicle that is a saloon with something painted on it.
 *
 * A function rather than a constant so a caller can append its own marking without
 * being able to mutate the shape every other car is drawn from.
 */
private fun car(): List<MarkerPart> = listOf(
    MarkerPart(0.17f, 0.20f, 0.30f, 0.36f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.70f, 0.20f, 0.83f, 0.36f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.17f, 0.64f, 0.30f, 0.80f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.70f, 0.64f, 0.83f, 0.80f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.23f, 0.17f, 0.77f, 0.94f, 0.15f, PartPaint.BODY),
    MarkerPart(0.28f, 0.05f, 0.72f, 0.42f, 0.16f, PartPaint.BODY),
    MarkerPart(0.27f, 0.36f, 0.73f, 0.74f, 0.11f, PartPaint.BODY_DARK),
    MarkerPart(0.29f, 0.26f, 0.71f, 0.40f, 0.07f, PartPaint.GLASS),
    MarkerPart(0.34f, 0.70f, 0.66f, 0.80f, 0.05f, PartPaint.GLASS),
)

/** The box van, shared with the ambulance drawn on top of it. */
private fun van(): List<MarkerPart> = listOf(
    MarkerPart(0.15f, 0.16f, 0.28f, 0.33f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.72f, 0.16f, 0.85f, 0.33f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.15f, 0.68f, 0.28f, 0.85f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.72f, 0.68f, 0.85f, 0.85f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.21f, 0.04f, 0.79f, 0.96f, 0.10f, PartPaint.BODY),
    MarkerPart(0.24f, 0.28f, 0.76f, 0.92f, 0.07f, PartPaint.BODY_DARK),
    MarkerPart(0.26f, 0.08f, 0.74f, 0.21f, 0.05f, PartPaint.GLASS),
)

/** The flatbed truck, shared with the fire engine drawn on top of it. */
private fun truck(): List<MarkerPart> = listOf(
    MarkerPart(0.15f, 0.12f, 0.28f, 0.28f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.72f, 0.12f, 0.85f, 0.28f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.15f, 0.70f, 0.28f, 0.86f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.72f, 0.70f, 0.85f, 0.86f, 0.04f, PartPaint.TYRE),
    MarkerPart(0.23f, 0.03f, 0.77f, 0.30f, 0.08f, PartPaint.BODY),
    MarkerPart(0.19f, 0.34f, 0.81f, 0.97f, 0.05f, PartPaint.BODY_DARK),
    MarkerPart(0.28f, 0.06f, 0.72f, 0.17f, 0.04f, PartPaint.GLASS),
)

/**
 * How far to turn a marker whose artwork faces up, for a vehicle heading
 * [headingDegrees] — which is the heading itself, wrapped into one turn.
 *
 * All the pose arithmetic this replaces came from the artwork facing west rather than
 * up; see [VehicleTopView]. What is left is the one thing still worth having in common
 * code: a vehicle that has not travelled yet has a NaN heading, and neither a Google
 * Maps `Marker` nor a `CGAffineTransform` will take that — one throws, the other draws
 * nothing. An unknown heading draws the vehicle facing up.
 */
internal fun markerRotationDegrees(headingDegrees: Double): Double =
    if (headingDegrees.isNaN()) 0.0
    else ((headingDegrees % 360.0) + 360.0) % 360.0

/**
 * The vehicle to draw for the slug out of `icon_url` — `LatestLocation.iconSlug` — so
 * the marker shows what the server says the tracker is fitted to.
 *
 * Null when the server named no vehicle, which is every device until `add-device` can
 * store a `vehicle_id`; the caller falls back to the heading arrow. A slug the app has
 * not seen still gets a vehicle rather than an arrow — the catalogue is server-side and
 * grows without a release, and a car is a better guess than nothing.
 */
/**
 * The vehicle to draw, from whichever of the two the server sent.
 *
 * [vehicleId] wins when the catalogue knows it; [slug] covers the endpoint that does
 * not send an id yet and any type added server-side after this build shipped.
 */
internal fun vehicleTopViewOf(vehicleId: Int?, slug: String?): VehicleTopView? =
    vehicleSlugForId(vehicleId)?.let(::vehicleTopViewFor) ?: vehicleTopViewFor(slug)

internal fun vehicleTopViewFor(slug: String?): VehicleTopView? =
    when (slug?.trim()?.lowercase()?.replace('_', '-')?.takeIf { it.isNotEmpty() }) {
        null -> null

        // Every slug `vehicle-types` serves is listed, all 41 of them, rather than
        // leaning on the fallback at the bottom — a vehicle that lands there is drawn
        // as a car, which is silent and wrong rather than obviously wrong.
        //
        // "sedan" is not in `vehicle-types` at all and the server sends it anyway
        // (device 0869727076942661, 2026-10-13), which is why the fallback stays.
        "car", "sedan", "hatchback", "electric-car", "other" -> VehicleTopView.CAR
        "suv", "jeep" -> VehicleTopView.SUV
        "taxi" -> VehicleTopView.TAXI
        "police-car" -> VehicleTopView.POLICE_CAR

        "bike", "motorcycle", "moped", "electric-bike", "atv" -> VehicleTopView.BIKE
        "scooter", "electric-scooter" -> VehicleTopView.SCOOTER
        "bicycle" -> VehicleTopView.BICYCLE
        "auto-rickshaw", "e-rickshaw", "three-wheeler" -> VehicleTopView.AUTO

        "van", "delivery-van", "minibus" -> VehicleTopView.VAN
        "ambulance" -> VehicleTopView.AMBULANCE
        "pickup-truck", "mini-truck" -> VehicleTopView.PICKUP
        "truck", "heavy-truck", "container-truck", "electric-truck",
        "dumper" -> VehicleTopView.TRUCK
        "fire-truck" -> VehicleTopView.FIRE_TRUCK
        "water-tanker", "fuel-tanker" -> VehicleTopView.TANKER

        "bus", "tourist-bus", "electric-bus", "school-bus" -> VehicleTopView.BUS

        "tractor" -> VehicleTopView.TRACTOR
        "jcb", "crane", "forklift" -> VehicleTopView.CONSTRUCTION
        "golf-cart" -> VehicleTopView.GOLF_CART

        else -> VehicleTopView.CAR
    }
