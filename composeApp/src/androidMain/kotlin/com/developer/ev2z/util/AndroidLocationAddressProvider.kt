package com.developer.ev2z.util


import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

lateinit var appContext: Context

fun initGeocoder(context: Context) {
    appContext = context.applicationContext
}

actual suspend fun getAddressFromLatLng(
    lat: Double,
    lng: Double
): String = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(appContext, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lng, 1)

        addresses?.firstOrNull()?.getAddressLine(0)
            ?: "Unknown location"
    } catch (e: Exception) {
        "Location unavailable"
    }
}

/**
 * `Geocoder` hands back an `Address` that carries the matched place's own coordinates
 * in `latitude`/`longitude` — guarded by `hasLatitude()`/`hasLongitude()`, because a
 * match assembled from a postal lookup can come back with none.
 */
actual suspend fun geocodeAddressAt(
    lat: Double,
    lng: Double
): GeocodedAddress? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(appContext, Locale.getDefault())
        val match = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
            ?: return@withContext null

        val line = match.getAddressLine(0)?.takeIf { it.isNotBlank() }
            ?: return@withContext null

        GeocodedAddress(
            address = line,
            latitude = if (match.hasLatitude()) match.latitude else null,
            longitude = if (match.hasLongitude()) match.longitude else null,
        )
    } catch (e: Exception) {
        // Null, not "Location unavailable": the caller must be able to tell a failed
        // lookup from an address, or it caches the failure and shows it forever.
        null
    }
}

actual suspend fun getStateFromLatLng(
    lat: Double,
    lng: Double
): String = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(appContext, Locale.US)
        geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.adminArea.orEmpty()
    } catch (e: Exception) {
        ""
    }
}

actual suspend fun getCityFromLatLng(
    lat: Double,
    lng: Double
): String = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(appContext, Locale.getDefault())
        val address = geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
        address?.locality
            ?: address?.subAdminArea
            ?: address?.adminArea
            ?: ""
    } catch (e: Exception) {
        ""
    }
}