// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/homeScreen/DeviceAddress.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.util.getAddressFromLatLng
import kotlin.math.round

/**
 * Turns a vehicle's current coordinates into the street address the owner recognises.
 *
 * The live map had no location text on it at all — a marker on a map and a device id,
 * so "where is my vehicle" could only be answered by reading the map. This puts the
 * address on the row next to the vehicle, and keeps it on the coordinates the device
 * is reporting now: while live tracking is running the address follows the vehicle
 * down the road rather than staying on where it was first seen.
 *
 * What it gives you is the postal address ("Sector 57, Noida, Uttar Pradesh"), not the
 * name of the business parked outside. Both platform geocoders answer with addresses;
 * a shop name like "Planet Retail" is a point-of-interest lookup, which is a different
 * (and billed) Places call — see the note on [DEVICE_ADDRESS_PRECISION].
 */

/**
 * Coordinates are rounded to this many decimals to form the cache key — 4 decimals is
 * about 11 m, so a parked vehicle keeps hitting the same entry however much its fix
 * drifts, and a 15s poll does not re-ask for an address that cannot have changed.
 */
internal const val DEVICE_ADDRESS_PRECISION = 4

/** Keeps a long drive from growing the cache without bound. */
internal const val DEVICE_ADDRESS_CACHE_LIMIT = 200

/**
 * Resolved addresses, keyed by rounded coordinates.
 *
 * Deliberately outside composition: the overview sheet is rebuilt every poll, and a
 * `remember` inside it would throw the addresses away and re-geocode all of them each
 * time the sheet is reopened.
 *
 * Snapshot-backed, so whichever caller resolves a key first publishes it to every
 * other caller reading the same one. The same vehicle's address is asked for from
 * three places at once — the overview row, the device picker and the map pin — and a
 * plain map meant all three missed, and all three geocoded the same coordinates.
 */
private val addressCache = mutableStateMapOf<String, String>()

/** Keys with a lookup already running, so the three callers above share one request. */
private val lookupsInFlight = mutableSetOf<String>()

/**
 * The last address actually shown for each device.
 *
 * Kept per device rather than per coordinate so a vehicle driving into a bucket that
 * has not been geocoded yet keeps the previous address on the row. Without it the row
 * would blank out every time the vehicle moved and fill back in a moment later, which
 * reads as flickering all the way down the road.
 */
private val lastShownByDevice = mutableStateMapOf<String, String>()

/**
 * The street address for [device] at the coordinates it is reporting right now, or
 * null until the first lookup for those coordinates comes back.
 *
 * This follows the vehicle: every poll brings new coordinates, and once they move out
 * of the current [DEVICE_ADDRESS_PRECISION] bucket the address is looked up again, so
 * a vehicle being tracked live shows where it is now rather than where it set off
 * from. A standing vehicle stays in one bucket and never re-asks.
 *
 * The previous address stays on screen while the next one resolves — blanking the row
 * on every poll would make it flicker all the way down the road.
 */
@Composable
fun rememberDeviceAddress(device: Device): String? {
    val latitude = device.getLatitudeDouble()
    val longitude = device.getLongitudeDouble()

    if (latitude == null || longitude == null) return null

    val key = deviceAddressKey(latitude, longitude)

    // Snapshot read: when the lookup below writes the key, every composable showing
    // this device recomposes with the address, without any of them owning the state.
    val resolved = addressCache[key]

    // SideEffect, not a plain assignment: writing snapshot state from inside
    // composition is what makes a composable re-invalidate itself. This runs once the
    // composition has committed.
    if (resolved != null) {
        SideEffect { lastShownByDevice[device.deviceId] = resolved }
    }

    LaunchedEffect(key) {
        if (addressCache.containsKey(key)) return@LaunchedEffect
        // add() returns false when another caller already started this exact lookup.
        if (!lookupsInFlight.add(key)) return@LaunchedEffect

        try {
            val address = runCatching { getAddressFromLatLng(latitude, longitude) }
                .getOrNull()
                ?.takeIf { it.isUsableAddress() }
                ?: return@LaunchedEffect

            // Oldest entry out first. A failed lookup is never stored, so the next
            // poll retries it rather than showing a permanent blank for what may have
            // been a dropped connection or a throttled geocoder.
            if (addressCache.size >= DEVICE_ADDRESS_CACHE_LIMIT) {
                addressCache.keys.firstOrNull()?.let { addressCache.remove(it) }
            }
            addressCache[key] = address
        } finally {
            lookupsInFlight.remove(key)
        }
    }

    return resolved ?: lastShownByDevice[device.deviceId]
}

/**
 * Addresses for a whole list of devices, keyed by `deviceId`.
 *
 * For callers that cannot ask per device: iOS builds its pins inside a `UIKitView`
 * update block, which is not a composable scope, so the addresses have to be resolved
 * in composition and handed in as plain data.
 */
@Composable
fun rememberDeviceAddresses(devices: List<Device>): Map<String, String> {
    val addresses = mutableMapOf<String, String>()

    // key() per device, because a device leaving the account shifts every later entry
    // along: without it the remembered slots would hand one vehicle's address to
    // whichever vehicle moved up into its position.
    for (device in devices) {
        key(device.deviceId) {
            rememberDeviceAddress(device)?.let { addresses[device.deviceId] = it }
        }
    }

    return addresses
}

/** Rounded coordinates, as a stable cache key. */
internal fun deviceAddressKey(latitude: Double, longitude: Double): String {
    val scale = tenTo(DEVICE_ADDRESS_PRECISION)
    return "${round(latitude * scale) / scale},${round(longitude * scale) / scale}"
}

private fun tenTo(power: Int): Double {
    var result = 1.0
    repeat(power) { result *= 10 }
    return result
}

/**
 * Both platform providers report failure by returning a sentence rather than throwing
 * — Android answers "Unknown location" / "Location unavailable". Those must not be
 * cached as if they were an address, or the row shows the failure forever.
 */
internal fun String.isUsableAddress(): Boolean {
    val trimmed = trim()
    if (trimmed.isEmpty()) return false
    return trimmed.lowercase() !in NON_ADDRESSES
}

private val NON_ADDRESSES = setOf("unknown location", "location unavailable", "unknown", "null")