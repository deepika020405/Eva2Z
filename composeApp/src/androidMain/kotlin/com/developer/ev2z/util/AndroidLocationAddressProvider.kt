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