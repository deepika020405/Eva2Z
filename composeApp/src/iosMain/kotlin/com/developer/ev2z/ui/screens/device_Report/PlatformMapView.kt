package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIImage
import platform.darwin.NSObject

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
            val mapView = MKMapView()

            val coordinate = CLLocationCoordinate2DMake(latitude, longitude)

            val region = MKCoordinateRegionMakeWithDistance(
                coordinate,
                zoom * 1000,
                zoom * 1000
            )
            mapView.setRegion(region, animated = true)

            // 📍 Annotation
            val annotation = MKPointAnnotation().apply {
                setCoordinate(coordinate)
                setTitle("Device")
            }

            mapView.addAnnotation(annotation)

            // 🎯 Delegate
            mapView.delegate = object : NSObject(), MKMapViewDelegateProtocol {

                override fun mapView(
                    mapView: MKMapView,
                    viewForAnnotation: MKAnnotationProtocol
                ): MKAnnotationView? {

                    if (viewForAnnotation is MKUserLocation) return null

                    val identifier = "bike_marker"

                    val annotationView =
                        mapView.dequeueReusableAnnotationViewWithIdentifier(identifier)
                            ?: MKAnnotationView(viewForAnnotation, identifier)

                    annotationView.annotation = viewForAnnotation
                    annotationView.canShowCallout = true

                    // ✅ Custom icon
                    annotationView.image = UIImage.imageNamed("ic_vechile")

                    return annotationView
                }
            }

            mapView
        }
    )
}
