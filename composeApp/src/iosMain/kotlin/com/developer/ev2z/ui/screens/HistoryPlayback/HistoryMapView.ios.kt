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
import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.network.SnappedRoute
import com.developer.ev2z.response.RoutePoint
import com.developer.ev2z.ui.components.UserFacingCone
import com.developer.ev2z.ui.components.followWithDeadzone
import com.developer.ev2z.ui.components.userFacingImage
import com.developer.ev2z.util.rememberDeviceFacing
import com.developer.ev2z.ui.components.vehicleArrowImage
import com.developer.ev2z.ui.components.vehicleTopViewFor
import com.developer.ev2z.ui.components.vehicleMarkerImage
import com.developer.ev2z.ui.components.vehicleScreenAngleRadians
import com.developer.ev2z.ui.components.vehicleViewTransform
import kotlinx.cinterop.*
import kotlin.math.PI
import kotlin.math.abs
import platform.CoreGraphics.CGPointMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIImage
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HistoryMapView(
    routePoints: List<RoutePoint>,
    snappedRoute: SnappedRoute,
    currentIndex: Int,
    modifier: Modifier,
    vehicleSlug: String?
) {
    if (routePoints.isEmpty()) return

    var isHybrid by remember { mutableStateOf(false) }

    // Where the phone is and which way its holder is facing, for the compass cone.
    // Worth having on playback too: it answers "was it anywhere near me" without
    // reading coordinates off the screen.
    val facing = rememberDeviceFacing()
    val cone = remember { UserFacingCone(userFacingImage()) }

    // Updated every animation frame; read inside `update` below, which re-runs with it.
    val smoothPosition = rememberSmoothPlaybackPosition(routePoints, currentIndex, snappedRoute)

    // The one pin, kept across frames so it can be moved instead of rebuilt.
    val markerAnnotation = remember { arrayOfNulls<MKPointAnnotation>(1) }

    // The vehicle the history response named, drawn exactly as the live map draws it —
    // playback used to show MapKit's default pin here, so the same vehicle looked like
    // two different things depending on which screen you were on.
    val vehicle = vehicleTopViewFor(vehicleSlug)

    // The colour comes from the fix being played back, so the marker reports what the
    // vehicle was doing at that moment rather than what it is doing now. It changes as
    // playback runs through the day: green over the stretches it was moving, red over
    // the ones where it had stopped.
    val statusKind = routePoints.getOrNull(currentIndex)?.statusKind() ?: "unknown"

    // Rasterised once per vehicle/state, not once per frame — the heading never changes
    // it, since the artwork is drawn from above and the view is simply turned. Held in
    // a ref as well, because the annotation-view delegate below outlives any one
    // composition.
    val markerImage = remember(vehicle, statusKind) {
        val color = statusColorOf(statusKind)
        if (vehicle == null) vehicleArrowImage(color)
        else vehicleMarkerImage(vehicle, color)
    }
    val markerImageRef = remember { arrayOfNulls<UIImage>(1) }
    SideEffect { markerImageRef[0] = markerImage }
    // The image currently on the live annotation view, so the frame loop can tell a
    // real change from the frames that follow it.
    val appliedImage = remember { arrayOfNulls<UIImage>(1) }

    // The heading the glide is on, and the screen angle currently applied. Held in refs
    // because the delegate — which MapKit calls when it rebuilds a view — outlives the
    // composition that computed them.
    val bearingRef = remember { doubleArrayOf(Double.NaN) }
    // Not NaN-initialised: every comparison against NaN is false, so the first frame
    // would never apply.
    val appliedAngle = remember { doubleArrayOf(-999.0) }

    /**
     * The angle to draw the vehicle at on screen.
     *
     * A MapKit annotation view is screen-space — unlike a flat Google Maps marker it
     * does not turn with the map — so the map's own rotation has to come out of the
     * compass heading, or the vehicle points somewhere else entirely the moment the
     * user turns the map. The heading itself needs no correcting: the artwork is drawn
     * from above and facing up, see VehicleTopView.
     */
    fun screenAngle(mapView: MKMapView): Double =
        vehicleScreenAngleRadians(bearingRef[0], mapView.camera.heading)

    // Track which route we've drawn the polyline for. Keyed on the snapped route too,
    // so the straight line drawn while snapping was in flight is replaced by the road
    // geometry when it lands.
    var drawnRouteId by remember { mutableStateOf(0) }
    val currentRouteId = remember(routePoints, snappedRoute) { drawnRouteId + 1 }

    // Create delegate inline and hold strong reference
    val mapDelegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {

            // The vehicle image in place of the default pin. Without this override
            // MapKit draws its own red balloon, which is what playback used to show.
            override fun mapView(
                mapView: MKMapView,
                viewForAnnotation: MKAnnotationProtocol
            ): MKAnnotationView? {
                if (viewForAnnotation is MKUserLocation) return null
                cone.annotationView(mapView, viewForAnnotation)?.let { return it }
                val id = "history_vehicle"
                val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                    ?: MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = id)
                view.annotation = viewForAnnotation
                view.image = markerImageRef[0]
                // Centred on the coordinate, not hanging above it by a pin's height:
                // the vehicle is at the point, so its middle belongs there.
                view.centerOffset = CGPointMake(0.0, 0.0)
                // The point's timestamp is worth tapping for, so the callout stays.
                view.canShowCallout = true
                val angle = screenAngle(mapView)
                view.transform = vehicleViewTransform(angle)
                appliedAngle[0] = angle
                return view
            }

            // ── Map rotated / panned ────────────────────────────────
            // The vehicle view is screen-space, so turning the map does not turn it:
            // without this the icon keeps pointing at the old north until the glide's
            // next frame moves it.
            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                cone.reapplyRotation(mapView)
                val angle = screenAngle(mapView)
                if (abs(angle - appliedAngle[0]) < 0.001) return
                markerAnnotation[0]?.let { marker ->
                    mapView.viewForAnnotation(marker)?.transform =
                        vehicleViewTransform(angle)
                }
                appliedAngle[0] = angle
            }

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

                // MapKit's own user-location view stays off: it draws a large
                // translucent accuracy circle along with its dot, and the two cannot be
                // separated. The cone carries the dot instead, circle and all gone.
                mapView.setShowsUserLocation(false)
                cone.update(mapView, facing)

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

                    // MapKit asks the delegate for an image only when it builds or
                    // recycles a view, so a marker already on screen has to be updated
                    // directly or it keeps the colour of the fix it was created on.
                    if (appliedImage[0] !== markerImage) {
                        mapView.viewForAnnotation(marker)?.image = markerImage
                        appliedImage[0] = markerImage
                    }

                    // Turn with the road: the vehicle comes round a roundabout facing
                    // the way it is going instead of sliding round it pointing north.
                    // Only touched when the angle actually moves, so a straight run
                    // costs no annotation-view lookups.
                    bearingRef[0] = position.bearingDegrees
                    val angle = screenAngle(mapView)
                    if (abs(angle - appliedAngle[0]) >= 0.001) {
                        mapView.viewForAnnotation(marker)?.transform =
                            vehicleViewTransform(angle)
                        appliedAngle[0] = angle
                    }

                    // Pan only once the marker nears an edge — re-centring every
                    // frame pinned it to the centre pixel and slid the map underneath,
                    // so playback read as a marker that never moved. The helper also
                    // leaves the zoom alone; setRegion with a fixed span used to
                    // re-impose its own on every step.
                    mapView.followWithDeadzone(position.latitude, position.longitude)
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