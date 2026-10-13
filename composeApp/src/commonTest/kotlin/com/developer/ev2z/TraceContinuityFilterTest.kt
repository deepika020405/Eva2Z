package com.developer.ev2z

import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.ui.screens.HistoryPlayback.keepTravelableFixes
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun fix(lat: Double, lng: Double, at: String, speed: Int = 40) =
    RoutePoint(
        latitude = lat.toString(),
        longitude = lng.toString(),
        speed = speed.toString(),
        createdAt = "2026-10-09 $at",
        movement = if (speed == 0) "parked" else "moving",
    )

private fun lengthMeters(points: List<RoutePoint>): Double =
    points.zipWithNext().sumOf { (a, b) ->
        val dLat = (b.getLatDouble()!! - a.getLatDouble()!!) * 111_320.0
        val dLng = (b.getLngDouble()!! - a.getLngDouble()!!) * 111_320.0 * 0.878
        sqrt(dLat * dLat + dLng * dLng)
    }

class TraceContinuityFilterTest {

    /**
     * The trace that sent us here: 2026-10-09, device 0869727076942661, northbound on
     * NH-19 with a second stream 16 km south woven through it every couple of minutes.
     */
    private val northbound = listOf(
        fix(28.407590, 77.333019, "08:04:02"),
        fix(28.424274, 77.328343, "08:06:32"),
        fix(28.437458, 77.324331, "08:09:25"),
        fix(28.464958, 77.316923, "08:12:58"),
        fix(28.478983, 77.315872, "08:15:30"),
        fix(28.493316, 77.316366, "08:17:21"),
        fix(28.520067, 77.310078, "08:21:17"),
        fix(28.524708, 77.306727, "08:23:29"),
        fix(28.537145, 77.302542, "08:24:10"),
        fix(28.562886, 77.291378, "08:29:02"),
    )

    private val southernStream = listOf(
        fix(28.340428, 77.352624, "08:02:54", speed = 6),
        fix(28.347475, 77.347628, "08:08:17", speed = 16),
        fix(28.348889, 77.342281, "08:13:30", speed = 17),
        fix(28.348014, 77.346900, "08:19:03", speed = 25),
        fix(28.348852, 77.341317, "08:25:00", speed = 19),
    )

    private val interleaved =
        (northbound + southernStream).sortedBy { it.createdAt }

    @Test
    fun theWovenInStreamIsDroppedAndTheJourneyIsKept() {
        val cleaned = keepTravelableFixes(interleaved)

        assertEquals(northbound, cleaned)
    }

    @Test
    fun theCleanedTraceNoLongerCrossesTheCityAndBack() {
        // Every intruder was a 16 km hop out and another one back.
        assertTrue(
            lengthMeters(keepTravelableFixes(interleaved)) < lengthMeters(interleaved) / 5,
            "the out-and-back hops are still in the path",
        )
    }

    @Test
    fun sparseHighwayReportingIsNotMistakenForAnotherStream() {
        // 2–3 km between fixes because the tracker reports every couple of minutes —
        // perfectly possible to drive, and the stretch that needs road geometry most.
        val sparse = listOf(
            fix(28.407590, 77.333019, "08:04:02"),
            fix(28.424274, 77.328343, "08:06:32"),
            fix(28.449810, 77.318932, "08:10:33"),
            fix(28.473120, 77.316293, "08:14:09"),
            fix(28.493316, 77.316366, "08:17:21"),
            fix(28.520067, 77.310078, "08:21:17"),
            fix(28.537145, 77.302542, "08:24:10"),
            fix(28.562886, 77.291378, "08:29:02"),
        )

        assertEquals(sparse, keepTravelableFixes(sparse))
    }

    @Test
    fun aVehicleParkedAllMorningAndThenDrivenIsLeftAlone() {
        val base = (0..11).map { minute -> fix(28.603840, 77.352805, at(minute), speed = 0) }
        val drive = listOf(
            fix(28.604061, 77.352437, "00:14:15", speed = 25),
            fix(28.606897, 77.357278, "00:16:38", speed = 6),
            fix(28.607912, 77.354820, "00:19:39", speed = 23),
            fix(28.601032, 77.353998, "00:22:18", speed = 7),
        )

        assertEquals(base + drive, keepTravelableFixes(base + drive))
    }

    @Test
    fun aTraceThatResumesAfterAnOutageKeepsBothHalves() {
        // The tracker went quiet for ten minutes and came back 65 km away: one hop
        // nothing could have driven, with a real stretch of route on either side of it.
        val before = (0..39).map { step ->
            fix(28.60 + step * 0.0004, 77.3528, at(6 * 60 + step), speed = 30)
        }
        val after = (0..39).map { step ->
            fix(28.00 + step * 0.0004, 77.3470, at(6 * 60 + 50 + step), speed = 30)
        }

        assertEquals(before + after, keepTravelableFixes(before + after))
    }

    @Test
    fun aShortTraceIsLeftAlone() {
        val short = listOf(
            fix(28.603840, 77.352805, "08:00:00"),
            fix(28.340428, 77.352624, "08:00:30"),
            fix(28.603840, 77.352805, "08:01:00"),
        )

        assertEquals(short, keepTravelableFixes(short))
    }

    @Test
    fun aTraceWithUnreadableTimestampsIsLeftAlone() {
        // Nothing can be judged without an interval, and dropping what cannot be
        // measured would take the route off the map.
        val undated = interleaved.map { it.copy(createdAt = "") }

        assertEquals(undated, keepTravelableFixes(undated))
    }
}

private fun Int.pad(): String = toString().padStart(2, '0')

/** A minute of the day as `HH:MM:10`, so a fixture can run past the hour. */
private fun at(minuteOfDay: Int): String = "${(minuteOfDay / 60).pad()}:${(minuteOfDay % 60).pad()}:10"
