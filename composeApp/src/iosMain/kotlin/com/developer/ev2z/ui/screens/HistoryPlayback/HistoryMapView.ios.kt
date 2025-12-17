// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryMapView.ios.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.developer.ev2z.response.RoutePoint
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HistoryMapView(
    routePoints: List<RoutePoint>,
    currentIndex: Int,
    modifier: Modifier
) {
    if (routePoints.isEmpty()) return

    UIKitView(
        modifier = modifier,
        factory = {
            val mapView = MKMapView()
            mapView.setShowsUserLocation(false)

            // ✅ Create polyline coordinates
            val coordinates = routePoints.mapNotNull { point ->
                val lat = point.getLatDouble()
                val lng = point.getLngDouble()
                if (lat != null && lng != null) {
                    CLLocationCoordinate2DMake(lat, lng)
                } else null
            }.toTypedArray()

            if (coordinates.isNotEmpty()) {
                // ✅ Add polyline overlay
                val polyline = MKPolyline.polylineWithCoordinates(
                    coordinates = coordinates,
                    count = coordinates.size.toULong()
                )
                mapView.addOverlay(polyline)

                // ✅ Fit map to show entire route
                mapView.setVisibleMapRect(
                    polyline.boundingMapRect,
                    animated = false
                )
            }

            mapView
        },
        update = { mapView ->
            // ✅ Update marker position during playback
            if (currentIndex in routePoints.indices) {
                val point = routePoints[currentIndex]
                val lat = point.getLatDouble() ?: return@UIKitView
                val lng = point.getLngDouble() ?: return@UIKitView

                // Remove old annotations
                mapView.removeAnnotations(mapView.annotations)

                // Add current position marker
                val annotation = MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(lat, lng))
                    setTitle("Point ${currentIndex + 1}")
                    setSubtitle(point.createdAt)
                }
                mapView.addAnnotation(annotation)

                // Center on current point
                val region = MKCoordinateRegionMake(
                    annotation.coordinate,
                    MKCoordinateSpanMake(0.01, 0.01)
                )
                mapView.setRegion(region, animated = true)
            }
        }
    )
}