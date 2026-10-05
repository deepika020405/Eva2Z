package com.developer.ev2z

import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.ui.screens.HistoryPlayback.dropStaleBeaconFixes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun fix(lat: Double, lng: Double, speed: Int = 0, at: String = "2026-10-05 04:00:00") =
    RoutePoint(
        latitude = lat.toString(),
        longitude = lng.toString(),
        speed = speed.toString(),
        createdAt = at,
        movement = if (speed == 0) "parked" else "moving",
    )

/** The depot coordinate the history endpoint keeps re-emitting. */
private val BASE = fix(28.603899, 77.352813)

class StaleFixFilterTest {

    /**
     * The trace that sent us here: 2026-10-05, device 0869727076942661. One parked
     * coordinate interleaved between every pair of moving fixes, drawn as a star of
     * straight lines over central Noida.
     */
    private val interleavedTrace = listOf(
        fix(28.603899, 77.352806, 0, "2026-10-05 04:37:47"),
        fix(28.603899, 77.352813, 0, "2026-10-05 04:37:48"),
        fix(28.603899, 77.352813, 0, "2026-10-05 04:38:03"),
        fix(28.603899, 77.352806, 0, "2026-10-05 04:38:04"),
        fix(28.603899, 77.352813, 0, "2026-10-05 04:38:39"),
        fix(28.603780, 77.352645, 0, "2026-10-05 04:38:40"),
        fix(28.603899, 77.352813, 0, "2026-10-05 04:39:46"),
        fix(28.601527, 77.353948, 30, "2026-10-05 04:39:47"),
        BASE,
        fix(28.599920, 77.350526, 32, "2026-10-05 04:40:53"),
        BASE,
        fix(28.600089, 77.338130, 17, "2026-10-05 04:42:01"),
        BASE,
        fix(28.597122, 77.333984, 16, "2026-10-05 04:43:07"),
        BASE,
        fix(28.597019, 77.330019, 36, "2026-10-05 04:45:31"),
        BASE,
        fix(28.600497, 77.326971, 10, "2026-10-05 04:46:37"),
        BASE,
        fix(28.604115, 77.323741, 43, "2026-10-05 04:47:50"),
        BASE,
        fix(28.608214, 77.320230, 21, "2026-10-05 04:50:04"),
        BASE,
        fix(28.610869, 77.321674, 21, "2026-10-05 04:51:11"),
        BASE,
        fix(28.612762, 77.322893, 8, "2026-10-05 04:52:18"),
    )

    @Test
    fun theRepeatedDepotFixIsDroppedAndTheDriveIsKept() {
        val cleaned = dropStaleBeaconFixes(interleavedTrace)

        // Every interleaved reappearance of the depot goes.
        assertEquals(9, interleavedTrace.size - cleaned.size)

        // None of the moving fixes do.
        val moving = interleavedTrace.filter { it.getSpeedInt() > 0 }
        assertTrue(moving.all { it in cleaned }, "a moving fix was dropped")
    }

    @Test
    fun theFirstStayAtTheDepotSurvives() {
        val cleaned = dropStaleBeaconFixes(interleavedTrace)

        // The vehicle really was parked there until 04:39:46 — that run is the trip's
        // start, not the beacon, so it stays on the map.
        assertEquals(interleavedTrace.take(7), cleaned.take(7))
    }

    @Test
    fun theCleanedTraceNoLongerDoublesBackOnItself() {
        fun length(points: List<RoutePoint>): Double =
            points.zipWithNext().sumOf { (a, b) ->
                val dLat = (b.getLatDouble()!! - a.getLatDouble()!!) * 111_320.0
                val dLng = (b.getLngDouble()!! - a.getLngDouble()!!) * 111_320.0 * 0.878
                kotlin.math.sqrt(dLat * dLat + dLng * dLng)
            }

        // The star of out-and-back hops measured many times the real drive.
        assertTrue(
            length(dropStaleBeaconFixes(interleavedTrace)) < length(interleavedTrace) / 5,
            "the beacon's out-and-back hops are still in the path",
        )
    }

    @Test
    fun aVehicleParkedAllMorningIsNotMistakenForABeacon() {
        // Same coordinate many times over, but in one unbroken stay.
        val parked = List(20) { BASE } + listOf(
            fix(28.601527, 77.353948, 30),
            fix(28.599920, 77.350526, 32),
            fix(28.600089, 77.338130, 17),
        )

        assertEquals(parked, dropStaleBeaconFixes(parked))
    }

    @Test
    fun aRoundTripBackToTheDepotIsNotABeacon() {
        // Out, back, and parked again — one return, which is an ordinary day's work.
        val roundTrip = listOf(
            BASE, BASE,
            fix(28.599920, 77.350526, 32),
            fix(28.597122, 77.333984, 16),
            fix(28.604115, 77.323741, 43),
            fix(28.597122, 77.333984, 16),
            fix(28.599920, 77.350526, 32),
            BASE, BASE,
        )

        assertEquals(roundTrip, dropStaleBeaconFixes(roundTrip))
    }

    @Test
    fun driftAroundOneSpotIsNotABeacon() {
        // Returns to the same place repeatedly, but never drives away from it.
        val idling = listOf(
            BASE,
            fix(28.603910, 77.352900, 2),
            BASE,
            fix(28.603880, 77.352700, 3),
            BASE,
            fix(28.603905, 77.352850, 1),
            BASE,
            fix(28.603890, 77.352780, 2),
            BASE,
        )

        assertEquals(idling, dropStaleBeaconFixes(idling))
    }

    @Test
    fun aShortTraceIsLeftAlone() {
        val short = listOf(BASE, fix(28.599920, 77.350526, 32), BASE)

        assertEquals(short, dropStaleBeaconFixes(short))
    }
}