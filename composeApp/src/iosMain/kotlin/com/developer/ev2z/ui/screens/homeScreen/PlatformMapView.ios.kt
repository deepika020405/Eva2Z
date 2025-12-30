// iosMain/ui/screens/homeScreen/PlatformMapView.ios.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.developer.ev2z.data.local.model.Device
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
) {
    val mapView = remember { MKMapView() }

    UIKitView(
        modifier = modifier,
        factory = {
            mapView.setShowsUserLocation(false)
            mapView
        },
        update = { view ->
            // Clear existing annotations
            view.removeAnnotations(view.annotations())

            // Add markers for all devices
            devices.forEach { device ->
                device.getLatitudeDouble()?.let { lat ->
                    device.getLongitudeDouble()?.let { lng ->
                        val annotation = MKPointAnnotation()
                        annotation.setCoordinate(CLLocationCoordinate2DMake(lat, lng))
                        annotation.setTitle(device.getDisplayName())
                        annotation.setSubtitle("${device.getStatusDisplay()} - ${device.speed} km/h")
                        view.addAnnotation(annotation)
                    }
                }
            }

            // Zoom to show all devices
            if (devices.isNotEmpty()) {
                var minLat = Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE
                var minLng = Double.MAX_VALUE
                var maxLng = -Double.MAX_VALUE

                devices.forEach { device ->
                    device.getLatitudeDouble()?.let { lat ->
                        device.getLongitudeDouble()?.let { lng ->
                            minLat = minOf(minLat, lat)
                            maxLat = maxOf(maxLat, lat)
                            minLng = minOf(minLng, lng)
                            maxLng = maxOf(maxLng, lng)
                        }
                    }
                }

                val centerLat = (minLat + maxLat) / 2
                val centerLng = (minLng + maxLng) / 2
                val spanLat = (maxLat - minLat) * 1.3 // Add 30% padding
                val spanLng = (maxLng - minLng) * 1.3

                val coordinate = CLLocationCoordinate2DMake(centerLat, centerLng)
                val span = MKCoordinateSpanMake(spanLat, spanLng)
                val region = MKCoordinateRegionMake(coordinate, span)
                view.setRegion(region, animated = true)
            }

            // Zoom to selected device if any
            selectedDevice?.let { device ->
                device.getLatitudeDouble()?.let { lat ->
                    device.getLongitudeDouble()?.let { lng ->
                        val coordinate = CLLocationCoordinate2DMake(lat, lng)
                        val span = MKCoordinateSpanMake(0.01, 0.01) // Closer zoom
                        val region = MKCoordinateRegionMake(coordinate, span)
                        view.setRegion(region, animated = true)
                    }
                }
            }
        }
    )
}