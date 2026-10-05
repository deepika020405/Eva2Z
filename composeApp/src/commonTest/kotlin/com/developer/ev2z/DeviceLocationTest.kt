package com.developer.ev2z

import com.developer.ev2z.data.local.model.Device
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The live map used to treat anything that parsed as a number as a position, so a
 * tracker with no GPS fix — which reports "0"/"0" — was drawn as a vehicle in the Gulf
 * of Guinea, and dragged the map's auto-fit bounds out into the Atlantic with it. What
 * has to hold is that only a real position counts as one.
 */
class DeviceLocationTest {

    private fun device(latitude: String?, longitude: String?, status: String = "moving") =
        Device(
            deviceId = "D1",
            deviceName = "Test",
            latitude = latitude,
            longitude = longitude,
            speed = 0,
            status = status,
            timestamp = null,
            lastSeenMinutesAgo = null,
        )

    @Test
    fun aRealFixIsReadAsItIs() {
        val delhi = device("28.6139", "77.2090")

        assertTrue(delhi.hasValidLocation())
        assertEquals(28.6139, delhi.getLatitudeDouble())
        assertEquals(77.2090, delhi.getLongitudeDouble())
    }

    @Test
    fun aTrackerWithNoFixIsNotAVehicleInTheAtlantic() {
        listOf(
            device("0", "0"),
            device("0.0", "0.0"),
            device("0.0000", "-0.0000"),
        ).forEach { noFix ->
            assertFalse(noFix.hasValidLocation(), "${noFix.latitude},${noFix.longitude}")
            assertNull(noFix.getLatitudeDouble())
            assertNull(noFix.getLongitudeDouble())
        }
    }

    @Test
    fun aGenuineFixNearTheEquatorOrPrimeMeridianStillCounts() {
        // Only both axes at zero together is the sentinel. One of them being near zero
        // is a real place, and rejecting it would blank out a vehicle that is reporting
        // perfectly well.
        assertTrue(device("0.0", "77.2090").hasValidLocation())
        assertTrue(device("28.6139", "0.0").hasValidLocation())
    }

    @Test
    fun coordinatesOffTheGlobeAreNotAPosition() {
        assertFalse(device("91.0", "77.2").hasValidLocation())
        assertFalse(device("28.6", "181.0").hasValidLocation())
        assertFalse(device("-90.1", "77.2").hasValidLocation())
    }

    @Test
    fun garbageThatParsesAsADoubleIsStillGarbage() {
        // "NaN" and "Infinity" both come back from toDoubleOrNull as real Doubles.
        assertFalse(device("NaN", "77.2").hasValidLocation())
        assertFalse(device("28.6", "Infinity").hasValidLocation())
        assertFalse(device("28.6", "-Infinity").hasValidLocation())
    }

    @Test
    fun aMissingOrUnreadableCoordinateIsNotAPosition() {
        assertFalse(device(null, "77.2").hasValidLocation())
        assertFalse(device("28.6", null).hasValidLocation())
        assertFalse(device("", "").hasValidLocation())
        assertFalse(device("north", "east").hasValidLocation())
    }

    @Test
    fun oneAxisIsNeverReportedWithoutTheOther() {
        // Half a position would put a marker on the equator or the prime meridian.
        val halfFix = device("28.6139", null)

        assertNull(halfFix.getLatitudeDouble())
        assertNull(halfFix.getLongitudeDouble())
    }

    private fun status(raw: String) = device("28.6", "77.2", status = raw)

    @Test
    fun theStatusLabelsMatchTheOverviewSheetsBuckets() {
        // The sheet counts Running / Idle / Parked / Offline, so every one of those has
        // to have a label — "parked" used to fall through to the raw API value.
        assertEquals("Running", status("moving").getStatusDisplay())
        assertEquals("Idle", status("idle").getStatusDisplay())
        assertEquals("Parked", status("parked").getStatusDisplay())
        assertEquals("Offline", status("offline").getStatusDisplay())
        assertEquals("No Data", status("no_data").getStatusDisplay())
    }

    @Test
    fun aParkedVehicleReadsAsParkedWhateverTheCasing() {
        // The old `when (status)` compared against lower-case literals, so anything the
        // server capitalised fell through to the unknown bucket: grey dot, azure pin.
        listOf("parked", "Parked", "PARKED", " parked ").forEach { raw ->
            assertEquals("Parked", status(raw).getStatusDisplay(), raw)
            assertEquals("parked", status(raw).getStatusKind(), raw)
            assertEquals(0xFFF44336, status(raw).getStatusColor(), raw)
        }
    }

    @Test
    fun aRunningVehicleReadsAsRunningWhateverTheServerCallsIt() {
        // TrackTrace, DeviceReport and HistoryPlayback already accept all three
        // spellings; the live map used to recognise only "moving".
        listOf("moving", "running", "on", "Moving", "ON").forEach { raw ->
            assertEquals("Running", status(raw).getStatusDisplay(), raw)
            assertEquals(0xFF4CAF50, status(raw).getStatusColor(), raw)
        }
    }

    @Test
    fun theIdleMisspellingTheRestOfTheAppAcceptsIsAcceptedHere() {
        // HistoryPlaybackScreen maps "ideal" alongside "idle", so the server sends it.
        assertEquals("Idle", status("ideal").getStatusDisplay())
        assertEquals("Idle", status("idle").getStatusDisplay())
    }

    @Test
    fun ignitionOffIsAStopNotAPark() {
        // Only the server decides a vehicle is parked. "off" reads as Stopped, matching
        // TrackTraceScreen, rather than silently promoting every stop to a park.
        assertEquals("Stopped", status("off").getStatusDisplay())
        assertEquals("Stopped", status("stopped").getStatusDisplay())
        assertEquals("stopped", status("off").getStatusKind())
    }

    @Test
    fun aStatusNobodyKnowsIsShownRatherThanHidden() {
        // Better a readable unknown label than an empty cell, so a new server value
        // shows up instead of vanishing.
        assertEquals("Towing", status("towing").getStatusDisplay())
        assertEquals("unknown", status("towing").getStatusKind())
    }
}