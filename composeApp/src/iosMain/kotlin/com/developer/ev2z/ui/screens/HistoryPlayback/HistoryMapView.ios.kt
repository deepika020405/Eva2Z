// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryMapView.ios.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.developer.ev2z.response.RoutePoint
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIEdgeInsetsMake

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

            val coordinates = routePoints.mapNotNull { point ->
                val lat = point.getLatDouble()
                val lng = point.getLngDouble()
                if (lat != null && lng != null) {
                    CLLocationCoordinate2DMake(lat, lng)
                } else null
            }

            if (coordinates.isNotEmpty()) {
                memScoped {
                    val coordsPointer = allocArrayOf(coordinates.toTypedArray())
                    val polyline = MKPolyline.polylineWithCoordinates(
                        coords = coordsPointer,
                        count = coordinates.size.toULong()
                    )
                    mapView.addOverlay(polyline)

                    val padding = UIEdgeInsetsMake(50.0, 50.0, 50.0, 50.0)
                    mapView.setVisibleMapRect(
                        polyline.boundingMapRect,
                        edgePadding = padding,
                        animated = false
                    )
                }
            }

            mapView
        },
        update = { mapView ->
            if (currentIndex in routePoints.indices) {
                val point = routePoints[currentIndex]
                val lat = point.getLatDouble() ?: return@UIKitView
                val lng = point.getLngDouble() ?: return@UIKitView

                mapView.removeAnnotations(mapView.annotations)

                val annotation = MKPointAnnotation().apply {
                    setCoordinate(CLLocationCoordinate2DMake(lat, lng))
                    setTitle("Point ${currentIndex + 1}")
                    setSubtitle(point.createdAt)
                }
                mapView.addAnnotation(annotation)

                val region = MKCoordinateRegionMake(
                    annotation.coordinate,
                    MKCoordinateSpanMake(0.01, 0.01)
                )
                mapView.setRegion(region, animated = true)
            }
        }
    )
}