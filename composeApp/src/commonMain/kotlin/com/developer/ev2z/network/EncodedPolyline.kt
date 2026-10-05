// composeApp/src/commonMain/kotlin/com/developer/ev2z/network/EncodedPolyline.kt
package com.developer.ev2z.network

/**
 * Decodes Google's [encoded polyline algorithm][1] — the format every Directions step
 * returns its geometry in.
 *
 * Latitude and longitude are stored as deltas from the previous point, each delta
 * zig-zag encoded into 5-bit chunks offset by 63 so the result is printable ASCII. A
 * chunk with the 0x20 bit set means another chunk follows.
 *
 * Returns an empty list for malformed input rather than throwing: this decodes a
 * network answer, and a route overlay must never take the playback screen down.
 *
 * [1]: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
internal fun decodePolyline(encoded: String): List<GeoPoint> {
    val points = ArrayList<GeoPoint>(encoded.length / 2)
    var index = 0
    var lat = 0
    var lng = 0

    while (index < encoded.length) {
        val dLat = readValue(encoded, index) ?: return points
        index = dLat.nextIndex
        lat += dLat.value

        val dLng = readValue(encoded, index) ?: return points
        index = dLng.nextIndex
        lng += dLng.value

        points += GeoPoint(lat / 1e5, lng / 1e5)
    }

    return points
}

/** One decoded delta, and where the next one starts. */
private class PolylineValue(val value: Int, val nextIndex: Int)

/** Reads one zig-zag encoded delta, or null if the chunks run off the end of [encoded]. */
private fun readValue(encoded: String, start: Int): PolylineValue? {
    var index = start
    var shift = 0
    var result = 0

    while (true) {
        if (index >= encoded.length) return null
        val chunk = encoded[index++].code - 63
        result = result or ((chunk and 0x1f) shl shift)
        shift += 5
        if (chunk < 0x20) break
        // A delta is at most 32 bits, so six chunks; more than that is corrupt input.
        if (shift > 32) return null
    }

    // Negative values are stored inverted, flagged by the low bit.
    val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
    return PolylineValue(value, index)
}