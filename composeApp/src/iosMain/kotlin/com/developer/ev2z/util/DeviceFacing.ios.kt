package com.developer.ev2z.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDeviceFacing(): DeviceFacing? {
    var position by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Degrees from true north. NaN until the first reading — a cone pointing north
    // because nothing has been measured yet is worse than no cone.
    var heading by remember { mutableStateOf(Double.NaN) }

    DisposableEffect(Unit) {
        val manager = CLLocationManager()

        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {

            override fun locationManager(
                manager: CLLocationManager,
                didUpdateLocations: List<*>
            ) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
                location.coordinate.useContents {
                    position = latitude to longitude
                }
            }

            override fun locationManager(
                manager: CLLocationManager,
                didUpdateHeading: CLHeading
            ) {
                // trueHeading is negative when iOS cannot work out the declination —
                // no location fix yet, usually. magneticHeading is still worth drawing
                // then: it is a degree or two out in India, which is invisible on a
                // cone this size.
                val reading = didUpdateHeading.trueHeading
                    .takeIf { it >= 0.0 }
                    ?: didUpdateHeading.magneticHeading

                if (reading < 0.0) return
                heading = reading
            }
        }

        manager.delegate = delegate
        // The maps already ask for this; requesting again is a no-op once granted and
        // the cone is simply never drawn if it is refused.
        manager.requestWhenInUseAuthorization()
        // Five metres: this draws a cone a few metres across, so chasing every
        // sub-metre update would only run the battery down.
        manager.distanceFilter = 5.0
        manager.headingFilter = 2.0
        manager.startUpdatingLocation()
        manager.startUpdatingHeading()

        onDispose {
            manager.stopUpdatingHeading()
            manager.stopUpdatingLocation()
            manager.delegate = null
        }
    }

    val (latitude, longitude) = position ?: return null
    return DeviceFacing(latitude, longitude, heading)
}
