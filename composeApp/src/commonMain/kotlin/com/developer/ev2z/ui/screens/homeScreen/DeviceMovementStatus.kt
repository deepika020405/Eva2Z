// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/homeScreen/DeviceMovementStatus.kt
package com.developer.ev2z.ui.screens.homeScreen

import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.approxDistanceMeters

/**
 * Decides Running vs Stopped from whether the coordinates are actually changing,
 * instead of taking the server's `status` word for it.
 *
 * The rule is the one the live map is meant to show: a vehicle whose position moves
 * between two polls is running, and one reporting the same spot is stopped. The server
 * field was telling a different story — it carries its own notion of moving/idle/parked
 * that did not match what the coordinates were doing on screen.
 *
 * Polls land every 15s (`HomeViewModel.DEVICES_POLL_INTERVAL_MS`), so a running vehicle
 * moves a long way between two of them and a stopped one does not move at all. Only the
 * previous poll's position is kept per device; comparing against it is what makes this
 * work without any history from the server.
 */

/**
 * How far a vehicle must move between two polls to count as running.
 *
 * A standing vehicle's reported position still wanders a few metres as the fix is
 * recomputed, so a bare "did the numbers change" test would flicker between Running and
 * Stopped while parked. 20 m is clear of that drift, and over a 15s poll it means
 * anything above about 5 km/h reads as running — below that the vehicle is crawling,
 * which is the answer the screen wants anyway.
 */
internal const val MOVEMENT_THRESHOLD_METERS = 20.0

/** The status values this produces, in the spelling `Device.getStatusKind()` reads. */
internal const val STATUS_RUNNING = "moving"
internal const val STATUS_STOPPED = "stopped"
internal const val STATUS_OFFLINE = "offline"

/**
 * The devices with their status replaced by what their coordinates say, and the counts
 * recomputed to match.
 *
 * The counts are rebuilt rather than passed through because the server's
 * `running_count` is derived from the same `status` field being overridden here —
 * leaving it alone would put "Running 1" at the top of the overview sheet above a row
 * reading "Stopped".
 */
internal fun DeviceResponse.withMovementStatus(
    previousPositions: Map<String, GeoPoint>,
): DeviceResponse {
    val updated = data.map { it.withMovementStatus(previousPositions[it.deviceId]) }

    return copy(
        data = updated,
        runningCount = updated.count { it.getStatusKind() == "running" },
        // Every non-running vehicle with a fix lands in the sheet's Parked column: the
        // derived status is a two-way split, so there is nothing left to call idle.
        idleCount = 0,
        parkedCount = updated.count { it.getStatusKind() == "stopped" },
        offlineCount = updated.count { it.getStatusKind() == "offline" },
    )
}

/**
 * One device's status, decided against where it was on the previous poll.
 *
 * [previous] is null the first time a device is seen — on the very first load, or when
 * it has just been added. There is nothing to compare against yet, so the reported
 * speed stands in for one poll rather than calling a moving vehicle stopped while the
 * screen opens.
 */
internal fun Device.withMovementStatus(previous: GeoPoint?): Device {
    val current = currentPosition()

    val status = when {
        // No usable fix at all: the vehicle is not reporting, which is a different
        // thing from reporting that it has not moved.
        current == null -> STATUS_OFFLINE

        previous == null -> if (speed > 0) STATUS_RUNNING else STATUS_STOPPED

        approxDistanceMeters(previous, current) >= MOVEMENT_THRESHOLD_METERS ->
            STATUS_RUNNING

        else -> STATUS_STOPPED
    }

    return copy(status = status)
}

/** Where the device is now, or null if it has no valid fix. */
internal fun Device.currentPosition(): GeoPoint? {
    val lat = getLatitudeDouble() ?: return null
    val lng = getLongitudeDouble() ?: return null
    return GeoPoint(lat, lng)
}

/**
 * The positions to compare the next poll against.
 *
 * Every device's current position is stored, not just the ones that moved. Keeping the
 * last *moved* position instead would let a parked vehicle's few-metre drift accumulate
 * until it crossed the threshold and announced itself as running.
 *
 * Devices without a fix are left out rather than stored as absent, so a vehicle that
 * drops offline and comes back is compared against where it actually was.
 */
internal fun List<Device>.positionsById(): Map<String, GeoPoint> =
    mapNotNull { device -> device.currentPosition()?.let { device.deviceId to it } }.toMap()