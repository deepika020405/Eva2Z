package com.developer.ev2z

import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.ui.screens.HistoryPlayback.DEFAULT_STEP_MS
import com.developer.ev2z.ui.screens.HistoryPlayback.playbackStepMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun fix(at: String, speed: Int = 20) = RoutePoint(
    latitude = "28.6",
    longitude = "77.35",
    speed = speed.toString(),
    createdAt = at,
    movement = if (speed == 0) "parked" else "moving",
)

class PlaybackPacingTest {

    /**
     * The whole point: a leg the vehicle took longer over is drawn over longer, so the
     * marker's speed on screen tracks the speed on the road instead of tracking how far
     * apart two fixes happened to land.
     */
    @Test
    fun `a longer leg takes longer to draw`() {
        val oneMinute = listOf(fix("2026-10-12 06:00:00"), fix("2026-10-12 06:01:00"))
        val fiveMinutes = listOf(fix("2026-10-12 06:00:00"), fix("2026-10-12 06:05:00"))

        assertTrue(
            playbackStepMillis(fiveMinutes, 1) > playbackStepMillis(oneMinute, 1),
            "five minutes of driving must not be drawn in the same time as one"
        )
    }

    /** A stop long enough to survive collapsing must not look like a hang. */
    @Test
    fun `a long gap is capped`() {
        val overnight = listOf(fix("2026-10-12 00:05:00", 0), fix("2026-10-12 08:30:00", 0))

        assertTrue(
            playbackStepMillis(overnight, 1) <= 1_200L,
            "an eight-hour leg would freeze playback without the ceiling"
        )
    }

    /** And a burst of fixes a second apart must not flash past unreadably. */
    @Test
    fun `a tiny gap has a floor`() {
        val burst = listOf(fix("2026-10-12 06:00:00"), fix("2026-10-12 06:00:01"))

        assertTrue(playbackStepMillis(burst, 1) >= 90L)
    }

    /**
     * This decides how fast to draw, so it has to answer with something sane for every
     * input the server can produce — including the backlog stream, which arrives
     * stamped out of order.
     */
    @Test
    fun `unanswerable legs fall back to the default`() {
        val points = listOf(fix("2026-10-12 06:00:00"), fix("2026-10-12 06:01:00"))

        assertEquals(DEFAULT_STEP_MS, playbackStepMillis(points, 0), "no leg before the first fix")
        assertEquals(DEFAULT_STEP_MS, playbackStepMillis(points, 9), "index past the end")
        assertEquals(DEFAULT_STEP_MS, playbackStepMillis(emptyList(), 1), "no points at all")

        val unparseable = listOf(fix("not a timestamp"), fix("2026-10-12 06:01:00"))
        assertEquals(DEFAULT_STEP_MS, playbackStepMillis(unparseable, 1))

        val backwards = listOf(fix("2026-10-12 06:05:00"), fix("2026-10-12 06:00:00"))
        assertEquals(DEFAULT_STEP_MS, playbackStepMillis(backwards, 1))
    }
}
