package com.developer.ev2z

import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.util.bearingBetween
import com.developer.ev2z.util.stepBearing
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Which way the playback marker faces. The case that sent us here is a roundabout: the
 * vehicle goes all the way round one, and the icon has to come out of it pointing the
 * way it is now going.
 */
class PlaybackBearingTest {

    @Test
    fun `due north and due east are read correctly`() {
        val origin = GeoPoint(28.6000, 77.3500)

        assertTrue(abs(bearingBetween(origin, GeoPoint(28.6010, 77.3500)) - 0.0) < 1.0)
        assertTrue(abs(bearingBetween(origin, GeoPoint(28.6000, 77.3510)) - 90.0) < 1.0)
        assertTrue(abs(bearingBetween(origin, GeoPoint(28.5990, 77.3500)) - 180.0) < 1.0)
        assertTrue(abs(bearingBetween(origin, GeoPoint(28.6000, 77.3490)) - 270.0) < 1.0)
    }

    /** Noise between two fixes at the same place is not a direction. */
    @Test
    fun `a standstill has no bearing`() {
        val parked = GeoPoint(28.603829, 77.352759)

        assertTrue(bearingBetween(parked, parked).isNaN())
        assertTrue(
            bearingBetween(parked, GeoPoint(28.6038295, 77.3527595)).isNaN(),
            "sub-metre wander is jitter; letting it through is what spins a parked marker"
        )
        assertTrue(
            // ~2 m north. Consumer GPS wanders this much standing still, and a snapped
            // road's own vertices sit this close together, so neither is a direction.
            bearingBetween(parked, GeoPoint(28.603847, 77.352759)).isNaN(),
            "a two-metre hop is noise, not a heading"
        )
    }

    /**
     * The whole reason easing exists: 350° → 10° is a twenty-degree turn to the right,
     * but the plain difference is -340°, which would spin the vehicle almost all the
     * way round the wrong way.
     */
    @Test
    fun `a turn through north goes the short way`() {
        val stepped = stepBearing(350.0, 10.0)

        assertTrue(
            stepped > 350.0 || stepped < 10.0,
            "expected a few degrees clockwise past north, got $stepped"
        )
    }

    /** A roundabout is a long arc of small steps, and it has to come out facing right. */
    @Test
    fun `easing converges on the target`() {
        var bearing = 0.0
        // One exit of a roundabout: a bit over a quarter turn.
        repeat(60) { bearing = stepBearing(bearing, 100.0) }

        assertTrue(abs(bearing - 100.0) < 1.0, "settled on $bearing instead of 100")
    }

    @Test
    fun `an unknown direction leaves the heading alone and an unknown heading takes it whole`() {
        assertEquals(137.0, stepBearing(137.0, Double.NaN), "nothing to turn towards")
        assertEquals(137.0, stepBearing(Double.NaN, 137.0), "the first heading is drawn, not eased into")
    }
}
