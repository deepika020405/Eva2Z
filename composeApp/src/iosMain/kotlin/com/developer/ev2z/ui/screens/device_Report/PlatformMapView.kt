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
import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.ui.components.VEHICLE_MARKER_POINTS
import com.developer.ev2z.ui.components.UserFacingCone
import com.developer.ev2z.ui.components.followWithDeadzone
import com.developer.ev2z.ui.components.uiColorOf
import com.developer.ev2z.ui.components.userFacingImage
import com.developer.ev2z.util.rememberDeviceFacing
import com.developer.ev2z.ui.components.vehicleArrowImage
import com.developer.ev2z.ui.components.vehicleTopViewFor
import com.developer.ev2z.ui.components.vehicleMarkerImage
import com.developer.ev2z.ui.components.vehicleScreenAngleRadians
import com.developer.ev2z.ui.components.vehicleViewTransform
import com.developer.ev2z.ui.screens.trackTrace.TrailPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.CoreLocation.CLLocationManager
import platform.Foundation.NSString
import platform.Foundation.create
import platform.MapKit.*
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIBezierPath
import platform.UIKit.UIFont
import platform.UIKit.drawAtPoint
import platform.UIKit.drawInRect
import platform.UIKit.sizeWithAttributes
import platform.UIKit.UIRectFillUsingBlendMode
import platform.CoreGraphics.CGBlendMode
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
    trailPoints: List<TrailPoint>,  // not rendered — kept for API compatibility
    statusKind: String,
    vehicleSlug: String?,
    addressLabel: String?,
    headingDegrees: Double
) {
    val vehicle = vehicleTopViewFor(vehicleSlug)
    // The vehicle turns to the heading as well as the arrow does — it used to be
    // arrow-only, on the grounds that a side-on glyph rotated to a heading reads as a
    // crash, which was true of the side-profile emoji and is not true of the artwork
    // drawn now: seen from above it is right at every angle. Same rule as Android: the
    // marker has to agree with the map it is drawn on.
    val rotateWithHeading = true
    val vehicleAnnotation  = remember { MKPointAnnotation() }
    // The address chip is its own annotation rather than text baked into the vehicle
    // image: the vehicle view is rotated to the heading, and a label inside it would
    // turn upside down every time the vehicle drove south.
    val labelAnnotation    = remember { MKPointAnnotation() }
    val userAnnotation     = remember { MKPointAnnotation() }
    val routePolylineRef   = remember { arrayOfNulls<MKPolyline>(1) }
    val mapViewRef         = remember { arrayOfNulls<MKMapView>(1) }
    val delegateHolder     = remember { arrayOfNulls<Any>(1) }
    val locationManager    = remember { CLLocationManager() }
    // Last point the arrow's heading was computed from, plus that heading. Both only
    // advance once the vehicle has really travelled BEARING_MIN_MOVE_METERS.
    val lastLocation       = remember { doubleArrayOf(latitude, longitude) }
    val lastBearing        = remember { doubleArrayOf(0.0) }
    // The heading actually drawn, which eases towards lastBearing over several frames.
    // NaN until the first real heading exists, so the arrow starts pointing the right
    // way instead of easing up from north.
    val drawnBearing       = remember { doubleArrayOf(Double.NaN) }
    val currentLocation    = remember { doubleArrayOf(latitude, longitude) }
    val routeActiveHolder  = remember { booleanArrayOf(false) }
    // Redrawn only when the status changes; this block re-runs on every interpolated
    // frame. Held in a ref as well so the annotation-view delegate — which outlives
    // any one composition — always reads the current one.
    val arrowImage         = remember(statusKind, vehicle) {
        // The body colour is the status colour, so the marker, the badge and the sheet
        // all agree: green running, yellow idling, red stopped or parked.
        val color = statusColorOf(statusKind)
        if (vehicle == null) vehicleArrowImage(color)
        else vehicleMarkerImage(vehicle, color)
    }
    val arrowImageRef      = remember { arrayOfNulls<UIImage>(1) }
    // Same reasoning: rasterised once per address, not once per frame. The address
    // only changes when the vehicle leaves an ~11 m bucket.
    val labelImage         = remember(addressLabel, statusKind) {
        addressLabel
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { buildAddressLabelImage(it, statusColorOf(statusKind)) }
    }
    val labelImageRef      = remember { arrayOfNulls<UIImage>(1) }
    val appliedLabel       = remember { arrayOfNulls<UIImage>(1) }
    val labelAdded         = remember { booleanArrayOf(false) }
    SideEffect { labelImageRef[0] = labelImage }
    // The image currently on the live annotation view, so the frame loop can tell a
    // real status change from the frames that follow it.
    val appliedArrow       = remember { arrayOfNulls<UIImage>(1) }
    SideEffect { arrowImageRef[0] = arrowImage }
    // The screen angle currently on the annotation view. Not NaN-initialised: every
    // comparison against NaN is false, so the first frame would never apply.
    val appliedAngle       = remember { doubleArrayOf(-999.0) }

    // Where the phone is and which way its holder is facing, for the compass cone —
    // which is about the person, not the vehicle; the two are usually kilometres
    // apart. Null with location permission refused, on a device with no magnetometer,
    // and before the first reading.
    val facing             = rememberDeviceFacing()
    val cone               = remember { UserFacingCone(userFacingImage()) }

    /**
     * The angle to draw the vehicle at on screen.
     *
     * A MapKit annotation view is screen-space — unlike a flat Google Maps marker it
     * does not turn with the map — so the map's own rotation has to be taken out of
     * the compass heading. Without this the icon pointed somewhere else entirely the
     * moment the user turned the map with two fingers. The heading itself needs no
     * correcting: the artwork is drawn from above and facing up, see VehicleTopView.
     */
    fun screenAngle(mapView: MKMapView): Double {
        if (!rotateWithHeading) return 0.0
        val heading = drawnBearing[0].let { if (it.isNaN()) lastBearing[0] else it }
        return vehicleScreenAngleRadians(heading, mapView.camera.heading)
    }
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
                        // Blanked rather than left to MapKit: its user-location view
                        // draws a large translucent accuracy circle with its dot, and
                        // the two cannot be separated. showsUserLocation has to stay
                        // on — the route-to-vehicle button reads the position off it —
                        // so the view is kept and given nothing to draw. The dot comes
                        // from the compass cone instead.
                        if (viewForAnnotation is MKUserLocation) {
                            val id = "user_location_blank"
                            val blank = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                                ?: MKAnnotationView(
                                    annotation = viewForAnnotation,
                                    reuseIdentifier = id
                                )
                            blank.annotation = viewForAnnotation
                            blank.image = null
                            blank.canShowCallout = false
                            blank.enabled = false
                            return blank
                        }

                        cone.annotationView(mapView, viewForAnnotation)?.let { return it }

                        // The address chip: no rotation, and offset upwards so it
                        // floats above the glyph instead of sitting on it.
                        if (viewForAnnotation === labelAnnotation) {
                            val labelId = "vehicle_address_label"
                            val labelView = mapView.dequeueReusableAnnotationViewWithIdentifier(labelId)
                                ?: MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = labelId)
                            labelView.annotation = viewForAnnotation
                            labelView.image = labelImageRef[0]
                            labelView.canShowCallout = false
                            val chipHeight = labelImageRef[0]?.size?.useContents { height } ?: 0.0
                            labelView.centerOffset =
                                CGPointMake(0.0, -(chipHeight / 2.0 + LABEL_LIFT_POINTS))
                            return labelView
                        }

                        val id = "vehicle_arrow"
                        val view = mapView.dequeueReusableAnnotationViewWithIdentifier(id)
                            ?: MKAnnotationView(annotation = viewForAnnotation, reuseIdentifier = id)
                        view.annotation   = viewForAnnotation
                        view.image        = arrowImageRef[0]
                        view.canShowCallout = false
                        view.centerOffset = CGPointMake(0.0, 0.0)
                        // The heading already on screen, not one recomputed from the
                        // last two points: recomputing here handed a recycled view a
                        // different angle from the eased one, so the arrow flicked
                        // whenever MapKit rebuilt it mid-turn.
                        val angle = screenAngle(mapView)
                        view.transform = vehicleViewTransform(angle)
                        appliedAngle[0] = angle
                        return view
                    }

                    // ── Map rotated / panned ────────────────────────
                    // The vehicle view is screen-space, so turning the map does not
                    // turn it: without this the icon kept pointing at the old north
                    // until the vehicle's next fix moved it.
                    override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                        cone.reapplyRotation(mapView)
                        val angle = screenAngle(mapView)
                        if (abs(angle - appliedAngle[0]) < 0.001) return
                        mapView.viewForAnnotation(vehicleAnnotation)?.transform =
                            vehicleViewTransform(angle)
                        appliedAngle[0] = angle
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

                cone.update(mapView, facing)

                val newCoord = CLLocationCoordinate2DMake(latitude, longitude)

                // latitude/longitude are interpolated frames between two real fixes
                // (see rememberAnimatedLatLng), so this block runs ~60 times a second.
                val moved = distanceMeters(lastLocation[0], lastLocation[1], latitude, longitude)

                // Vehicle marker position every frame; heading only once the vehicle has
                // really moved — a sub-metre step gives an essentially random angle,
                // which made the arrow spin on the spot.
                vehicleAnnotation.setCoordinate(newCoord)

                // A status change has to reach the view that is already on screen:
                // MapKit asks the delegate for an image only when it builds or
                // recycles a view, so an arrow placed while the vehicle was running
                // would stay green after it stopped.
                if (appliedArrow[0] !== arrowImage) {
                    mapView.viewForAnnotation(vehicleAnnotation)?.image = arrowImage
                    appliedArrow[0] = arrowImage
                }

                // ── Address chip ─────────────────────────────────────
                // Follows the vehicle every frame; the annotation is added and removed
                // as the address appears and goes away, and its image is only pushed
                // when the address itself changed.
                labelAnnotation.setCoordinate(newCoord)
                if (labelImage == null) {
                    if (labelAdded[0]) {
                        mapView.removeAnnotation(labelAnnotation)
                        labelAdded[0] = false
                        appliedLabel[0] = null
                    }
                } else {
                    if (!labelAdded[0]) {
                        mapView.addAnnotation(labelAnnotation)
                        labelAdded[0] = true
                        appliedLabel[0] = labelImage
                    } else if (appliedLabel[0] !== labelImage) {
                        // MapKit asks the delegate for an image only when it builds or
                        // recycles a view, so a chip already on screen has to be
                        // updated directly or it keeps the old street.
                        mapView.viewForAnnotation(labelAnnotation)?.let { view ->
                            view.image = labelImage
                            val chipHeight = labelImage.size.useContents { height }
                            view.centerOffset =
                                CGPointMake(0.0, -(chipHeight / 2.0 + LABEL_LIFT_POINTS))
                        }
                        appliedLabel[0] = labelImage
                    }
                }

                // A heading handed in beats one measured here: the caller walking the
                // marker along a snapped road knows where the road goes next, and this
                // can only see where the coordinates have been. Without a road to
                // follow, the baseline is the live equivalent of the look-ahead — a
                // heading measured over a few metres is mostly GPS wander.
                if (!headingDegrees.isNaN()) {
                    lastBearing[0] = headingDegrees
                    lastLocation[0] = latitude
                    lastLocation[1] = longitude
                } else if (moved >= HEADING_BASELINE_METERS) {
                    lastBearing[0] = calculateBearing(
                        lastLocation[0], lastLocation[1], latitude, longitude
                    )
                    lastLocation[0] = latitude
                    lastLocation[1] = longitude
                }

                // Rotation is eased towards the heading over several frames rather than
                // applied in the one frame the heading changed, which snapped the arrow
                // through the whole turn at every corner. Only touched when the angle
                // actually moves, so a parked vehicle on a still map costs no
                // annotation-view lookups.
                drawnBearing[0] = stepBearing(drawnBearing[0], lastBearing[0])
                val angle = screenAngle(mapView)
                if (abs(angle - appliedAngle[0]) >= 0.001) {
                    mapView.viewForAnnotation(vehicleAnnotation)?.transform =
                        vehicleViewTransform(angle)
                    appliedAngle[0] = angle
                }

                // Camera follows the vehicle only when the route line is NOT active,
                // and then only once the marker nears an edge.
                //
                // Re-centring on every frame — which is what this used to do — pins
                // the marker to the centre pixel and slides the map underneath it: the
                // vehicle is travelling the whole time but never moves on screen,
                // which reads as a marker that does not move at all. Same fix as
                // playback, same helper.
                if (!routeActive) {
                    mapView.followWithDeadzone(latitude, longitude)
                }

                currentLocation[0]   = latitude
                currentLocation[1]   = longitude
                routeActiveHolder[0] = routeActive

                // Device moved → refresh route line if active. MKPolyline is immutable,
                // so this replaces an overlay; only do it on real movement, not per frame.
                if (routeActive && moved >= BEARING_MIN_MOVE_METERS) {
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

/**
 * How far the vehicle has to travel before its heading is measured again.
 *
 * The live equivalent of the look-ahead the playback marker uses: a heading measured
 * over a few metres is mostly GPS wander, and drawing it twitches the marker left and
 * right the whole way down a straight road. Twenty metres is about a car's length four
 * times over — short enough to catch a turn as it happens, long enough that the angle
 * is the road's and not the receiver's.
 */
private const val HEADING_BASELINE_METERS = 20.0

/**
 * Travel below this is interpolation noise rather than movement, so the route line is
 * left alone.
 */
private const val BEARING_MIN_MOVE_METERS = 3.0

/**
 * Fraction of the remaining turn applied per frame. At ~60 fps a turn settles in
 * roughly a fifth of a second: fast enough to agree with where the vehicle is going,
 * slow enough to read as a turn rather than a snap.
 */
private const val BEARING_EASE = 0.18

/**
 * Eases [drawn] towards [target] along the shorter arc.
 *
 * Interpolating the raw numbers is not enough: 350° → 10° is a 20° turn to the right,
 * but the plain difference is -340°, which spins the arrow almost all the way round the
 * wrong way. Wrapping the delta into ±180° picks the direction the vehicle actually
 * turned.
 */
private fun stepBearing(drawn: Double, target: Double): Double {
    if (drawn.isNaN()) return target
    val delta = (target - drawn + 540.0) % 360.0 - 180.0
    if (abs(delta) < 0.5) return target
    return (drawn + delta * BEARING_EASE + 360.0) % 360.0
}

/** Flat-earth distance — accurate enough for a few-metre threshold, and cheap. */
private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val meanLat = (lat1 + lat2) / 2 * PI / 180
    val dLat = (lat2 - lat1) * 111_320.0
    val dLng = (lon2 - lon1) * 111_320.0 * cos(meanLat)
    return sqrt(dLat * dLat + dLng * dLng)
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


// ─────────────────────────────────────────────────────────────────────────────
//  Address chip
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Clearance between the chip's tail and the glyph it points at, in points — half the
 * marker, which is how far its top edge reaches from the coordinate, plus a little air.
 */
/** Shared with the fleet map, which lifts the same chip off its selected vehicle. */
internal const val LABEL_LIFT_POINTS = VEHICLE_MARKER_POINTS / 2.0 + 3.0

private const val LABEL_MAX_TEXT_POINTS = 140.0
private const val LABEL_PADDING_X = 5.5
private const val LABEL_PADDING_Y = 3.5
private const val LABEL_DOT_RADIUS = 2.25
private const val LABEL_TAIL = 3.2

/**
 * A white chip holding [text], with a tail pointing down at the vehicle and a dot in
 * the status colour out of [statusArgb].
 *
 * One line, ellipsised past [LABEL_MAX_TEXT_POINTS] — a full postal address is wide
 * enough to span the screen, and a chip that wide stops being readable and starts
 * covering the road the vehicle is on. Android wraps to two lines; MapKit has no
 * cheap way to measure a wrapped run, so this truncates instead.
 */
@OptIn(ExperimentalForeignApi::class)
/**
 * Shared with the fleet map — see `homeScreen/PlatformMapView.ios.kt`. One builder so
 * the two maps cannot drift into two different-looking chips for the same address.
 */
internal fun buildAddressLabelImage(text: String, statusArgb: Long): UIImage? {
    val font = UIFont.boldSystemFontOfSize(7.5)
    // Typed as Map<Any?, *>: the UIKit binding takes that, and an inferred
    // Map<String?, UIFont> is not it.
    val attributes: Map<Any?, *> = mapOf<Any?, Any?>(
        NSFontAttributeName to font,
        NSForegroundColorAttributeName to UIColor(
            red = 0.106, green = 0.106, blue = 0.106, alpha = 1.0
        )
    )
    val measureAttributes: Map<Any?, *> = mapOf<Any?, Any?>(NSFontAttributeName to font)

    var shown = text
    while (shown.length > 6 &&
        measureWidth("$shown…", measureAttributes) > LABEL_MAX_TEXT_POINTS
    ) {
        shown = shown.dropLast(2).trimEnd()
    }
    if (shown.length < text.length) shown = "$shown…"

    val label = NSString.create(string = shown)
    val labelSize = label.sizeWithAttributes(measureAttributes)
    val textWidth = labelSize.useContents { width }
    val textHeight = labelSize.useContents { height }

    val dotColumn = LABEL_DOT_RADIUS * 2 + 3.0
    val chipWidth = dotColumn + textWidth + LABEL_PADDING_X * 2
    val chipHeight = textHeight + LABEL_PADDING_Y * 2

    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(chipWidth, chipHeight + LABEL_TAIL), false, 0.0
    )

    UIColor.whiteColor.setFill()
    UIBezierPath.bezierPathWithRoundedRect(
        CGRectMake(0.0, 0.0, chipWidth, chipHeight), cornerRadius = 4.5
    ).fill()

    // Tail, drawn off the bottom edge so the chip keeps its crisp corner radius.
    val tail = UIBezierPath()
    tail.moveToPoint(CGPointMake(chipWidth / 2.0 - LABEL_TAIL, chipHeight - 0.5))
    tail.addLineToPoint(CGPointMake(chipWidth / 2.0 + LABEL_TAIL, chipHeight - 0.5))
    tail.addLineToPoint(CGPointMake(chipWidth / 2.0, chipHeight + LABEL_TAIL))
    tail.closePath()
    tail.fill()

    // A hairline border: a plain white chip disappears into a white building footprint,
    // and this map has a satellite mode where that is most of the frame.
    val border = UIBezierPath.bezierPathWithRoundedRect(
        CGRectMake(0.25, 0.25, chipWidth - 0.5, chipHeight - 0.5), cornerRadius = 4.5
    )
    UIColor(red = 0.0, green = 0.0, blue = 0.0, alpha = 0.2).setStroke()
    border.setLineWidth(0.5)
    border.stroke()

    uiColorOf(statusArgb).setFill()
    UIBezierPath.bezierPathWithOvalInRect(
        CGRectMake(
            LABEL_PADDING_X,
            chipHeight / 2.0 - LABEL_DOT_RADIUS,
            LABEL_DOT_RADIUS * 2,
            LABEL_DOT_RADIUS * 2
        )
    ).fill()

    label.drawAtPoint(
        CGPointMake(LABEL_PADDING_X + dotColumn, LABEL_PADDING_Y),
        withAttributes = attributes
    )

    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return image
}

@OptIn(ExperimentalForeignApi::class)
private fun measureWidth(text: String, attributes: Map<Any?, *>): Double =
    NSString.create(string = text).sizeWithAttributes(attributes).useContents { width }

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLng = (lon2 - lon1) * PI / 180
    val y = sin(dLng) * cos(lat2 * PI / 180)
    val x = cos(lat1 * PI / 180) * sin(lat2 * PI / 180) -
            sin(lat1 * PI / 180) * cos(lat2 * PI / 180) * cos(dLng)
    return (atan2(y, x) * 180 / PI + 360) % 360
}