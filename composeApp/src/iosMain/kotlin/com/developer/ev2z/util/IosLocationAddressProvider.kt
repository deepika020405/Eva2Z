package com.developer.ev2z.util

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLPlacemark
import kotlin.coroutines.resume

actual suspend fun getAddressFromLatLng(
    lat: Double,
    lng: Double
): String = suspendCancellableCoroutine<String> { cont ->

    val geocoder = CLGeocoder()
    val location = CLLocation(latitude = lat, longitude = lng)

    geocoder.reverseGeocodeLocation(location) { placemarks, error ->

        if (error != null) {
            cont.resume("Location unavailable")
            return@reverseGeocodeLocation
        }

        val placemark = placemarks
            ?.firstOrNull() as? CLPlacemark

        if (placemark == null) {
            cont.resume("Unknown location")
            return@reverseGeocodeLocation
        }

        val address = listOfNotNull(
            placemark.name,
            placemark.locality,
            placemark.administrativeArea,
            placemark.country
        ).joinToString(", ")

        cont.resume(
            if (address.isNotBlank()) address else "Unknown location"
        )
    }
}

actual suspend fun getStateFromLatLng(
    lat: Double,
    lng: Double
): String = suspendCancellableCoroutine<String> { cont ->

    val geocoder = CLGeocoder()
    val location = CLLocation(latitude = lat, longitude = lng)

    geocoder.reverseGeocodeLocation(location) { placemarks, error ->

        if (error != null) {
            cont.resume("")
            return@reverseGeocodeLocation
        }

        val placemark = placemarks?.firstOrNull() as? CLPlacemark
        cont.resume(placemark?.administrativeArea ?: "")
    }
}

actual suspend fun getCityFromLatLng(
    lat: Double,
    lng: Double
): String = suspendCancellableCoroutine<String> { cont ->

    val geocoder = CLGeocoder()
    val location = CLLocation(latitude = lat, longitude = lng)

    geocoder.reverseGeocodeLocation(location) { placemarks, error ->

        if (error != null) {
            cont.resume("")
            return@reverseGeocodeLocation
        }

        val placemark = placemarks?.firstOrNull() as? CLPlacemark

        val city = placemark?.locality
            ?: placemark?.subAdministrativeArea
            ?: placemark?.administrativeArea
            ?: ""

        cont.resume(city)
    }
}