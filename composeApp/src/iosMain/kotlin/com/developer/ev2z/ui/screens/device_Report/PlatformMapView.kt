package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationDegrees
import platform.MapKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double
) {
    UIKitView(factory = {
        val mapView = MKMapView()
        val coord = CLLocationCoordinate2DMake(latitude, longitude)

        val region = MKCoordinateRegionMake(
            coord,
            MKCoordinateSpanMake(0.01, 0.01)
        )
        mapView.setRegion(region, true)

        val marker = MKPointAnnotation().apply {
            this.setCoordinate(coord)
            this.setTitle("Device")        // ✅ correct
        }

        mapView.addAnnotation(marker)

        mapView
    }, modifier = modifier)
}