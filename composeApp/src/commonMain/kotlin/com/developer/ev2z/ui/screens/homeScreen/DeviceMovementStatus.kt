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
 * The comparison is made over [MOVEMENT_WINDOW_MS], which is a separate thing from how
 * often positions are polled — see [MovementStatusTracker] for why the two must not be
 * the same number.
 */

/**
 * How far a vehicle must move across the comparison window to count as running.
 *
 * A standing vehicle's reported position still wanders a few metres as the fix is
 * recomputed, so a bare "did the numbers change" test would flicker between Running and
 * Stopped while parked. 20 m is clear of that drift, and over [MOVEMENT_WINDOW_MS] it
 * means anything above about 5 km/h reads as running — below that the vehicle is
 * crawling, which is the answer the screen wants anyway.
 */
internal const val MOVEMENT_THRESHOLD_METERS = 20.0

/**
 * How far apart the two positions being compared have to be in *time*.
 *
 * [MOVEMENT_THRESHOLD_METERS] only means "about 5 km/h" against a span this wide; over a
 * 5s span the same 20 m would mean 14 km/h, so every vehicle in city traffic would read
 * as Stopped. Worse, trackers upload on their own schedule — often slower than the app
 * polls — so consecutive polls frequently return the *identical* fix, and a verdict
 * taken on those would call a moving vehicle stopped.
 *
 * Holding the window at 15s is what keeps the verdict meaningful while positions refresh
 * as fast as the poll interval allows.
 */
internal const val MOVEMENT_WINDOW_MS = 15_000L

/** The status values this produces, in the spelling `Device.getStatusKind()` reads. */
internal const val STATUS_RUNNING = "moving"
internal const val STATUS_STOPPED = "stopped"
internal const val STATUS_OFFLINE = "offline"

/**
 * The devices with their status replaced by what their coordinates say, and the counts
 * recomputed to match.
 *
 * [previousPositions] is the reference the current fix is measured against, and it has
 * to be [MOVEMENT_WINDOW_MS] old for the verdict to mean anything — which is
 * [MovementStatusTracker]'s job, not this function's. Call this directly only where that
 * is already guaranteed.
 */
internal fun DeviceResponse.withMovementStatus(
    previousPositions: Map<String, GeoPoint>,
): DeviceResponse = withStatuses { it.withMovementStatus(previousPositions[it.deviceId]).status }

/**
 * The devices restamped with [statusOf], and the counts rebuilt from the result.
 *
 * The counts are rebuilt rather than passed through because the server's
 * `running_count` is derived from the same `status` field being overridden here —
 * leaving it alone would put "Running 1" at the top of the overview sheet above a row
 * reading "Stopped".
 */
private fun DeviceResponse.withStatuses(statusOf: (Device) -> String): DeviceResponse {
    val updated = data.map { it.copy(status = statusOf(it)) }

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
 * Keeps the Running/Stopped verdict on a [MOVEMENT_WINDOW_MS] cadence while positions
 * refresh at whatever rate the caller polls.
 *
 * The two used to be the same thing: every poll became the reference the next poll was
 * compared against. That only worked while the poll interval happened to equal the
 * window, and polling faster silently broke the verdict — a 5s span is too short to tell
 * 20 m of travel from 20 m of GPS drift, and a tracker that uploads slower than the app
 * polls hands back the same fix twice, which reads as "has not moved".
 *
 * So the reference position is held until it is old enough to be worth comparing against,
 * and in between the last verdict is carried. Offline is decided fresh on every poll
 * regardless: losing the fix is a property of the response in hand, not of movement.
 */
internal class MovementStatusTracker(private val windowMs: Long = MOVEMENT_WINDOW_MS) {

    private var reference: Map<String, GeoPoint> = emptyMap()
    private var referenceAtMs: Long? = null
    private var verdict: Map<String, String> = emptyMap()

    fun apply(response: DeviceResponse, nowMs: Long): DeviceResponse {
        val windowElapsed = referenceAtMs?.let { nowMs - it >= windowMs } ?: true

        if (windowElapsed) {
            val decided = response.withMovementStatus(reference)
            verdict = decided.data.associate { it.deviceId to it.status }
            reference = response.data.positionsById()
            referenceAtMs = nowMs
            return decided
        }

        return response.withStatuses { device ->
            when {
                device.isNotReporting() -> STATUS_OFFLINE
                // A device first seen mid-window has no verdict to carry, so it falls
                // back to its reported speed exactly as a first sighting does.
                else -> verdict[device.deviceId] ?: device.withMovementStatus(null).status
            }
        }
    }
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
        // Not reporting at all, which is a different thing from reporting that it has
        // not moved — and the two were being conflated. See [isNotReporting]. The null
        // test is spelled out rather than left to the helper so the branches below keep
        // their smart cast on `current`.
        current == null || isNotReporting() -> STATUS_OFFLINE

        previous == null -> if (speed > 0) STATUS_RUNNING else STATUS_STOPPED

        approxDistanceMeters(previous, current) >= MOVEMENT_THRESHOLD_METERS ->
            STATUS_RUNNING

        else -> STATUS_STOPPED
    }

    return copy(status = status)
}

/**
 * Whether this tracker is not reporting, whatever its last coordinates say.
 *
 * A dead tracker does not stop sending a position — the fleet endpoint keeps returning
 * the last one it ever had, days old, with `status: "offline"` and
 * `last_seen_minutes_ago` in the thousands. So the no-fix test below is not enough on
 * its own, and the movement rule ran on those coordinates instead: a tracker silent for
 * five days had "not moved", which came out as **Parked**, in red, next to a count of
 * zero offline devices — while the server's own payload said two were offline.
 *
 * The server's word is taken for this and only this. Running versus stopped is still
 * decided from the coordinates, because that is the judgement the server was getting
 * wrong; whether the tracker is reporting at all is the one thing only the server knows.
 */
internal fun Device.isNotReporting(): Boolean =
    currentPosition() == null || getStatusKind() == STATUS_OFFLINE

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