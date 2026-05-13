package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import platform.CoreGraphics.CGAffineTransformMakeRotation
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager
import platform.MapKit.*
import platform.UIKit.UIBezierPath
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
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
    val vehicleAnnotation        = remember { MKPointAnnotation() }
    val startAnnotation          = remember { MKPointAnnotation() }
    val userAnnotation           = remember { MKPointAnnotation() }
    val polylineRef              = remember { arrayOfNulls<MKPolyline>(1) }
    val userToDevicePolylineRef  = remember { arrayOfNulls<MKPolyline>(1) }
    val mapViewRef               = remember { arrayOfNulls<MKMapView>(1) }
    val locationManager          = remember { CLLocationManager() }
    val lastLocation             = remember { doubleArrayOf(latitude, longitude) }
    val arrowImage               = remember { buildNavigationArrowImage() }
    var startAnnotationAdded     by remember { mutableStateOf(false) }
    var userAnnotationAdded      by remember { mutableStateOf(false) }
    var isHybrid                 by remember { mutableStateOf(false) }
    var routeActive              by remember { mutableStateOf(false) }

    Box(modifier = modifier) {

        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val mapView = MKMapView()
                mapViewRef[0] = mapView

                locationManager.requestWhenInUseAuthorization()
                mapView.showsUserLocation = true

                val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
                vehicleAnnotation.setCoordinate(coordinate)
                vehicleAnnotation.setTitle("Vehicle")
                mapView.addAnnotation(vehicleAnnotation)

                val region = MKCoordinateRegionMakeWithDistance(
                    coordinate,
                    zoom * 1000,
                    zoom * 1000
                )
                mapView.setRegion(region, animated = false)

                mapView.delegate = object : NSObject(), MKMapViewDelegateProtocol {

                    // ── Custom annotation views ─────────────────────
                    override fun mapView(
                        mapView: MKMapView,
                        viewForAnnotation: MKAnnotationProtocol
                    ): MKAnnotationView? {
                        if (viewForAnnotation is MKUserLocation) return null

                        val title = viewForAnnotation.title ?: ""
                        return when (title) {
                            "Start" -> {
                                val id = "start_marker"
                                val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                                    as? MKMarkerAnnotationView
                                    ?: MKMarkerAnnotationView(
                                        annotation = viewForAnnotation,
                                        reuseIdentifier = id
                                    )
                                view.annotation = viewForAnnotation
                                view.markerTintColor = UIColor(
                                    red = 0x4C / 255.0, green = 0xAF / 255.0,
                                    blue = 0x50 / 255.0, alpha = 1.0
                                )
                                view.glyphText = "S"
                                view.canShowCallout = true
                                view
                            }
                            "You" -> {
                                val id = "user_marker"
                                val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                                    as? MKMarkerAnnotationView
                                    ?: MKMarkerAnnotationView(
                                        annotation = viewForAnnotation,
                                        reuseIdentifier = id
                                    )
                                view.annotation = viewForAnnotation
                                view.markerTintColor = UIColor(
                                    red = 0x21 / 255.0, green = 0x96 / 255.0,
                                    blue = 0xF3 / 255.0, alpha = 1.0
                                )
                                view.glyphText = "📍"
                                view.canShowCallout = true
                                view
                            }
                            else -> {
                                val id = "vehicle_arrow"
                                val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                                    ?: MKAnnotationView(
                                        annotation = viewForAnnotation,
                                        reuseIdentifier = id
                                    )
                                view.annotation = viewForAnnotation
                                view.image = arrowImage
                                view.canShowCallout = false
                                view.centerOffset = CGPointMake(0.0, 0.0)
                                val bearing = calculateBearing(
                                    lastLocation[0], lastLocation[1],
                                    latitude, longitude
                                )
                                view.transform = CGAffineTransformMakeRotation(bearing * PI / 180.0)
                                view
                            }
                        }
                    }

                    // ── Polyline renderer ───────────────────────────
                    override fun mapView(
                        mapView: MKMapView,
                        rendererForOverlay: MKOverlayProtocol
                    ): MKOverlayRenderer {
                        if (rendererForOverlay is MKPolyline) {
                            val renderer = MKPolylineRenderer(polyline = rendererForOverlay)
                            renderer.strokeColor = UIColor(
                                red = 0x21 / 255.0, green = 0x96 / 255.0,
                                blue = 0xF3 / 255.0, alpha = 1.0
                            )
                            renderer.lineWidth = 5.0
                            return renderer
                        }
                        return MKOverlayRenderer(overlay = rendererForOverlay)
                    }
                }

                mapView
            },
            update = { mapView ->

                // ── 0. map type toggle ─────────────────────────────
                mapView.setMapType(
                    if (isHybrid) MKMapTypeHybrid else MKMapTypeStandard
                )

                val newCoordinate = CLLocationCoordinate2DMake(latitude, longitude)

                // ── 1. bearing + move vehicle marker ────────────────
                val bearing = calculateBearing(
                    lastLocation[0], lastLocation[1],
                    latitude, longitude
                )

                vehicleAnnotation.setCoordinate(newCoordinate)

                mapView.viewForAnnotation(vehicleAnnotation)?.transform =
                    CGAffineTransformMakeRotation(bearing * PI / 180.0)

                // ── 2. camera follow ────────────────────────────────
                mapView.setCenterCoordinate(newCoordinate, animated = true)

                lastLocation[0] = latitude
                lastLocation[1] = longitude

                // ── 3. trail polyline ───────────────────────────────
                if (trailPoints.size >= 2) {
                    polylineRef[0]?.let { mapView.removeOverlay(it) }

                    // CLLocationCoordinate2D is { double latitude; double longitude; } =
                    // two consecutive 8-byte doubles. Write raw bytes through a
                    // DoubleVar array, then reinterpret the pointer — this avoids
                    // the K/N struct-field access that the current bindings reject.
                    val polyline = memScoped {
                        val size = trailPoints.size
                        val buffer = allocArray<DoubleVar>(size * 2)
                        trailPoints.forEachIndexed { i, p ->
                            buffer[i * 2] = p.latitude
                            buffer[i * 2 + 1] = p.longitude
                        }
                        MKPolyline.polylineWithCoordinates(
                            buffer.reinterpret<CLLocationCoordinate2D>(),
                            size.toULong()
                        )
                    }
                    polylineRef[0] = polyline
                    mapView.addOverlay(polyline)

                    // ── 4. start marker ─────────────────────────────
                    val startPoint = trailPoints.first()
                    val startCoord = CLLocationCoordinate2DMake(
                        startPoint.latitude,
                        startPoint.longitude
                    )
                    if (!startAnnotationAdded) {
                        startAnnotation.setTitle("Start")
                        startAnnotation.setCoordinate(startCoord)
                        mapView.addAnnotation(startAnnotation)
                        startAnnotationAdded = true
                    } else {
                        startAnnotation.setCoordinate(startCoord)
                    }
                } else {
                    // trail too short — clear any leftovers
                    polylineRef[0]?.let {
                        mapView.removeOverlay(it)
                        polylineRef[0] = null
                    }
                    if (startAnnotationAdded) {
                        mapView.removeAnnotation(startAnnotation)
                        startAnnotationAdded = false
                    }
                }
            }
        )

        // ── Right-side map control buttons ──────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // User → Device route button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (routeActive) Color(0xFFE3F2FD) else Color.White)
                    .border(
                        1.dp,
                        if (routeActive) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        val mv = mapViewRef[0] ?: return@clickable
                        if (routeActive) {
                            userToDevicePolylineRef[0]?.let { mv.removeOverlay(it) }
                            userToDevicePolylineRef[0] = null
                            if (userAnnotationAdded) {
                                mv.removeAnnotation(userAnnotation)
                                userAnnotationAdded = false
                            }
                            routeActive = false
                        } else {
                            val userLocation = mv.userLocation.location ?: return@clickable
                            val userLat = userLocation.coordinate.useContents { latitude }
                            val userLng = userLocation.coordinate.useContents { longitude }

                            // User annotation
                            userAnnotation.setTitle("You")
                            userAnnotation.setCoordinate(
                                CLLocationCoordinate2DMake(userLat, userLng)
                            )
                            if (!userAnnotationAdded) {
                                mv.addAnnotation(userAnnotation)
                                userAnnotationAdded = true
                            }

                            // Route polyline
                            userToDevicePolylineRef[0]?.let { mv.removeOverlay(it) }
                            val polyline = memScoped {
                                val buf = allocArray<DoubleVar>(4)
                                buf[0] = userLat; buf[1] = userLng
                                buf[2] = latitude; buf[3] = longitude
                                MKPolyline.polylineWithCoordinates(
                                    buf.reinterpret<CLLocationCoordinate2D>(), 2.toULong()
                                )
                            }
                            userToDevicePolylineRef[0] = polyline
                            mv.addOverlay(polyline)

                            // Fit camera to show both points
                            val minLat = min(userLat, latitude)
                            val maxLat = max(userLat, latitude)
                            val minLng = min(userLng, longitude)
                            val maxLng = max(userLng, longitude)
                            val spanLat = (maxLat - minLat) * 1.5 + 0.01
                            val spanLng = (maxLng - minLng) * 1.5 + 0.01
                            val center = CLLocationCoordinate2DMake(
                                (minLat + maxLat) / 2, (minLng + maxLng) / 2
                            )
                            mv.setRegion(
                                MKCoordinateRegionMake(
                                    center, MKCoordinateSpanMake(spanLat, spanLng)
                                ),
                                animated = true
                            )
                            routeActive = true
                        }
                    }
                    .padding(8.dp)
            ) {
                Text(text = "📍", fontSize = 18.sp)
            }

            // Satellite / Normal toggle
            Box(
                modifier = Modifier
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
}

