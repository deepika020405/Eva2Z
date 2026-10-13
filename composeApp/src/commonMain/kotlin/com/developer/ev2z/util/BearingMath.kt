package com.developer.ev2z.util

import kotlin.math.abs

/**
 * Eases [drawn] towards [target] along the shorter arc, by [ease] of what is left.
 *
 * Interpolating the raw numbers is not enough: 350° → 10° is a twenty-degree turn to
 * the right, but the plain difference is -340°, which spins whatever is being drawn
 * almost all the way round the wrong way. Wrapping the delta into ±180° picks the
 * direction the thing actually turned.
 *
 * A NaN [target] leaves the heading alone — there is nothing to turn towards — and a
 * NaN [drawn] takes the target whole, so a first heading is drawn rather than eased
 * into from north.
 *
 * Shared because three things need the same arithmetic and the same answer: the live
 * vehicle marker, the playback marker, and the compass cone showing which way the
 * phone is pointing.
 */
internal fun easeBearing(drawn: Double, target: Double, ease: Double): Double {
    if (target.isNaN()) return drawn
    if (drawn.isNaN()) return target
    val delta = (target - drawn + 540.0) % 360.0 - 180.0
    if (abs(delta) < 0.5) return target
    return (drawn + delta * ease + 360.0) % 360.0
}
