package com.developer.ev2z

import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.ui.components.MAX_GLIDE_MS
import com.developer.ev2z.ui.components.MIN_GLIDE_MS
import com.developer.ev2z.ui.components.glideDurationMs
import com.developer.ev2z.ui.components.isFarJump
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What makes a marker read as a vehicle driving rather than as a dot being teleported.
 *
 * The rule both live maps now share: a glide lasts slightly longer than the gap
 * between the last two *fixes*, so the next fix always lands while the marker is still
 * moving. Pacing to the poll interval instead is what made the Home map stutter — the
 * app polls every few seconds, a tracker uploads far less often, so the marker
 * finished its glide and then sat still until the vehicle reported again.
 */
class MarkerGlideTest {

    @Test
    fun aGlideOutlastsTheGapItWasMeasuredFrom() {
        // Ten seconds between fixes must buy more than ten seconds of travel,
        // otherwise the marker stops dead before the next one arrives.
        assertTrue(glideDurationMs(10_000) > 10_000)
    }

    @Test
    fun aBurstOfFixesCannotDriveTheGlideToZero() {
        // Two fixes 50 ms apart would otherwise ask for a 56 ms animation, which is
        // three frames — a teleport with extra steps.
        assertTrue(glideDurationMs(50) >= MIN_GLIDE_MS)
    }

    @Test
    fun aTrackerThatWentQuietDoesNotCrawlForTheWholeSilence() {
        // Five minutes off the air must not become a five-minute glide.
        assertTrue(glideDurationMs(300_000) <= (MAX_GLIDE_MS * 1.2).toInt())
    }

    @Test
    fun ordinaryTravelIsDrivenAndSignalRecoveryIsSnapped() {
        // ~100 m of city driving between fixes.
        assertFalse(isFarJump(28.60788, 77.425053, 28.60878, 77.425053))

        // The same tracker reappearing 20 km away — it did not drive there at 60 fps.
        assertTrue(isFarJump(28.60788, 77.425053, 28.80788, 77.425053))
    }

    @Test
    fun aStoppedVehicleIsRedAndARunningOneIsGreen() {
        val green = 0xFF4CAF50
        val red = 0xFFF44336

        // Every spelling the four endpoints use for "it is moving".
        assertEquals(green, statusColorOf("running"))
        assertEquals(green, statusColorOf("moving"))
        assertEquals(green, statusColorOf("on"))
        assertEquals(green, statusColorOf("Running"))

        // And for "it is not".
        assertEquals(red, statusColorOf("stopped"))
        assertEquals(red, statusColorOf("stop"))
        assertEquals(red, statusColorOf("off"))
        assertEquals(red, statusColorOf("parked"))

        // Offline is deliberately neither: the tracker stopped reporting, which is not
        // the same claim as the vehicle having stopped.
        assertEquals(0xFFBDBDBD, statusColorOf("offline"))
    }
}
