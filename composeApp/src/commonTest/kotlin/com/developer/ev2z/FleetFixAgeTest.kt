package com.developer.ev2z

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.data.local.model.RUNNING_COLOR
import com.developer.ev2z.data.local.model.STOPPED_COLOR
import com.developer.ev2z.data.local.model.fleetMarkerColorOf
import com.developer.ev2z.data.local.model.formatFixAge
import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.network.ApiJson
import com.developer.ev2z.ui.screens.homeScreen.MovementStatusTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `user/devices/latest-locations` reports how old each device's fix is, and the live
 * map used to drop it.
 *
 * The account below is the real one that sent us here: three devices, one of them
 * moving right now and the other two last heard from six hours and five days ago. All
 * three drew the same kind of pin, all three had the same claim on the word "live", and
 * none of them carried a vehicle — `vehicle_id` is null throughout, so every marker is
 * the same grey arrow and the chip is the only thing telling them apart.
 */
class FleetFixAgeTest {

    private val fleet = """
        {
          "success": true,
          "user_id": 2826,
          "username": "Neetesh Nayak",
          "name": "Neetesh Nayak",
          "total_devices": 3,
          "running_count": 1,
          "idle_count": 0,
          "parked_count": 0,
          "offline_count": 2,
          "data": [
            {
              "device_id": "0356218606631586",
              "device_name": "ka 03 AJ 9053",
              "vehicle_id": null, "vehicle_name": null, "icon": null, "icon_url": null,
              "latitude": "12.900704", "longitude": "77.641759",
              "speed": 0, "status": "offline",
              "timestamp": "2026-10-10 05:43:50",
              "last_seen_minutes_ago": 7394
            },
            {
              "device_id": "0356218606632014",
              "device_name": "KA 03 AH 0291",
              "vehicle_id": null, "vehicle_name": null, "icon": null, "icon_url": null,
              "latitude": "12.923781", "longitude": "77.521716",
              "speed": 12, "status": "moving",
              "timestamp": "2026-10-15 08:58:31",
              "last_seen_minutes_ago": 0
            },
            {
              "device_id": "0356218606630471",
              "device_name": "Ka 01el 1839",
              "vehicle_id": null, "vehicle_name": null, "icon": null, "icon_url": null,
              "latitude": "12.905156", "longitude": "77.644388",
              "speed": 0, "status": "offline",
              "timestamp": "2026-10-15 03:04:53",
              "last_seen_minutes_ago": 353
            }
          ]
        }
    """.trimIndent()

    private val response = ApiJson.decodeFromString(DeviceResponse.serializer(), fleet)

    /** Every device the account owns reaches the map, each at its own coordinates. */
    @Test
    fun allThreeDevicesArriveWithTheirOwnPositions() {
        val devices = response.data
        assertEquals(3, devices.size)
        assertEquals(3, response.totalDevices)
        assertEquals("Neetesh Nayak", response.displayName)
        assertTrue(devices.all { it.hasValidLocation() })

        assertEquals(12.900704 to 77.641759, devices[0].getLatitudeDouble()!! to devices[0].getLongitudeDouble()!!)
        assertEquals(12.923781 to 77.521716, devices[1].getLatitudeDouble()!! to devices[1].getLongitudeDouble()!!)
        assertEquals(12.905156 to 77.644388, devices[2].getLatitudeDouble()!! to devices[2].getLongitudeDouble()!!)
    }

    /** `moving` is the server's word for running, and the counts agree with the rows. */
    @Test
    fun statusesMatchTheCountsTheHeaderReports() {
        assertEquals(1, response.runningCount)
        assertEquals(2, response.offlineCount)
        assertEquals(listOf("offline", "running", "offline"), response.data.map { it.getStatusKind() })
        assertEquals(1, response.data.count { it.getStatusKind() == "running" })
        assertEquals(2, response.data.count { it.getStatusKind() == "offline" })
    }

    @Test
    fun aStaleFixSaysHowOldItIsAndALiveOneSaysNothing() {
        val (fiveDays, live, sixHours) = Triple(
            response.data[0], response.data[1], response.data[2]
        )

        assertEquals("5d ago", fiveDays.fixAgeLabel())
        assertTrue(fiveDays.isFixStale())

        // Reported zero minutes ago: nothing to say, and the chip stays a plain name.
        assertNull(live.fixAgeLabel())
        assertFalse(live.isFixStale())

        // 353 minutes floored to hours, not rounded up to six: the age exists to be
        // distrusted by, and overstating it is its own kind of wrong.
        assertEquals("5h ago", sixHours.fixAgeLabel())
        assertTrue(sixHours.isFixStale())
    }

