package com.developer.ev2z

import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.ui.screens.device_Report.MAX_ATTRIBUTABLE_GAP_SECONDS
import com.developer.ev2z.ui.screens.device_Report.computeActivityBreakdown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A fix carrying only what the breakdown reads: a time and a movement state. */
private fun fix(at: String, movement: String) =
    RoutePoint(
        latitude = "28.6",
        longitude = "77.4",
        speed = if (movement == "moving") "30" else "0",
        createdAt = at,
        movement = movement,
    )

class DeviceActivityBreakdownTest {

    @Test
    fun `credits each interval to the state reported at its start`() {
        // Running 06:00–06:10, parked 06:10–06:30, reported every five minutes — inside
        // the cadence a tracker actually keeps, so nothing here is an outage.
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:05:00", "moving"),
                fix("2026-10-14 06:10:00", "parked"),
                fix("2026-10-14 06:15:00", "parked"),
                fix("2026-10-14 06:20:00", "parked"),
                fix("2026-10-14 06:25:00", "parked"),
                fix("2026-10-14 06:30:00", "parked"),
            )
        )!!

        assertEquals(10, breakdown.running.minutes)
        assertEquals(20, breakdown.parked.minutes)
        assertEquals(0, breakdown.stopped.minutes)
        assertEquals(30, breakdown.knownMinutes)
    }

    /**
     * The whole reason this is not just a sum of intervals. A tracker that reports
     * "moving" and then goes dark until the evening did not drive all afternoon.
     */
    @Test
    fun `does not credit a state across a reporting outage`() {
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:05:00", "moving"),
                // Eight hours of silence.
                fix("2026-10-14 14:05:00", "parked"),
                fix("2026-10-14 14:15:00", "parked"),
            )
        )!!

        assertEquals(5, breakdown.running.minutes, "only the reported running counts")
        assertEquals(10, breakdown.parked.minutes)
        assertEquals(8 * 60, breakdown.unknownMinutes, "the outage is unknown, not parked")
        assertEquals(15, breakdown.knownMinutes)
    }

    @Test
    fun `a gap just inside the limit is still credited`() {
        val justInside = MAX_ATTRIBUTABLE_GAP_SECONDS - 60L
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:14:00", "parked"),
            )
        )!!

        assertEquals((justInside / 60L).toInt(), breakdown.running.minutes)
        assertEquals(0, breakdown.unknownMinutes)
    }

    /**
     * `idle` is ignition-on-not-moving. The screen has three buckets, and idling is time
     * the vehicle was not going anywhere — so it reads as stopped rather than disappearing.
     */
    @Test
    fun `idle counts as stopped`() {
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "ideal"),   // the server's spelling
                fix("2026-10-14 06:10:00", "stopped"),
                fix("2026-10-14 06:20:00", "stopped"),
            )
        )!!

        assertEquals(20, breakdown.stopped.minutes)
        assertEquals(0, breakdown.running.minutes)
        assertEquals(0, breakdown.parked.minutes)
    }

    /**
     * `offline` and `no_data` describe the tracker, not the vehicle — crediting them to a
     * motion state would invent one. See `statusKindOf`.
     */
    @Test
    fun `connectivity states are unknown time, not a motion state`() {
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "offline"),
                fix("2026-10-14 06:10:00", "no_data"),
                fix("2026-10-14 06:20:00", "moving"),
                fix("2026-10-14 06:25:00", "moving"),
            )
        )!!

        assertEquals(5, breakdown.running.minutes)
        assertEquals(20, breakdown.unknownMinutes)
        assertEquals(5, breakdown.knownMinutes)
        assertEquals(100, breakdown.running.percent, "percent is a share of known time")
    }

    /** Out-of-order arrivals are routine on this endpoint; a negative interval must not subtract. */
    @Test
    fun `handles fixes that arrive out of time order`() {
        val inOrder = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:05:00", "moving"),
                fix("2026-10-14 06:10:00", "parked"),
                fix("2026-10-14 06:15:00", "parked"),
            )
        )!!

        val shuffled = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:15:00", "parked"),
                fix("2026-10-14 06:05:00", "moving"),
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:10:00", "parked"),
            )
        )!!

        assertEquals(10, inOrder.running.minutes, "sanity: the ordered case is non-zero")
        assertEquals(5, inOrder.parked.minutes)
        assertEquals(inOrder.running.minutes, shuffled.running.minutes)
        assertEquals(inOrder.parked.minutes, shuffled.parked.minutes)
    }

    /** A batch upload stamps every fix in it with the same arrival second. */
    @Test
    fun `duplicate timestamps contribute nothing instead of breaking the total`() {
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:00:00", "parked"),
                fix("2026-10-14 06:10:00", "parked"),
            )
        )!!

        assertEquals(10, breakdown.knownMinutes)
        assertEquals(10, breakdown.parked.minutes)
    }

    /** A donut with arcs totalling 99% leaves a notch in the ring. */
    @Test
    fun `percentages always add up to a hundred`() {
        // Three equal thirds — the case that rounds to 99 if each share is rounded alone.
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:10:00", "stopped"),
                fix("2026-10-14 06:20:00", "parked"),
                fix("2026-10-14 06:30:00", "parked"),
            )
        )!!

        val total = breakdown.running.percent + breakdown.stopped.percent + breakdown.parked.percent
        assertEquals(100, total)
    }

    @Test
    fun `reports hours to one decimal`() {
        val breakdown = computeActivityBreakdown(
            listOf(
                fix("2026-10-14 06:00:00", "moving"),
                fix("2026-10-14 06:09:00", "parked"),
            )
        )!!

        // 9 minutes = 0.15 h, which shows as 0.2 at one decimal.
        assertEquals(0.2, breakdown.running.hours)
    }

    @Test
    fun `returns null when the fixes cannot answer`() {
        assertNull(computeActivityBreakdown(emptyList()), "no fixes")
        assertNull(
            computeActivityBreakdown(listOf(fix("2026-10-14 06:00:00", "moving"))),
            "a single fix spans no time",
        )
        assertNull(
            computeActivityBreakdown(
                listOf(
                    fix("not a date", "moving"),
                    fix("also not a date", "parked"),
                )
            ),
            "unreadable timestamps",
        )
        assertNull(
            computeActivityBreakdown(
                listOf(
                    fix("2026-10-14 06:00:00", "offline"),
                    fix("2026-10-14 06:10:00", "offline"),
                )
            ),
            "nothing but connectivity states leaves no known time to divide",
        )
    }

    /** A vehicle parked all day is the common case, and must not read as zero. */
    @Test
    fun `a day spent entirely parked is all parked`() {
        val fixes = (0 until 60).map { minute ->
            fix("2026-10-14 06:${minute.toString().padStart(2, '0')}:00", "parked")
        }

        val breakdown = computeActivityBreakdown(fixes)!!

        assertEquals(59, breakdown.parked.minutes)
        assertEquals(100, breakdown.parked.percent)
        assertEquals(0, breakdown.running.minutes)
        assertTrue(breakdown.knownHours > 0.0)
    }
}
