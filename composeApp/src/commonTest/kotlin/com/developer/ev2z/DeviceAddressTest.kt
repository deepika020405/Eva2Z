package com.developer.ev2z

import com.developer.ev2z.ui.screens.homeScreen.deviceAddressKey
import com.developer.ev2z.ui.screens.homeScreen.isUsableAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The live map now shows a parked vehicle's street address. Reverse geocoding is slow
 * and rate-limited — iOS throttles it outright — so what has to hold is that a vehicle
 * sitting still keeps hitting one cache entry however much its fix drifts, and that a
 * geocoder failure is never mistaken for an address and cached forever.
 */
class DeviceAddressTest {

    @Test
    fun aParkedVehiclesDriftStaysInOneCacheEntry() {
        val parked = deviceAddressKey(28.613900, 77.209000)

        // A standing tracker's fix wanders a few metres between reports. Each of these
        // must reuse the resolved address rather than asking the geocoder again.
        assertEquals(parked, deviceAddressKey(28.613912, 77.209031))
        assertEquals(parked, deviceAddressKey(28.613949, 77.208961))
    }

    @Test
    fun aVehicleThatActuallyMovedGetsAFreshLookup() {
        // ~500 m away is a different address and must not reuse the old one.
        assertNotEquals(
            deviceAddressKey(28.6139, 77.2090),
            deviceAddressKey(28.6184, 77.2090),
        )
    }

    @Test
    fun theKeyIsStableForTheSameCoordinates() {
        assertEquals(deviceAddressKey(28.6139, 77.2090), deviceAddressKey(28.6139, 77.2090))
    }

    @Test
    fun negativeCoordinatesRoundTheSameWay() {
        val key = deviceAddressKey(-33.8688, -151.2093)

        assertEquals(key, deviceAddressKey(-33.86881, -151.20932))
    }

    @Test
    fun aGeocoderFailureIsNotAnAddress() {
        // Both providers report failure by returning a sentence instead of throwing.
        // Caching one would leave "Location unavailable" on the row permanently.
        assertFalse("Unknown location".isUsableAddress())
        assertFalse("Location unavailable".isUsableAddress())
        assertFalse("unknown".isUsableAddress())
        assertFalse("".isUsableAddress())
        assertFalse("   ".isUsableAddress())
        assertFalse("null".isUsableAddress())
    }

    @Test
    fun arealAddressIsKept() {
        assertTrue("Sector 57, Noida, Uttar Pradesh 201301, India".isUsableAddress())
        assertTrue("Kanchanganga Apartment, Sector 54, Noida".isUsableAddress())
    }
}