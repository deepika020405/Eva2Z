// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/homeScreen/SmoothDevicePositions.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.ui.components.glideDurationMs
import com.developer.ev2z.ui.components.isFarJump
import com.developer.ev2z.util.bearingBetween
import com.developer.ev2z.util.stepBearing
import io.ktor.util.date.getTimeMillis

/**
 * A marker's drawn position, and which way the vehicle on it is facing there.
 *
 * The heading travels with the position because it is *measured* from it: the only
 * thing the fleet endpoint says about direction is where the vehicle was last time,
 * so the leg being glided is the direction. Working it out at the map instead — which
 * is where it would have to go otherwise — means each platform's map doing the same
 * arithmetic on coordinates it has already been handed, and the two drifting apart.
 */
data class DevicePosition(
    val latitude: Double,
    val longitude: Double,
    /**
     * Degrees clockwise from north, or NaN for a marker that has not yet travelled far
     * enough for a direction to mean anything — a vehicle parked since the screen
     * opened has no heading, and `markerRotationDegrees` draws it facing up.
     */
    val bearingDegrees: Double = Double.NaN,
)

/**
 * One marker's journey from where it was drawn to the fix that has just arrived.
 *
 * Held per device rather than per refresh: a fleet response moves some vehicles and
 * not others, and a marker halfway through its glide must carry on from where it is
 * rather than restart because a different device reported.
 */
private data class GlideLeg(
    val from: GeoPoint,
    val to: GeoPoint,
    val startedAtMs: Long,
    val durationMs: Int,
    /**
     * The direction this leg travels in, held for the whole leg rather than measured
     * per frame: the chord between two fixes has one angle, and re-deriving it from
     * the few metres covered since the last frame is noise — that is what made an
     * earlier marker spin on the spot. NaN for a leg too short to have a direction.
     */
    val bearingDegrees: Double,
)

/**
 * Walks each device's marker from where it is currently drawn to where the server
 * now says it is — and which way it is facing as it goes — rather than letting it
 * teleport.
 *
 * The pace comes from the gap actually observed between that device's last two
 * distinct fixes, not from the poll interval — see
 * [com.developer.ev2z.ui.components.MIN_GLIDE_MS] and friends for why those are
 * different numbers. Pacing to the poll interval is what made these markers move in
 * bursts: the fleet is polled every [DEVICES_POLL_INTERVAL_MS], a tracker uploads far
 * less often, so a glide sized to one poll finished while the following polls returned
 * the identical fix and the marker sat still until the vehicle finally reported again.
 *
 * Interpolation stays in [Double] the whole way. Compose's own `animateFloatAsState`
 * carries values as Float, and a Float's precision around a longitude of 78° is only
 * about a metre — enough to show as jitter on a zoomed-in map.
 *
 * A device drawn for the first time is placed directly at its coordinates: it has no
 * previous position to glide from, and sliding it in from somewhere else would be an
 * invention. A device that jumps implausibly far — a tracker coming back after losing
 * signal — is likewise placed, not driven across the country at speed.
 *
 * Returned map is keyed by `Device.deviceId` and is snapshot-backed, so reading it
 * re-runs the caller on every animation frame.
 */
@Composable
fun rememberSmoothDevicePositions(devices: List<Device>): Map<String, DevicePosition> {
    val drawn = remember { mutableStateMapOf<String, DevicePosition>() }
    // Plain maps: these are read and written inside the animation loop only, and
    // making them snapshot-backed would recompose every reader on every frame for
    // state nothing outside this function looks at.
    val legs = remember { mutableMapOf<String, GlideLeg>() }
    val lastFixAtMs = remember { mutableMapOf<String, Long>() }

    // Only coordinates belong in the key: a status or speed change must not restart
    // a glide that is halfway through.
    val targets = remember(devices) {
        devices.mapNotNull { device ->
            val lat = device.getLatitudeDouble() ?: return@mapNotNull null
            val lng = device.getLongitudeDouble() ?: return@mapNotNull null
            device.deviceId to GeoPoint(lat, lng)
        }.toMap()
    }

    LaunchedEffect(targets) {
        // Forget devices that have left the account or lost their fix.
        drawn.keys.retainAll(targets.keys)
        legs.keys.retainAll(targets.keys)
        lastFixAtMs.keys.retainAll(targets.keys)

        val now = getTimeMillis()

        targets.forEach { (id, target) ->
            val current = drawn[id]

            // Never drawn before — place it, and start the clock that paces its first
            // real glide. No heading yet: one fix says nothing about direction.
            if (current == null) {
                drawn[id] = DevicePosition(target.latitude, target.longitude)
                lastFixAtMs[id] = now
                return@forEach
            }

            // This device didn't report anything new. Either it is already gliding
            // towards this fix, or it arrived and has nothing left to do.
            if (legs[id]?.to == target) return@forEach
            if (legs[id] == null &&
                current.latitude == target.latitude &&
                current.longitude == target.longitude
            ) return@forEach

            val gapMs = lastFixAtMs[id]?.let { now - it }
            lastFixAtMs[id] = now

            val from = GeoPoint(current.latitude, current.longitude)

            if (isFarJump(from.latitude, from.longitude, target.latitude, target.longitude)) {
                // A tracker coming back after losing signal, not a drive. Its last
                // known facing is kept rather than reset: the vehicle did not turn,
                // the report did, and spinning every recovered marker to north would
                // be an invention.
                drawn[id] = DevicePosition(
                    target.latitude, target.longitude, current.bearingDegrees
                )
                legs.remove(id)
                return@forEach
            }

            legs[id] = GlideLeg(
                from = from,
                to = target,
                startedAtMs = now,
                // No gap to measure yet on this device's first move, so the poll
                // interval seeds it; every leg after that is paced by measurement.
                durationMs = gapMs?.let { glideDurationMs(it) }
                    ?: DEVICES_POLL_INTERVAL_MS.toInt(),
                bearingDegrees = bearingBetween(from, target),
            )
        }

        // Drive every leg that is still running. The loop ends when they all finish,
        // and the next fleet response starts it again.
        while (legs.isNotEmpty()) {
            withFrameNanos { }
            val frameAtMs = getTimeMillis()
            val finished = mutableListOf<String>()

            legs.forEach { (id, leg) ->
                val fraction = ((frameAtMs - leg.startedAtMs).toDouble() / leg.durationMs)
                    .coerceIn(0.0, 1.0)
                // Eased towards the leg's direction, not assigned: a vehicle that
                // turned a corner between two fixes should swing round over a few
                // frames the way it did on the road, rather than flick to the new
                // angle in one. A NaN leg bearing leaves the facing alone.
                val bearing = stepBearing(
                    drawn[id]?.bearingDegrees ?: Double.NaN,
                    leg.bearingDegrees,
                )
                drawn[id] = DevicePosition(
                    latitude = leg.from.latitude +
                            (leg.to.latitude - leg.from.latitude) * fraction,
                    longitude = leg.from.longitude +
                            (leg.to.longitude - leg.from.longitude) * fraction,
                    bearingDegrees = bearing,
                )
                if (fraction >= 1.0) finished += id
            }

            finished.forEach { legs.remove(it) }
        }
    }

    return drawn
}
