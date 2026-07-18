package com.developer.ev2z.util



expect suspend fun getAddressFromLatLng(
    lat: Double,
    lng: Double
): String

/** Returns just the city/locality (e.g. "Pune") for the given coordinates, or empty if unknown. */
expect suspend fun getCityFromLatLng(
    lat: Double,
    lng: Double
): String