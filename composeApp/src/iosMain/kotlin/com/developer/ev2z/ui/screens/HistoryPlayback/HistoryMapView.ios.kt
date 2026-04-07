package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.response.RoutePoint
import kotlinx.cinterop.*
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HistoryMapView(
    routePoints: List<RoutePoint>,
    currentIndex: Int,
    modifier: Modifier
) {
    if (routePoints.isEmpty()) return

    var isHybrid by remember { mutableStateOf(false) }

    // Track which routePoints list we've drawn the polyline for
    var drawnRouteId by remember { mutableStateOf(0) }
    val currentRouteId = remember(routePoints) { drawnRouteId + 1 }

    // Create delegate inline and hold strong reference
    val mapDelegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(
                mapView: MKMapView,
                rendererForOverlay: MKOverlayProtocol
            ): MKOverlayRenderer {
                val polylineOverlay = rendererForOverlay as? MKPolyline
                if (polylineOverlay != null) {
                    return MKPolylineRenderer(polyline = polylineOverlay).apply {
                        strokeColor = UIColor(red = 0.298, green = 0.686, blue = 0.314, alpha = 1.0)
                        lineWidth = 5.0
                    }
                }
                return MKOverlayRenderer(overlay = rendererForOverlay)
            }
        }
    }

    Box(modifier = modifier) {
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val mapView = MKMapView()
                mapView.setShowsUserLocation(false)
                mapView.delegate = mapDelegate
                mapView
            },
            update = { mapView ->
                mapView.delegate = mapDelegate

                mapView.setMapType(
                    if (isHybrid) MKMapTypeHybrid else MKMapTypeStandard
                )

                // Redraw polyline when routePoints change
                if (currentRouteId != drawnRouteId) {
                    drawnRouteId = currentRouteId

                    // Remove old overlays
                    mapView.removeOverlays(mapView.overlays)

                    val coordinates = routePoints.mapNotNull { point ->
                        val lat = point.getLatDouble()
                        val lng = point.getLngDouble()
                        if (lat != null && lng != null) {
                            CLLocationCoordinate2DMake(lat, lng)
                        } else null
                    }

                    if (coordinates.isNotEmpty()) {
                        memScoped {
                            val coordArray = allocArray<CLLocationCoordinate2D>(coordinates.size)
                            coordinates.forEachIndexed { index, coord ->
                                coordArray[index].latitude = coord.useContents { latitude }
                                coordArray[index].longitude = coord.useContents { longitude }
                            }

                            val polyline = MKPolyline.polylineWithCoordinates(
                                coords = coordArray,
                                count = coordinates.size.toULong()
                            )
                            mapView.addOverlay(polyline as MKOverlayProtocol)

                            val padding = UIEdgeInsetsMake(60.0, 60.0, 60.0, 60.0)
                            mapView.setVisibleMapRect(
                                polyline.boundingMapRect,
                                edgePadding = padding,
                                animated = true
                            )
                        }
                    }
                }

                // Update marker position
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

        // Satellite toggle button
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .clickable { isHybrid = !isHybrid }
                .padding(8.dp)
        ) {
            Text(
                text = if (!isHybrid) "\uD83D\uDEF0\uFE0F" else "\uD83D\uDDFA\uFE0F",
                fontSize = 18.sp
            )
        }
    }
}