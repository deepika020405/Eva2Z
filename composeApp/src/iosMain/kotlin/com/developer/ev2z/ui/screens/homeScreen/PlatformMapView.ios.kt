package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MapKit.*
import platform.CoreLocation.CLLocationCoordinate2DMake

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double
) {
    UIKitView(
        modifier = modifier,
        factory = {
            val map = MKMapView()

            val center = CLLocationCoordinate2DMake(latitude, longitude)

            // Set region
            val span = MKCoordinateSpanMake(0.1, 0.1)
            val region = MKCoordinateRegionMake(center, span)
            map.setRegion(region, animated = true)

            // ⭐ FIXED MARKER (NO 'val cannot be reassigned')
            val annotation = MKPointAnnotation()
            annotation.setCoordinate(center)
            annotation.setTitle("Delhi")
            annotation.setSubtitle("India Gate, CP, Raj path")

            map.addAnnotation(annotation)

            map
        }
    )
}