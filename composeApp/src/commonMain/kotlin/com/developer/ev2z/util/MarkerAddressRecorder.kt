package com.developer.ev2z.util

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.approxDistanceMeters

/**
 * Keeps a record of every address shown on a map marker, together with the coordinates
 * behind it.
 *
 * A marker's address is the end of a two-step chain: the fleet/live API reports a fix,
 * and that fix is handed to the platform reverse geocoder, which answers with a place
 * that has coordinates of its own. When the address on a pin looks wrong, the only way
 * to say *which* step was wrong is to have both pairs — the fix the marker is drawn at,
 * and the position of the address the geocoder picked for it. One shows a bad tracker
 * fix, the other shows a geocoder reaching for the nearest thing it could name.
 *
 * Every marker site goes through [com.developer.ev2z.ui.screens.homeScreen.rememberMarkerAddress],
 * so recording happens there rather than at each call site — a new map gets this for
 * free by asking for its address the same way the others do.
 *
 * In-memory only: this is a diagnostic trail for the session that is running, not
 * something to persist or upload.
 */

/** Which marker the record came from. Plain strings so a new screen can add its own. */
object MarkerAddressSource {
    /** The fleet map's vehicle pins — the Android `GoogleMap` and the iOS `MKMapView`. */
    const val FLEET_MAP: String = "fleet-map"

    /** The vehicle rows in the live-location overview sheet (address text, no pin). */
    const val FLEET_SHEET_ROW: String = "fleet-sheet-row"

    /** Track & Trace: the single followed vehicle and its address chip. */
    const val TRACK_TRACE_MAP: String = "track-trace-map"

    /** A caller that did not name itself. */
    const val UNSPECIFIED: String = "unspecified"
}

/**
 * One address as it was shown on a marker.
 *
 * @property source which marker, out of [MarkerAddressSource].
 * @property ownerKey the device id the marker stands for, or the screen's own key when
 *   there is no device row behind it.
 * @property address the text on the marker.
 * @property markerLatitude the API fix the marker is placed at — the vehicle's real
 *   reported position, not an interpolated animation frame.
 * @property markerLongitude the API fix's longitude.
 * @property addressLatitude the latitude the *geocoder response* gives for [address],
 *   or null when the platform supplied the match no position.
 * @property addressLongitude the response's longitude, same caveat.
 */
data class MarkerAddressFix(
    val source: String,
    val ownerKey: String,
    val address: String,
    val markerLatitude: Double,
    val markerLongitude: Double,
    val addressLatitude: Double?,
    val addressLongitude: Double?,
) {
    /**
     * How far the address the geocoder named sits from the marker it was asked about,
     * in metres, or null when the response carried no coordinates.
     *
     * This is the number that tells the two failure modes apart: a few metres is the
     * address under the vehicle, a kilometre is the geocoder naming the nearest place
     * it knows about.
     */
    val offsetMeters: Double?
        get() {
            val lat = addressLatitude ?: return null
            val lng = addressLongitude ?: return null
            return approxDistanceMeters(
                GeoPoint(markerLatitude, markerLongitude),
                GeoPoint(lat, lng),
            )
        }
}

/** Beyond this many entries the oldest records are dropped. */
internal const val MARKER_ADDRESS_HISTORY_LIMIT = 100

object MarkerAddressRecorder {

    /**
     * The newest record per marker, keyed `source|ownerKey`.
     *
     * Snapshot-backed so a debug overlay can read it and recompose; nothing in the
     * shipping UI reads it today.
     */
    private val latest = mutableStateMapOf<String, MarkerAddressFix>()

    /** The trail, oldest first, capped at [MARKER_ADDRESS_HISTORY_LIMIT]. */
    private val trail = mutableListOf<MarkerAddressFix>().toMutableStateList()

    /**
     * Records [fix], unless it is identical to the last record for the same marker.
     *
     * The de-duplication is not just tidiness: this is called from a `SideEffect` on
     * every recomposition of a marker, and writing snapshot state unconditionally from
     * there would invalidate any reader on every frame.
     */
    fun record(fix: MarkerAddressFix) {
        val key = keyOf(fix.source, fix.ownerKey)
        if (latest[key] == fix) return

        latest[key] = fix

        if (trail.size >= MARKER_ADDRESS_HISTORY_LIMIT) {
            trail.removeAt(0)
        }
        trail.add(fix)

        println(
            """
            ========== EV2Z MARKER ADDRESS ==========
            Source       : ${fix.source}
            Owner        : ${fix.ownerKey}
            Address      : ${fix.address}
            Marker Lat   : ${fix.markerLatitude}
            Marker Lng   : ${fix.markerLongitude}
            Response Lat : ${fix.addressLatitude ?: "—"}
            Response Lng : ${fix.addressLongitude ?: "—"}
            Offset       : ${fix.offsetMeters?.let { "${it.toInt()} m" } ?: "—"}
            =========================================
            """.trimIndent()
        )
    }

    /** The address last shown on one marker, or null if that marker never showed one. */
    fun latestFor(source: String, ownerKey: String): MarkerAddressFix? =
        latest[keyOf(source, ownerKey)]

    /** Every marker's newest record. */
    fun all(): List<MarkerAddressFix> = latest.values.toList()

    /** The recorded trail, oldest first. */
    fun history(): List<MarkerAddressFix> = trail.toList()

    /** Drops everything — for tests, and for a logout that should not keep positions. */
    fun clear() {
        latest.clear()
        trail.clear()
    }

    private fun keyOf(source: String, ownerKey: String) = "$source|$ownerKey"
}
