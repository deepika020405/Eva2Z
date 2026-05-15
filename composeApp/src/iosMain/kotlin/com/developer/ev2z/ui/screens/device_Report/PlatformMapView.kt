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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
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
    trailPoints: List<TrailPoint>   // not rendered — kept for API compatibility
) {
    val vehicleAnnotation  = remember { MKPointAnnotation() }
    val userAnnotation     = remember { MKPointAnnotation() }
    val routePolylineRef   = remember { arrayOfNulls<MKPolyline>(1) }
    val mapViewRef         = remember { arrayOfNulls<MKMapView>(1) }
    val delegateHolder     = remember { arrayOfNulls<Any>(1) }
    val locationManager    = remember { CLLocationManager() }
    val lastLocation       = remember { doubleArrayOf(latitude, longitude) }
    val currentLocation    = remember { doubleArrayOf(latitude, longitude) }
    val routeActiveHolder  = remember { booleanArrayOf(false) }
    val arrowImage         = remember { buildNavigationArrowImage() }
    var isHybrid           by remember { mutableStateOf(false) }
    var routeActive        by remember { mutableStateOf(false) }
    var userAnnotationAdded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {

        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val mapView = MKMapView()
                mapViewRef[0] = mapView

                locationManager.requestWhenInUseAuthorization()
                mapView.showsUserLocation = true   // shows blue dot + enables didUpdateUserLocation

                val coord = CLLocationCoordinate2DMake(latitude, longitude)
                vehicleAnnotation.setCoordinate(coord)
                vehicleAnnotation.setTitle("Vehicle")
                mapView.addAnnotation(vehicleAnnotation)
                mapView.setRegion(
                    MKCoordinateRegionMakeWithDistance(coord, zoom * 1000, zoom * 1000),
                    animated = false
                )

                val delegate = object : NSObject(), MKMapViewDelegateProtocol {

                    // ── Vehicle arrow annotation view ───────────────
                    override fun mapView(
                        mapView: MKMapView,
                        viewForAnnotation: MKAnnotationProtocol
                    ): MKAnnotationView? {
                        if (viewForAnnotation is MKUserLocation) return null   // use default blue dot
                        val id = "vehicle_arrow"
                        val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                            ?: MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = id)
                        view.annotation   = viewForAnnotation
                        view.image        = arrowImage
                        view.canShowCallout = false
                        view.centerOffset = CGPointMake(0.0, 0.0)
                        val bearing = calculateBearing(
                            lastLocation[0], lastLocation[1],
                            currentLocation[0], currentLocation[1]
                        )
                        view.transform = CGAffineTransformMakeRotation(bearing * PI / 180.0)
                        return view
                    }

                    // ── Blue polyline renderer ──────────────────────
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

                    // ── Real-time user location updates ─────────────
                    // Called by MapKit whenever the blue dot moves.
                    override fun mapView(
                        mapView: MKMapView,
                        didUpdateUserLocation: MKUserLocation
                    ) {
                        if (!routeActiveHolder[0]) return
                        val loc = didUpdateUserLocation.location ?: return
                        val uLat = loc.coordinate.useContents { latitude }
                        val uLng = loc.coordinate.useContents { longitude }
                        drawRouteLine(mapView, uLat, uLng, currentLocation[0], currentLocation[1], routePolylineRef)
                    }
                }

                delegateHolder[0] = delegate
                mapView.delegate  = delegate
                mapView
            },
            update = { mapView ->

                // Guard against weak-ref GC race
                (delegateHolder[0] as? MKMapViewDelegateProtocol)?.let {
                    if (mapView.delegate !== it) mapView.delegate = it
                }

                mapView.setMapType(if (isHybrid) MKMapTypeHybrid else MKMapTypeStandard)

                val newCoord = CLLocationCoordinate2DMake(latitude, longitude)

                // Vehicle marker bearing + position
                val bearing = calculateBearing(lastLocation[0], lastLocation[1], latitude, longitude)
                vehicleAnnotation.setCoordinate(newCoord)
                mapView.viewForAnnotation(vehicleAnnotation)?.transform =
                    CGAffineTransformMakeRotation(bearing * PI / 180.0)

                // Camera follows device only when route is NOT active
                if (!routeActive) {
                    mapView.setCenterCoordinate(newCoord, animated = true)
                }

                lastLocation[0]      = latitude
                lastLocation[1]      = longitude
                currentLocation[0]   = latitude
                currentLocation[1]   = longitude
                routeActiveHolder[0] = routeActive

                // Device moved → refresh route line if active
                if (routeActive) {
                    val userLoc = mapView.userLocation.location
                    if (userLoc != null) {
                        val uLat = userLoc.coordinate.useContents { latitude }
                        val uLng = userLoc.coordinate.useContents { longitude }
                        drawRouteLine(mapView, uLat, uLng, latitude, longitude, routePolylineRef)
                    }
                }
            }
        )

        // ── Buttons ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // 📍 User → Device route toggle
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
                            // Hide route
                            routePolylineRef[0]?.let { mv.removeOverlay(it) }
                            routePolylineRef[0]  = null
                            routeActiveHolder[0] = false
                            routeActive          = false
                            // Resume following device
                            mv.setCenterCoordinate(
                                CLLocationCoordinate2DMake(currentLocation[0], currentLocation[1]),
                                animated = true
                            )
                        } else {
                            // Show route
                            val userLoc = mv.userLocation.location ?: return@clickable
                            val uLat = userLoc.coordinate.useContents { latitude }
                            val uLng = userLoc.coordinate.useContents { longitude }
                            drawRouteLine(mv, uLat, uLng, latitude, longitude, routePolylineRef)
                            // Zoom to fit both user and device
                            val minLat = min(uLat, latitude); val maxLat = max(uLat, latitude)
                            val minLng = min(uLng, longitude); val maxLng = max(uLng, longitude)
                            mv.setRegion(
                                MKCoordinateRegionMake(
                                    CLLocationCoordinate2DMake((minLat + maxLat) / 2, (minLng + maxLng) / 2),
                                    MKCoordinateSpanMake((maxLat - minLat) * 1.5 + 0.01, (maxLng - minLng) * 1.5 + 0.01)
                                ),
                                animated = true
                            )
                            routeActiveHolder[0] = true
                            routeActive          = true
                        }
                    }
                    .padding(8.dp)
            ) {
                Text(text = "📍", fontSize = 18.sp)
            }

            // 🛰️ Satellite toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .clickable { isHybrid = !isHybrid }
                    .padding(8.dp)
            ) {
                Text(text = if (!isHybrid) "🛰️" else "🗺️", fontSize = 18.sp)
            }
        }
    }
}

