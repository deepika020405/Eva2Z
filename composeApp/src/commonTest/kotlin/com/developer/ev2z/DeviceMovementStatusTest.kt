package com.developer.ev2z

import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.ui.screens.homeScreen.MOVEMENT_THRESHOLD_METERS
import com.developer.ev2z.ui.screens.homeScreen.positionsById
import com.developer.ev2z.ui.screens.homeScreen.withMovementStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The live map shows Running or Stopped based on whether the coordinates are actually
 * changing between polls, not on the server's own status field. What has to hold is
 * that a vehicle reporting the same spot reads as Stopped, one whose position moves
 * reads as Running, and a parked vehicle's few-metre GPS drift does not flip it back
 * and forth between the two.
 */
class DeviceMovementStatusTest {

    private fun device(
        id: String = "D1",
        lat: String? = "28.6139",
        lng: String? = "77.2090",
        speed: Int = 0,
        status: String = "idle",
    ) = Device(
        deviceId = id,
        deviceName = "Test",
        latitude = lat,
        longitude = lng,
        speed = speed,
        status = status,
        timestamp = null,
        lastSeenMinutesAgo = null,
    )

    /** A point [metres] north of 28.6139,77.2090. */
    private fun north(metres: Double) = GeoPoint(28.6139 + metres / 111_320.0, 77.2090)

    private val here = GeoPoint(28.6139, 77.2090)

    @Test
    fun aVehicleReportingTheSameSpotIsStopped() {
        val stillThere = device().withMovementStatus(previous = here)

        assertEquals("Stopped", stillThere.getStatusDisplay())
        assertEquals("stopped", stillThere.getStatusKind())
    }

    @Test
    fun aVehicleWhoseCoordinatesMovedIsRunning() {
        // 100 m on since the last poll — 24 km/h over a 15s tick.
        val moved = device(lat = north(100.0).latitude.toString())
            .withMovementStatus(previous = here)

        assertEquals("Running", moved.getStatusDisplay())
        assertEquals("running", moved.getStatusKind())
    }

    @Test
    fun gpsDriftAroundAParkedVehicleDoesNotReadAsRunning() {
        // A standing tracker's fix wanders a few metres between reports. Calling that
        // movement would flip the status back and forth while the vehicle is parked.
        listOf(1.0, 5.0, 15.0).forEach { drift ->
            val parked = device(lat = north(drift).latitude.toString())
                .withMovementStatus(previous = here)

            assertEquals("Stopped", parked.getStatusDisplay(), "drifted $drift m")
        }
    }

    @Test
    fun theThresholdItselfCountsAsMovement() {
        val atThreshold = device(lat = north(MOVEMENT_THRESHOLD_METERS + 0.5).latitude.toString())
            .withMovementStatus(previous = here)

        assertEquals("Running", atThreshold.getStatusDisplay())
    }

    @Test
    fun theServerStatusIsOverriddenNotTrusted() {
        // The server calling it "moving" does not make it moving: the coordinates are
        // unchanged, so the screen says Stopped. This is the whole point of the change.
        val serverSaysMoving = device(status = "moving").withMovementStatus(previous = here)
        assertEquals("Stopped", serverSaysMoving.getStatusDisplay())

        val serverSaysParked = device(status = "parked", lat = north(500.0).latitude.toString())
            .withMovementStatus(previous = here)
        assertEquals("Running", serverSaysParked.getStatusDisplay())
    }

    @Test
    fun theFirstSightingOfADeviceFallsBackToItsReportedSpeed() {
        // No previous poll to compare against on the very first load. Showing a moving
        // vehicle as Stopped for 15s while the screen opens would be worse than
        // trusting the speed for one tick.
        assertEquals("Running", device(speed = 40).withMovementStatus(previous = null).getStatusDisplay())
        assertEquals("Stopped", device(speed = 0).withMovementStatus(previous = null).getStatusDisplay())
    }

    @Test
    fun aDeviceWithNoFixIsOfflineNotStopped() {
        // Not reporting at all is a different thing from reporting that it has not
        // moved, and the two must not collapse into the same label.
        assertEquals("Offline", device(lat = null).withMovementStatus(previous = here).getStatusDisplay())
        assertEquals("Offline", device(lat = "0", lng = "0").withMovementStatus(null).getStatusDisplay())
    }

    @Test
    fun theOverviewCountsAgreeWithTheRowsBeneathThem() {
        val response = DeviceResponse(
            success = true,
            userId = 1,
            name = "Test",
            totalDevices = 3,
            // What the server thinks — deliberately wrong, so a pass-through shows up.
            runningCount = 3,
            idleCount = 0,
            parkedCount = 0,
            offlineCount = 0,
            data = listOf(
                device(id = "moving", lat = north(400.0).latitude.toString()),
                device(id = "still"),
                device(id = "nofix", lat = null),
            ),
        )

        val derived = response.withMovementStatus(
            previousPositions = mapOf("moving" to here, "still" to here),
        )

        assertEquals(1, derived.runningCount)
        assertEquals(1, derived.parkedCount)
        assertEquals(1, derived.offlineCount)
        assertEquals(0, derived.idleCount)

        // And the rows say the same thing the counts do.
        assertEquals(
            listOf("Running", "Stopped", "Offline"),
            derived.data.map { it.getStatusDisplay() },
        )
    }

    @Test
    fun everyDeviceWithAFixIsRememberedForTheNextPoll() {
        val positions = listOf(
            device(id = "a"),
            device(id = "b", lat = north(50.0).latitude.toString()),
            device(id = "nofix", lng = null),
        ).positionsById()

        // The one without a fix is left out rather than stored, so when it comes back
        // it is compared against where it actually was.
        assertEquals(setOf("a", "b"), positions.keys)
        assertEquals(here, positions["a"])
    }

    @Test
    fun aCrawlingVehicleSettlesOnOneAnswerRatherThanFlickering() {
        // Storing the current position every poll — not the last position that counted
        // as a move — is what stops slow drift accumulating into a false Running.
        var previous = here
        val driftPerPoll = 3.0
        val labels = mutableListOf<String>()

        repeat(10) { poll ->
            val reported = north(driftPerPoll * (poll + 1))
            labels += device(lat = reported.latitude.toString())
                .withMovementStatus(previous)
                .getStatusDisplay()
            previous = reported
        }

        // 30 m of accumulated drift, never more than 3 m in any one poll.
        assertTrue(labels.all { it == "Stopped" }, "flickered: $labels")
    }
}