// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/homeScreen/SmoothDevicePositions.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.developer.ev2z.data.local.model.Device

/** A marker's drawn position — latitude to longitude. */
typealias DevicePosition = Pair<Double, Double>

/** How long a marker takes to travel from its drawn position to the new one. */
private const val GLIDE_DURATION_MS = 1_500.0

/**
 * Walks each device's marker from where it is currently drawn to where the server
 * now says it is, rather than letting it teleport.
 *
 * Coordinates refresh every 15s (`HomeViewModel.startDevicesPolling`) and a moving
 * vehicle covers a visible distance in that time, so applying the new position in a
 * single frame reads as a glitch instead of as movement.
 *
 * Interpolation stays in [Double] the whole way. Compose's own `animateFloatAsState`
 * carries values as Float, and a Float's precision around a longitude of 78° is only
 * about a metre — enough to show as jitter on a zoomed-in map.
 *
 * A device drawn for the first time is placed directly at its coordinates: it has no
 * previous position to glide from, and sliding it in from somewhere else would be an
 * invention.
 *
 * Returned map is keyed by `Device.deviceId` and is snapshot-backed, so reading it
 * re-runs the caller on every animation frame.
 */
@Composable
fun rememberSmoothDevicePositions(devices: List<Device>): Map<String, DevicePosition> {
    val drawn = remember { mutableStateMapOf<String, DevicePosition>() }

    // Only coordinates belong in the key: a status or speed change must not restart
    // a glide that is halfway through.
    val targets = remember(devices) {
        devices.mapNotNull { device ->
            val lat = device.getLatitudeDouble() ?: return@mapNotNull null
            val lng = device.getLongitudeDouble() ?: return@mapNotNull null
            device.deviceId to (lat to lng)
        }.toMap()
    }

    LaunchedEffect(targets) {
        // Forget devices that have left the account or lost their fix.
        drawn.keys.retainAll(targets.keys)

        // Where this leg starts: what is on screen now, or the target itself for a
        // device that has never been drawn.
        val from = targets.mapValues { (id, target) -> drawn[id] ?: target }
        from.forEach { (id, position) -> drawn[id] = position }

        // Nothing actually moved — a poll that returned the same coordinates.
        if (from.all { (id, position) -> position == targets[id] }) return@LaunchedEffect

        val startedAt = withFrameNanos { it }
        while (true) {
            val elapsedMs = (withFrameNanos { it } - startedAt) / 1_000_000.0
            val fraction = (elapsedMs / GLIDE_DURATION_MS).coerceAtMost(1.0)

            targets.forEach { (id, target) ->
                val origin = from.getValue(id)
                drawn[id] = (origin.first + (target.first - origin.first) * fraction) to
                        (origin.second + (target.second - origin.second) * fraction)
            }

            if (fraction >= 1.0) return@LaunchedEffect
        }
    }

    return drawn
}