    /**
     * What the pins are labelled with. Without a vehicle assigned every marker is the
     * same arrow, so the registration is what tells three grey pins apart.
     */
    @Test
    fun theMapLabelCarriesTheRegistrationAndTheAgeWhenThereIsOne() {
        assertEquals("ka 03 AJ 9053 · 5d ago", response.data[0].mapLabel())
        assertEquals("KA 03 AH 0291", response.data[1].mapLabel())
        assertEquals("Ka 01el 1839 · 5h ago", response.data[2].mapLabel())

        // No vehicle on any of them, which is what falls back to the heading arrow.
        assertTrue(response.data.all { it.iconSlug == null })
    }

    /**
     * A tracker that has not reported for days must not come out as a parked vehicle.
     *
     * The live map decides Running vs Stopped from whether the coordinates move, which
     * is right — but a dead tracker keeps returning the last position it ever had, so it
     * "has not moved", and the verdict came back **Parked**. On this account that put
     * "Parked 2 · Offline 0" at the top of the overview sheet while the payload it was
     * built from said `parked_count: 0, offline_count: 2`, and the two dead trackers sat
     * on the map in red as though the vehicles were simply standing still.
     */
    @Test
    fun aDeadTrackerStaysOfflineRatherThanBecomingParked() {
        val tracker = MovementStatusTracker()
        val shown = tracker.apply(response, nowMs = 0L)

        assertEquals(listOf("offline", "running", "offline"), shown.data.map { it.getStatusKind() })
        assertEquals(1, shown.runningCount)
        assertEquals(0, shown.parkedCount)
        assertEquals(2, shown.offlineCount)
        // The server's own counts, which the screen now agrees with again.
        assertEquals(response.runningCount, shown.runningCount)
        assertEquals(response.offlineCount, shown.offlineCount)
    }

    /**
     * And still offline on the next window, where the carried verdict used to decide it.
     */
    @Test
    fun theVerdictCarriedAcrossAPollDoesNotResurrectADeadTracker() {
        val tracker = MovementStatusTracker()
        tracker.apply(response, nowMs = 0L)

        // Mid-window: the last verdict is carried for everything still reporting.
        val midWindow = tracker.apply(response, nowMs = 5_000L)
        assertEquals(2, midWindow.offlineCount)
        assertEquals(0, midWindow.parkedCount)

        // Past the window the verdict is decided afresh, which is exactly the path that
        // used to call the dead trackers parked. They stay offline.
        val nextWindow = tracker.apply(response, nowMs = 20_000L)
        assertEquals(2, nextWindow.offlineCount)

        // The reporting vehicle, on the other hand, really has been at one spot for the
        // whole window — the same payload replayed — so Stopped is the right answer for
        // it, and the one that distinguishes "not moving" from "not reporting".
        assertEquals(1, nextWindow.parkedCount)
        assertEquals(0, nextWindow.runningCount)
    }

    /**
     * What the markers are actually painted: green for the vehicle that is moving, red
     * for the two that are not.
     *
     * The status kinds above are the right answer for the counts and the rows, where
     * "offline" is a distinction worth drawing — but they used to be the marker colour
     * too, and the grey that came out of them is why two standing vehicles looked like
     * map furniture on a screen called Live Location. The age they carry in the chip is
     * what keeps a red five-day-old fix from claiming to have just parked.
     */
    @Test
    fun theMarkersAreGreenMovingAndRedStandingStill() {
        val shown = MovementStatusTracker().apply(response, nowMs = 0L).data

        assertEquals(
            listOf(STOPPED_COLOR, RUNNING_COLOR, STOPPED_COLOR),
            shown.map { fleetMarkerColorOf(it.getStatusKind()) },
        )
        // No grey anywhere on the map, however quiet the tracker has been.
        assertTrue(shown.none { fleetMarkerColorOf(it.getStatusKind()) == statusColorOf("offline") })
    }

    @Test
    fun theAgeIsSpelledTheSameWayEverywhere() {
        assertEquals("just now", formatFixAge(0))
        assertEquals("1m ago", formatFixAge(1))
        assertEquals("59m ago", formatFixAge(59))
        assertEquals("1h ago", formatFixAge(60))
        assertEquals("23h ago", formatFixAge(1439))
        assertEquals("1d ago", formatFixAge(1440))
        assertEquals("5d ago", formatFixAge(7394))
    }

    /**
     * The other endpoint counts in seconds under a different key — see
     * `last_seen_seconds_ago` on live-location — and the same label has to come out, or
     * the fleet map and the single-vehicle map would disagree about one tracker.
     */
    @Test
    fun secondsFromTheOtherEndpointGiveTheSameLabel() {
        val inSeconds = """
            {
              "success": true,
              "data": [
                {
                  "device_id": "0356218606632014",
                  "device_name": "KA 03 AH 0291",
                  "latitude": "12.9", "longitude": "77.5",
                  "status": "parked",
                  "last_seen_seconds_ago": 21180
                }
              ]
            }
        """.trimIndent()

        val device = ApiJson
            .decodeFromString(DeviceResponse.serializer(), inSeconds)
            .data.single()

        assertEquals(353, device.lastSeenMinutes())
        assertEquals("5h ago", device.fixAgeLabel())
    }
}
