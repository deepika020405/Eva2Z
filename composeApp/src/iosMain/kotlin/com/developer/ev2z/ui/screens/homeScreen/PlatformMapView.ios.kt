// iosMain/ui/screens/homeScreen/PlatformMapView.ios.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
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
    UIKitView(
        modifier = modifier,
        factory = {
            val mapView = MKMapView()
            mapView.setShowsUserLocation(false)

            // Set initial region
            val coordinate = CLLocationCoordinate2DMake(28.6139, 77.2090)
            val span = MKCoordinateSpanMake(0.1, 0.1)
            val region = MKCoordinateRegionMake(coordinate, span)
            mapView.setRegion(region, animated = false)

            // Add markers
            devices.forEach { device ->
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(
                    CLLocationCoordinate2DMake(device.latitude?.toDouble() ?: 0.0, device.longitude?.toDouble() ?: 0.0)
                )
                annotation.setTitle(device.deviceName)
                annotation.setSubtitle(device.status)
                mapView.addAnnotation(annotation)
            }

            mapView
        },
        update = { mapView ->
            // Update markers if needed
        }
    )
}