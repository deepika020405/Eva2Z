package com.developer.ev2z.ui.components

import kotlin.math.abs

/**
 * How every live vehicle marker in the app is paced, in one place.
 *
 * Two screens animate the same markers from the same trackers — the fleet map on Home
 * and the single-vehicle map on Track & Trace / Device Report — and they had worked
 * this out separately. Track & Trace measured the real gap between fixes; Home used a
 * flat duration equal to the poll interval, which is not the same number and is not
 * even the right quantity, so its markers lurched and froze while the other screen's
 * glided.
 *
 * The quantity that matters is the gap between *distinct fixes*, never the polling
 * rate. The app asks every few seconds; the tracker uploads on its own schedule, so
 * several polls in a row hand back the identical fix. A glide paced to the poll
 * interval therefore finishes long before the next real position arrives, and the
 * marker stands still for the remainder of every cycle.
 */

/** A jump larger than this is a signal recovery, not travel — snap instead of gliding. */
internal const val SNAP_DEGREES = 0.02   // ≈ 2 km

/**
 * Bounds on the glide duration the measured fix gap is allowed to set. Below the floor
 * the marker would be racing a burst of fixes; above the ceiling a device that went
 * quiet for a minute and came back would crawl for the whole minute.
 */
internal const val MIN_GLIDE_MS = 1_200
internal const val MAX_GLIDE_MS = 12_000

/**
 * The glide is stretched slightly past the measured gap so the next fix always arrives
 * mid-glide. Finishing early is what the eye notices: the marker stops dead for the
 * leftover milliseconds of every cycle. Arriving a little late costs a steady ~10% of
 * one leg in lag — a few metres at city speeds — and that lag does not accumulate,
 * because each new leg is measured from where the marker actually is.
 */
internal const val GLIDE_OVERSHOOT = 1.12f

/** The glide duration for a leg whose predecessor arrived [gapMs] ago. */
internal fun glideDurationMs(gapMs: Long): Int =
    (gapMs.coerceIn(MIN_GLIDE_MS.toLong(), MAX_GLIDE_MS.toLong()) * GLIDE_OVERSHOOT).toInt()

/** Whether this step is too large to be travel — see [SNAP_DEGREES]. */
internal fun isFarJump(
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double
): Boolean = abs(toLat - fromLat) > SNAP_DEGREES || abs(toLng - fromLng) > SNAP_DEGREES
