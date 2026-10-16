package com.developer.ev2z.util

import androidx.compose.runtime.Composable

/**
 * Where the phone is and which way it is pointing — what the map draws a compass cone
 * from, the way Google Maps does.
 *
 * Deliberately separate from the vehicle markers. A tracker's heading comes from its
 * own fixes and belongs to the vehicle, which is usually nowhere near the phone; this
 * belongs to the person holding it. Mixing the two would have a parked car in Noida
 * swing round because the owner turned on their sofa in Bengaluru.
 */
data class DeviceFacing(
    val latitude: Double,
    val longitude: Double,
    /**
     * Compass heading in degrees clockwise from true north, corrected for how the
     * phone itself is held: the same room faces the same way whether the device is
     * portrait, upside down or landscape.
     *
     * NaN while the sensor has not reported yet, or on a device without a usable
     * magnetometer — the cone is not drawn then, rather than drawn pointing north.
     */
    val headingDegrees: Double,
) {
    val hasHeading: Boolean get() = !headingDegrees.isNaN()
}

/**
 * The phone's position and compass heading, updating as either changes, or null until
 * both are known.
 *
 * Null is the normal state for a good part of the app's life — before the sensor's
 * first reading, with location permission refused, on a device with no compass — so
 * every caller has to treat it as "do not draw the cone" rather than as an error.
 *
 * Nothing is registered until this enters composition and everything is unregistered
 * when it leaves: a magnetometer left running costs battery on a screen nobody is
 * looking at.
 */
@Composable
expect fun rememberDeviceFacing(): DeviceFacing?

/**
 * How much of the remaining turn the compass cone takes per reading.
 *
 * A raw magnetometer wobbles by several degrees while the phone is sitting still, and
 * drawn straight it makes the cone twitch. Low enough to settle that down, high enough
 * that turning on the spot keeps up.
 */
internal const val COMPASS_EASE = 0.2

/**
 * How far the eased heading has to move before it is published to composition.
 *
 * The compass listener fires about sixteen times a second and the heading it produces
 * is Compose state read in the body of every map screen that draws a cone, so each
 * reading recomposed a whole map screen. Almost all of those were worthless: a phone
 * sitting still still wobbles by a fraction of a degree, so the eased value never
 * repeats exactly and every reading looked like a change.
 *
 * A degree is well inside the width of the cone being drawn, and a phone actually
 * being turned crosses it in a reading or two — so the cone still tracks the hand
 * holding it, while a stationary one stops recomposing altogether.
 */
internal const val COMPASS_PUBLISH_DEGREES = 1.0