// Draw/refresh the blue user→device polyline
@OptIn(ExperimentalForeignApi::class)
private fun drawRouteLine(
    mapView: MKMapView,
    userLat: Double, userLng: Double,
    deviceLat: Double, deviceLng: Double,
    ref: Array<MKPolyline?>
) {
    ref[0]?.let { mapView.removeOverlay(it) }
    val coords = doubleArrayOf(userLat, userLng, deviceLat, deviceLng)
    val polyline = coords.usePinned { pinned ->
        MKPolyline.polylineWithCoordinates(
            pinned.addressOf(0).reinterpret<CLLocationCoordinate2D>(),
            2.toULong()
        )
    }
    ref[0] = polyline
    mapView.addOverlay(polyline, level = MKOverlayLevelAboveLabels)
}

@OptIn(ExperimentalForeignApi::class)
private fun buildNavigationArrowImage(): UIImage? {
    val size = CGSizeMake(42.0, 42.0)
    UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
    val blue  = UIColor(red = 0x21 / 255.0, green = 0x96 / 255.0, blue = 0xF3 / 255.0, alpha = 1.0)
    val white = UIColor(red = 1.0, green = 1.0, blue = 1.0, alpha = 1.0)
    blue.setFill()
    UIBezierPath.bezierPathWithOvalInRect(CGRectMake(1.0, 1.0, 40.0, 40.0)).fill()
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

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLng = (lon2 - lon1) * PI / 180
    val y = sin(dLng) * cos(lat2 * PI / 180)
    val x = cos(lat1 * PI / 180) * sin(lat2 * PI / 180) -
            sin(lat1 * PI / 180) * cos(lat2 * PI / 180) * cos(dLng)
    return (atan2(y, x) * 180 / PI + 360) % 360
}