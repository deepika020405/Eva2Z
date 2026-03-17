package com.developer.ev2z.ui.screens.device_Report

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import com.developer.ev2z.ui.screens.trackTrace.TrailPoint
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject
import kotlin.math.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double,
    trailPoints: List<TrailPoint>
) {
    val annotation = remember { MKPointAnnotation() }
    val lastLocation = remember { doubleArrayOf(latitude, longitude) }
    var isHybrid by remember { mutableStateOf(false) } // ✅ ADD

    Box(modifier = modifier) { // ✅ Box wrap

        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val mapView = MKMapView()

                val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
                annotation.setCoordinate(coordinate)
                annotation.setTitle("Device")
                mapView.addAnnotation(annotation)

                val region = MKCoordinateRegionMakeWithDistance(
                    coordinate,
                    zoom * 1000,
                    zoom * 1000
                )
                mapView.setRegion(region, animated = false)

                mapView.delegate = object : NSObject(), MKMapViewDelegateProtocol {
                    override fun mapView(
                        mapView: MKMapView,
                        viewForAnnotation: MKAnnotationProtocol
                    ): MKAnnotationView? {
                        if (viewForAnnotation is MKUserLocation) return null

                        val id = "default_pin"
                        val view =
                            mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                                ?: MKPinAnnotationView(null, id)

                        view.annotation = viewForAnnotation
                        view.canShowCallout = false
                        return view
                    }
                }

                mapView
            },
            update = { mapView ->

                // ✅ mapType toggle
                mapView.setMapType(
                    if (isHybrid) MKMapTypeHybrid else MKMapTypeStandard
                )

                val newCoordinate = CLLocationCoordinate2DMake(latitude, longitude)

                val bearing = calculateBearing(
                    lastLocation[0],
                    lastLocation[1],
                    latitude,
                    longitude
                )

                annotation.setCoordinate(newCoordinate)

                // 🔁 rotate marker
                mapView.viewForAnnotation(annotation)?.transform =
                    CGAffineTransformMakeRotation(bearing * PI / 180.0)

                // 🎥 camera follow
                mapView.setCenterCoordinate(newCoordinate, animated = true)

                lastLocation[0] = latitude
                lastLocation[1] = longitude
            }
        )

        // ✅ Toggle Button
        // ✅ Middle right
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)    // ← BottomEnd se CenterEnd
                .padding(end = 10.dp)          // ← bottom padding hatao
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .clickable { isHybrid = !isHybrid }
                .padding(8.dp)
        ) {
            Text(
                text = if (!isHybrid) "🛰️" else "🗺️",
                fontSize = 18.sp
            )
        }
    }
}

fun calculateBearing(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val startLat = lat1 * PI / 180
    val startLng = lon1 * PI / 180
    val endLat   = lat2 * PI / 180
    val endLng   = lon2 * PI / 180

    val dLng = endLng - startLng

    val y = sin(dLng) * cos(endLat)
    val x = cos(startLat) * sin(endLat) -
            sin(startLat) * cos(endLat) * cos(dLng)

    return (atan2(y, x) * 180 / PI + 360) % 360
}