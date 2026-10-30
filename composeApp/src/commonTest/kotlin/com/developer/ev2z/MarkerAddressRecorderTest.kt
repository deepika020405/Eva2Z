package com.developer.ev2z

import com.developer.ev2z.util.MARKER_ADDRESS_HISTORY_LIMIT
import com.developer.ev2z.util.MarkerAddressFix
import com.developer.ev2z.util.MarkerAddressRecorder
import com.developer.ev2z.util.MarkerAddressSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Marker addresses are recorded with both pairs of coordinates: the API fix the pin is
 * drawn at, and the position the geocoder's own response gives for the address it
 * answered with. What has to hold is that the two pairs stay apart, that a marker
 * recomposing on every animation frame does not fill the trail with one position, and
 * that a long drive cannot grow the trail without bound.
 */
class MarkerAddressRecorderTest {

    @BeforeTest
    fun reset() = MarkerAddressRecorder.clear()

    @AfterTest
    fun tidy() = MarkerAddressRecorder.clear()

    private fun fix(
        source: String = MarkerAddressSource.FLEET_MAP,
        ownerKey: String = "0869727076942661",
        address: String = "Sector 57, Noida, Uttar Pradesh",
        markerLatitude: Double = 28.6139,
        markerLongitude: Double = 77.2090,
        addressLatitude: Double? = 28.6141,
        addressLongitude: Double? = 77.2093,
    ) = MarkerAddressFix(
        source = source,
        ownerKey = ownerKey,
        address = address,
        markerLatitude = markerLatitude,
        markerLongitude = markerLongitude,
        addressLatitude = addressLatitude,
        addressLongitude = addressLongitude,
    )

    @Test
    fun bothPairsOfCoordinatesAreKept() {
        MarkerAddressRecorder.record(fix())

        val recorded = MarkerAddressRecorder.latestFor(
            MarkerAddressSource.FLEET_MAP,
            "0869727076942661",
        )

        assertNotNull(recorded)
        // The fix the marker sits on — what latest-locations reported.
        assertEquals(28.6139, recorded.markerLatitude)
        assertEquals(77.2090, recorded.markerLongitude)
        // Where the geocoder says that address is, which is a different point.
        assertEquals(28.6141, recorded.addressLatitude)
        assertEquals(77.2093, recorded.addressLongitude)
    }

    @Test
    fun theSameFixRecordedTwiceIsOneEntry() {
        // A marker recomposes on every animation frame, and each recomposition records.
        MarkerAddressRecorder.record(fix())
        MarkerAddressRecorder.record(fix())
        MarkerAddressRecorder.record(fix())

        assertEquals(1, MarkerAddressRecorder.history().size)
    }

    @Test
    fun aVehicleThatMovedIsANewEntry() {
        MarkerAddressRecorder.record(fix())
        MarkerAddressRecorder.record(fix(markerLatitude = 28.6184))

        assertEquals(2, MarkerAddressRecorder.history().size)
        // The newest wins for that marker.
        assertEquals(
            28.6184,
            MarkerAddressRecorder.latestFor(
                MarkerAddressSource.FLEET_MAP,
                "0869727076942661",
            )?.markerLatitude,
        )
    }

    @Test
    fun twoMarkersForOneVehicleDoNotOverwriteEachOther() {
        // The fleet map pin and the overview row show the same vehicle's address.
        MarkerAddressRecorder.record(fix(source = MarkerAddressSource.FLEET_MAP))
        MarkerAddressRecorder.record(
            fix(source = MarkerAddressSource.FLEET_SHEET_ROW, address = "Sector 62, Noida")
        )

        assertEquals(
            "Sector 57, Noida, Uttar Pradesh",
            MarkerAddressRecorder.latestFor(
                MarkerAddressSource.FLEET_MAP,
                "0869727076942661",
            )?.address,
        )
        assertEquals(
            "Sector 62, Noida",
            MarkerAddressRecorder.latestFor(
                MarkerAddressSource.FLEET_SHEET_ROW,
                "0869727076942661",
            )?.address,
        )
    }

    @Test
    fun twoVehiclesNeverInheritEachOthersRecord() {
        MarkerAddressRecorder.record(fix(ownerKey = "deviceA"))
        MarkerAddressRecorder.record(fix(ownerKey = "deviceB", address = "Sector 62, Noida"))

        assertEquals(2, MarkerAddressRecorder.all().size)
        assertNull(MarkerAddressRecorder.latestFor(MarkerAddressSource.FLEET_MAP, "deviceC"))
    }

    @Test
    fun aLongDriveDoesNotGrowTheTrailWithoutBound() {
        repeat(MARKER_ADDRESS_HISTORY_LIMIT + 50) { step ->
            MarkerAddressRecorder.record(fix(markerLatitude = 28.6139 + step * 0.001))
        }

        assertEquals(MARKER_ADDRESS_HISTORY_LIMIT, MarkerAddressRecorder.history().size)
        // Oldest out first, so the trail ends on the newest position.
        assertEquals(
            28.6139 + (MARKER_ADDRESS_HISTORY_LIMIT + 49) * 0.001,
            MarkerAddressRecorder.history().last().markerLatitude,
        )
    }

    @Test
    fun theOffsetIsHowFarTheNamedAddressSitsFromTheMarker() {
        // ~22 m north-east of the fix: the address under the vehicle.
        val near = fix(addressLatitude = 28.61408, addressLongitude = 77.20915).offsetMeters
        assertNotNull(near)
        assertTrue(near < 50.0, "expected a few tens of metres, got $near")

        // A geocoder reaching for the nearest place it can name, half a kilometre off.
        val far = fix(addressLatitude = 28.6184, addressLongitude = 77.2090).offsetMeters
        assertNotNull(far)
        assertTrue(far > 400.0, "expected hundreds of metres, got $far")
    }

    @Test
    fun aResponseWithNoCoordinatesHasNoOffset() {
        // Android's Address can carry no position, and so can an iOS CLPlacemark; the
        // address is still shown, and the offset is simply unknown.
        assertNull(fix(addressLatitude = null, addressLongitude = null).offsetMeters)
        assertNull(fix(addressLongitude = null).offsetMeters)
    }
}
