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
import com.developer.ev2z.network.SnappedRoute
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
    snappedRoute: SnappedRoute,
    currentIndex: Int,
    modifier: Modifier
) {
    if (routePoints.isEmpty()) return

    var isHybrid by remember { mutableStateOf(false) }

    // Updated every animation frame; read inside `update` below, which re-runs with it.
    val smoothPosition = rememberSmoothPlaybackPosition(routePoints, currentIndex, snappedRoute)

    // The one pin, kept across frames so it can be moved instead of rebuilt.
    val markerAnnotation = remember { arrayOfNulls<MKPointAnnotation>(1) }

    // Track which route we've drawn the polyline for. Keyed on the snapped route too,
    // so the straight line drawn while snapping was in flight is replaced by the road
    // geometry when it lands.
    var drawnRouteId by remember { mutableStateOf(0) }
    val currentRouteId = remember(routePoints, snappedRoute) { drawnRouteId + 1 }

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

                    // The road-snapped path when it is ready, the recorded fixes until
                    // then — the snapped one bends with the street instead of cutting
                    // between fixes.
                    val coordinates = if (snappedRoute.path.size >= 2) {
                        snappedRoute.path.map {
                            CLLocationCoordinate2DMake(it.latitude, it.longitude)
                        }
                    } else {
                        routePoints.mapNotNull { point ->
                            val lat = point.getLatDouble()
                            val lng = point.getLngDouble()
                            if (lat != null && lng != null) {
                                CLLocationCoordinate2DMake(lat, lng)
                            } else null
                        }
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

                // Move the marker along the glide. The pin is kept and repositioned
                // rather than removed and re-added: rebuilding it 60 times a second
                // flickers, and the old code's remove/add was also what made the
                // annotation blink on every playback step.
                val position = smoothPosition.value
                if (position != null) {
                    val coordinate = CLLocationCoordinate2DMake(
                        position.latitude,
                        position.longitude,
                    )

                    val marker = markerAnnotation[0]
                        ?: MKPointAnnotation().also {
                            markerAnnotation[0] = it
                            mapView.addAnnotation(it)
                        }

                    marker.setCoordinate(coordinate)
                    marker.setTitle("Point ${currentIndex + 1}")
                    marker.setSubtitle(routePoints.getOrNull(currentIndex)?.createdAt ?: "")

                    // Centre only — setRegion with a fixed 0.01 span re-imposed its own
                    // zoom on every step, so the map could not be zoomed out while
                    // playing. animated = false because the glide is already the
                    // animation; animating on top of it would lag behind the marker.
                    mapView.setCenterCoordinate(coordinate, animated = false)
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