// ─────────────────────────────────────────────────────────────────
// Draws a blue circle with a white upward-pointing chevron inside,
// so iOS has a visible navigation arrow without needing an asset
// added to Assets.xcassets.
// ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalForeignApi::class)
private fun buildNavigationArrowImage(): UIImage? {
    val size = CGSizeMake(42.0, 42.0)
    UIGraphicsBeginImageContextWithOptions(size, false, 0.0)

    val blue = UIColor(
        red = 0x21 / 255.0, green = 0x96 / 255.0,
        blue = 0xF3 / 255.0, alpha = 1.0
    )
    val white = UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 1.0)

    // Blue circle background
    blue.setFill()
    UIBezierPath.bezierPathWithOvalInRect(CGRectMake(1.0, 1.0, 40.0, 40.0)).fill()

    // White arrow chevron pointing up (north = bearing 0)
    white.setFill()
    val arrow = UIBezierPath()
    arrow.moveToPoint(CGPointMake(21.0, 8.0))
    arrow.addLineToPoint(CGPointMake(32.0, 32.0))
    arrow.addLineToPoint(CGPointMake(21.0, 26.0))
    arrow.addLineToPoint(CGPointMake(10.0, 32.0))
    arrow.closePath()
    arrow.fill()

    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return image
}

// ─────────────────────────────────────────────────────────────────
// Bearing calculator — same formula as Android
// ─────────────────────────────────────────────────────────────────
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
    val x = cos(startLat) * sin(endLat) -
            sin(startLat) * cos(endLat) * cos(dLng)

    return (atan2(y, x) * 180 / PI + 360) % 360
}