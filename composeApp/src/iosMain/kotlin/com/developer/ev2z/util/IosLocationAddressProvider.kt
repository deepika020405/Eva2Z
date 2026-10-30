// Reading a CLLocationCoordinate2D out of a placemark needs the cinterop opt-in, same
// as every other file here that touches one.
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.developer.ev2z.util

import kotlinx.cinterop.useContents
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

/**
 * A `CLPlacemark` carries the matched place's own position in `location`, which is what
 * the recorded response coordinates come from. It is optional on the placemark — a
 * match with none still gives an address, so the text is kept and the coordinates come
 * back null rather than taking the whole answer down.
 */
actual suspend fun geocodeAddressAt(
    lat: Double,
    lng: Double
): GeocodedAddress? = suspendCancellableCoroutine { cont ->

    val geocoder = CLGeocoder()
    val location = CLLocation(latitude = lat, longitude = lng)

    geocoder.reverseGeocodeLocation(location) { placemarks, error ->

        if (error != null) {
            cont.resume(null)
            return@reverseGeocodeLocation
        }

        val placemark = placemarks?.firstOrNull() as? CLPlacemark
        if (placemark == null) {
            cont.resume(null)
            return@reverseGeocodeLocation
        }

        val address = listOfNotNull(
            placemark.name,
            placemark.locality,
            placemark.administrativeArea,
            placemark.country
        ).joinToString(", ")

        if (address.isBlank()) {
            cont.resume(null)
            return@reverseGeocodeLocation
        }

        val matched = placemark.location
        cont.resume(
            GeocodedAddress(
                address = address,
                latitude = matched?.coordinate?.useContents { latitude },
                longitude = matched?.coordinate?.useContents { longitude },
            )
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