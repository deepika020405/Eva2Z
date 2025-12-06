package com.developer.ev2z.ui.screens.device_Report

import platform.CoreLocation.*
import platform.Foundation.*

actual class PlatformAddressProvider actual constructor(context: Any?) {
    actual fun getAddress(lat: Double, lng: Double, callback: (String?) -> Unit) {
        val geocoder = CLGeocoder()
        val location = CLLocation(latitude = lat, longitude = lng)

        geocoder.reverseGeocodeLocation(location) { placemarks, error ->
            val place = placemarks?.firstOrNull() as? CLPlacemark

            if (place == null) {
                callback(null)
                return@reverseGeocodeLocation
            }

            // Helper to get String safely via KVC
            fun CLPlacemark.stringForKey(key: String): String? =
                this.valueForKey(key) as? String

            val name = place.stringForKey("name")                // street / place name
            val subLocality = place.stringForKey("subLocality")  // area / colony
            val locality = place.stringForKey("locality")        // city
            val adminArea = place.stringForKey("administrativeArea") // state
            val country = place.stringForKey("country")          // country

            val parts: List<String> = listOfNotNull(
                name,
                subLocality,
                locality,
                adminArea,
                country
            )

            val address = parts.joinToString(", ")

            callback(if (address.isBlank()) null else address)
        }
    }
}