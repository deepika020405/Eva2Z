package com.developer.ev2z.util



expect suspend fun getAddressFromLatLng(
    lat: Double,
    lng: Double
): String

/**
 * One reverse-geocoder answer: the address a marker shows, plus the coordinates the
 * geocoder itself reports for that address.
 *
 * [getAddressFromLatLng] throws the second half away. The address on a marker is asked
 * for at the vehicle's fix, but the geocoder answers with a *place* — and the place it
 * picked has its own position, which is the only way to tell "the address under the
 * marker" from "an address a few hundred metres up the road that happened to be the
 * nearest thing the geocoder could name". Recording both is what
 * [com.developer.ev2z.util.MarkerAddressRecorder] exists for.
 *
 * @property address the text shown on the marker.
 * @property latitude the response's own latitude, or null when the platform gives the
 *   match no position — Android's `Address` may carry no coordinates, and an iOS
 *   `CLPlacemark` may have no `location`.
 * @property longitude the response's own longitude, same caveat.
 */
data class GeocodedAddress(
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
)

/**
 * The same reverse geocode as [getAddressFromLatLng], keeping the response's own
 * coordinates.
 *
 * Returns null — rather than a sentence like "Unknown location" — when the lookup fails
 * or answers with nothing usable, so a failure can never be cached or recorded as if it
 * were an address.
 */
expect suspend fun geocodeAddressAt(
    lat: Double,
    lng: Double
): GeocodedAddress?

/** Returns just the city/locality (e.g. "Pune") for the given coordinates, or empty if unknown. */
expect suspend fun getCityFromLatLng(
    lat: Double,
    lng: Double
): String

/**
 * Returns the state / administrative area (e.g. "Uttar Pradesh") for the given
 * coordinates, or empty if unknown. Used to pick the right state's fuel rates.
 */
expect suspend fun getStateFromLatLng(
    lat: Double,
    lng: Double
): String