package com.developer.ev2z

import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.ui.screens.HistoryPlayback.collapseStationaryRuns
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun fix(lat: Double, lng: Double, speed: Int = 0, at: String = "2026-10-12 00:00:00") =
    RoutePoint(
        latitude = lat.toString(),
        longitude = lng.toString(),
        speed = speed.toString(),
        createdAt = at,
        movement = if (speed == 0) "parked" else "moving",
    )

class StationaryRunCollapseTest {

    /**
     * The trace that sent us here: 2026-10-12, device 0869727076942661. The bus was
     * parked overnight, so the day opens with ninety-odd fixes on one coordinate and
     * playback stood still for its first minute.
     */
    @Test
    fun `an overnight park becomes a single fix`() {
        val parked = List(90) { minute ->
            fix(28.603829, 77.352759, 0, "2026-10-12 00:${minute.toString().padStart(2, '0')}:50")
        }
        val drive = listOf(
            fix(28.601527, 77.353948, 30, "2026-10-12 06:30:00"),
            fix(28.599920, 77.350526, 32, "2026-10-12 06:31:00"),
            fix(28.600089, 77.338130, 17, "2026-10-12 06:32:00"),
        )

        val collapsed = collapseStationaryRuns(parked + drive)

        assertEquals(4, collapsed.size)
        // The first fix of the park survives, so the route still starts where the
        // vehicle actually spent the night.
        assertEquals("2026-10-12 00:00:50", collapsed.first().createdAt)
        assertEquals(drive.map { it.createdAt }, collapsed.drop(1).map { it.createdAt })
    }

    /** A real journey is left exactly as it is — nothing here repeats a place. */
    @Test
    fun `a moving trace is untouched`() {
        val trace = listOf(
            fix(28.603829, 77.352759, 20, "2026-10-12 06:00:00"),
            fix(28.601527, 77.353948, 30, "2026-10-12 06:01:00"),
            fix(28.599920, 77.350526, 32, "2026-10-12 06:02:00"),
            fix(28.600089, 77.338130, 17, "2026-10-12 06:03:00"),
        )

        assertEquals(trace, collapseStationaryRuns(trace))
    }

    /**
     * Sub-metre wander at a standstill is jitter, not travel, and must not keep the
     * marker busy — but it must not hide the creep either: measuring from the last
     * *kept* fix means ten two-metre steps still register as a move.
     */
    @Test
    fun `jitter collapses but a slow creep does not`() {
        val jitter = listOf(
            fix(28.603829, 77.352759),
            fix(28.6038295, 77.3527595),
            fix(28.603829, 77.3527585),
        )
        // Two, not one: the anchor plus the final fix, which is kept unconditionally so
        // playback ends where the vehicle did. Both are the same spot, so the marker
        // has nothing to travel either way.
        assertEquals(2, collapseStationaryRuns(jitter + jitter.last()).size)

        // ~2 m per step, north: the fourth fix is ~6 m out and has to survive.
        val creep = List(6) { step -> fix(28.603829 + step * 0.000018, 77.352759) }
        assertTrue(
            collapseStationaryRuns(creep).size >= 3,
            "a creep away from the anchor is travel, not jitter"
        )
    }

    /** Playback has to finish where the vehicle did, even if it parked there. */
    @Test
    fun `the last fix is always kept`() {
        val trace = listOf(
            fix(28.601527, 77.353948, 30, "2026-10-12 06:00:00"),
            fix(28.603829, 77.352759, 0, "2026-10-12 06:30:00"),
            fix(28.603829, 77.352759, 0, "2026-10-12 06:31:00"),
            fix(28.603829, 77.352759, 0, "2026-10-12 23:59:00"),
        )

        val collapsed = collapseStationaryRuns(trace)

        assertEquals("2026-10-12 23:59:00", collapsed.last().createdAt)
        assertEquals(3, collapsed.size)
    }
}
