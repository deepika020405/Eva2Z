package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
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
    zoom: Double
) {
    val annotation = remember { MKPointAnnotation() }
    val lastLocation = remember { doubleArrayOf(latitude, longitude) }

    UIKitView(
        modifier = modifier,
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

            val newCoordinate =
                CLLocationCoordinate2DMake(latitude, longitude)

            val bearing = calculateBearing(
                lastLocation[0],
                lastLocation[1],
                latitude,
                longitude
            )

            annotation.setCoordinate(newCoordinate)

            // 🔁 rotate marker (default pin)
            mapView.viewForAnnotation(annotation)?.transform =
                CGAffineTransformMakeRotation(bearing * PI / 180.0)

            // 🎥 camera follow
            mapView.setCenterCoordinate(newCoordinate, animated = true)

            lastLocation[0] = latitude
            lastLocation[1] = longitude
        }
    )
}


fun calculateBearing(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val startLat = lat1 * PI / 180
    val startLng = lon1 * PI / 180
    val endLat = lat2 * PI / 180
    val endLng = lon2 * PI / 180

    val dLng = endLng - startLng

    val y = sin(dLng) * cos(endLat)
    val x =
        cos(startLat) * sin(endLat) -
                sin(startLat) * cos(endLat) * cos(dLng)

    return (atan2(y, x) * 180 / PI + 360) % 360
}
