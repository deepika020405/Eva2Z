package com.developer.ev2z.ui.screens.device_Report

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.ui.components.CAMERA_PAN_MS
import com.developer.ev2z.ui.components.VEHICLE_MARKER_PX
import com.developer.ev2z.ui.components.holdsComfortably
import com.developer.ev2z.ui.components.vehicleArrowIcon
import com.developer.ev2z.ui.components.vehicleTopViewFor
import com.developer.ev2z.ui.components.vehicleMarkerIcon
import com.developer.ev2z.ui.components.userFacingIcon
import com.developer.ev2z.ui.components.markerRotationDegrees
import com.developer.ev2z.util.rememberDeviceFacing
import com.developer.ev2z.ui.screens.trackTrace.TrailPoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

@SuppressLint("MissingPermission")
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
    val context = LocalContext.current

    val vehicle = vehicleTopViewFor(vehicleSlug)

    // Rebuilt only when the vehicle or its status changes — this composable re-runs
    // on every interpolated frame, and rasterising the marker 60 times a second
    // would allocate a bitmap per frame. The heading never rebuilds it: the artwork is
    // drawn from above, so the marker is only turned, see VehicleTopView.
    val vehicleIcon = remember(statusKind, vehicle) {
        // The body colour is the status colour, so the marker, the badge and the dot in
        // the sheet all agree: green running, yellow idling, red stopped or parked.
        val color = statusColorOf(statusKind).toInt()
        if (vehicle == null) vehicleArrowIcon(context, color)
        else vehicleMarkerIcon(context, vehicle, color)
    }

    // Same reasoning as the marker bitmap: keyed on the text, so the chip is drawn
    // once per address rather than once per animation frame. The address only changes
    // when the vehicle leaves an ~11 m bucket (see DEVICE_ADDRESS_PRECISION).
    val labelIcon = remember(addressLabel, statusKind) {
        addressLabel
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { addressLabelIcon(it, statusColorOf(statusKind).toInt()) }
    }
    // The vehicle turns to the heading as well as the arrow does.
    //
    // It used to be arrow-only, on the grounds that a side-on glyph rotated to 137°
    // reads as a crash — which was true of the side-profile emoji and is not true of
    // the artwork drawn now: seen from above it is right at every angle. The marker is
    // also `flat(true)`, stuck to the map surface, so it turns when the user rotates
    // the map whatever this says; holding its own heading at zero made it the one thing
    // on screen agreeing with neither the map under it nor the direction of travel.
    val rotateWithHeading = true
    // The icon currently pushed to the marker, so the frame loop can tell a real
    // colour change from the 59 frames that follow it.
    val appliedIcon = remember { arrayOfNulls<BitmapDescriptor>(1) }

    // Where the phone is and which way its holder is facing, for the compass cone —
    // which is about the person, not the vehicle. Null with location permission
    // refused, on a device with no magnetometer, and before the first reading.
    val facing = rememberDeviceFacing()
    val facingIcon = remember { userFacingIcon(context) }
    val facingHolder = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }

    var isHybrid    by remember { mutableStateOf(false) }
    var routeActive by remember { mutableStateOf(false) }
    /**
     * Navigation mode: the map turns so the road ahead is always up the screen, the
     * way a car's satnav draws it, instead of holding north up.
     *
     * The three things that decide which way the vehicle is drawn only come together
     * here. The marker's heading is the road's (from the walk); the map's bearing is
     * that same heading, so the vehicle sits upright on screen and the *world* turns
     * around it; and the phone's own rotation keeps the compass cone pointing where
     * its holder is looking, which is a different question and stays separate.
     *
     * Off by default — north-up is the right way to read a map you are not driving
     * along — and the whole thing is one button.
     */
    var navigationMode by remember { mutableStateOf(false) }
    // The bearing the map is currently turned to, so it is only pushed when it has
    // really changed; a camera move per frame on an unchanged bearing is wasted work.
    val appliedMapBearing = remember { floatArrayOf(Float.NaN) }
    var hasPerm     by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val mapView        = remember { MapView(context) }
    val markerHolder   = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }
    // The address chip rides on its own marker rather than being drawn into the
    // vehicle bitmap: the vehicle marker rotates to the heading, and text baked into
    // it would turn upside down every time the vehicle drove south.
    val labelHolder    = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }
    // The chip bitmap currently on that marker, so the frame loop can tell a real
    // address change from the frames that follow it.
    val appliedLabel   = remember { arrayOfNulls<BitmapDescriptor>(1) }
    val routePolyline  = remember { arrayOfNulls<Polyline>(1) }
    // Last point the marker's heading was measured from, and that heading. Both are
    // only updated once the vehicle has actually travelled HEADING_BASELINE_METERS.
    val lastLocation   = remember { doubleArrayOf(latitude, longitude) }
    val lastBearing    = remember { floatArrayOf(0f) }
    // The heading actually drawn, which eases towards lastBearing over several frames.
    // NaN until the first real heading exists, so the arrow starts pointing the right
    // way instead of easing up from north.
    val drawnBearing   = remember { floatArrayOf(Float.NaN) }
    val mapHolder      = remember { arrayOfNulls<com.google.android.gms.maps.GoogleMap>(1) }
    val fusedClient    = remember { LocationServices.getFusedLocationProviderClient(context) }
    // Mutable refs readable by the LocationCallback (main thread)
    val routeActiveRef  = remember { booleanArrayOf(false) }
    val deviceLatLngRef = remember { doubleArrayOf(latitude, longitude) }
    val userLatLngRef   = remember { arrayOfNulls<LatLng>(1) }
    // True while an edge pan is playing out, so the frame loop does not start another
    // one on top of it.
    val cameraPanning   = remember { booleanArrayOf(false) }

    // Draw / refresh the blue route line on the map
    fun refreshRoute(map: com.google.android.gms.maps.GoogleMap, userLatLng: LatLng) {
        val deviceLatLng = LatLng(deviceLatLngRef[0], deviceLatLngRef[1])
        val existing = routePolyline[0]
        if (existing != null) {
            // Move the existing line rather than replacing it: this runs on every
            // marker frame, and remove + addPolyline allocated a new overlay ~60
            // times a second, which stuttered the whole map.
            existing.points = listOf(userLatLng, deviceLatLng)
            return
        }
        routePolyline[0] = map.addPolyline(
            PolylineOptions()
                .add(userLatLng, deviceLatLng)
                .color(android.graphics.Color.parseColor("#2196F3"))
                .width(8f)
                .geodesic(true)
        )
    }

    // LocationCallback — fires on every user location update (every ~3 s when active)
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val userLatLng = LatLng(loc.latitude, loc.longitude)
                userLatLngRef[0] = userLatLng
                if (!routeActiveRef[0]) return
                val map = mapHolder[0] ?: return
                refreshRoute(map, userLatLng)
            }
        }
    }

    // Start / stop user location updates with the route toggle
    DisposableEffect(routeActive, hasPerm) {
        if (routeActive && hasPerm) {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
                .setMinUpdateIntervalMillis(2_000L)
                .build()
            fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
        }
        onDispose {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPerm = granted }

    LaunchedEffect(isHybrid) {
        mapView.getMapAsync { map ->
            map.mapType = if (isHybrid)
                com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID
            else
                com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
        }
    }

    Box(modifier = modifier) {

        AndroidView(
            factory = { ctx ->
                mapView.apply {
                    onCreate(null)
                    getMapAsync { map ->
                        mapHolder[0] = map
                        // The SDK's my-location layer stays off: it draws a large
                        // translucent accuracy circle along with its dot, and the two
                        // cannot be separated. The dot is drawn by the compass-cone
                        // marker below instead, circle and all gone. The user's
                        // position for the route line comes from fusedClient, not
                        // from this layer, so nothing else depends on it.
                        map.uiSettings.isMyLocationButtonEnabled = false
                        val loc = LatLng(latitude, longitude)
                        markerHolder[0] = map.addMarker(
                            MarkerOptions()
                                .position(loc)
                                .icon(vehicleIcon)
                                .anchor(0.5f, 0.5f)
                                .flat(true)
                        )
                        appliedIcon[0] = vehicleIcon
                        labelHolder[0] = addLabelMarker(map, loc, labelIcon)
                        appliedLabel[0] = labelIcon
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, zoom.toFloat()))
                    }
                }
            },
            // The latitude/longitude arriving here are interpolated frames between two
            // real fixes (see rememberAnimatedLatLng), so this block runs ~60 times a
            // second. Everything in it has to be cheap and must not start an animation
            // of its own.
            update = { _ ->
                val newLatLng = LatLng(latitude, longitude)
                deviceLatLngRef[0] = latitude
                deviceLatLngRef[1] = longitude
                routeActiveRef[0]  = routeActive

                val applyFrame = { map: com.google.android.gms.maps.GoogleMap ->
                    mapHolder[0] = map

                    // ── Vehicle marker bearing + position ────────────
                    // Heading is only recomputed once the vehicle has really travelled
                    // HEADING_BASELINE_METERS from the point the last one was measured
                    // from.
                    //
                    // The baseline is the whole trick, and it is the live equivalent of
                    // the look-ahead playback uses. Over three metres — which is about
                    // what consumer GPS wanders standing still — the angle is mostly
                    // noise, and the marker twitches left and right all the way down a
                    // straight road. Over twenty it is the direction of the road.
                    // A heading handed in beats one measured here: the caller walking
                    // the marker along a snapped road knows where the road goes next,
                    // and this can only see where the coordinates have been.
                    if (!headingDegrees.isNaN()) {
                        lastBearing[0] = headingDegrees.toFloat()
                        lastLocation[0] = latitude
                        lastLocation[1] = longitude
                    } else {
                        val moved = distanceMeters(
                            lastLocation[0], lastLocation[1], latitude, longitude
                        )
                        if (moved >= HEADING_BASELINE_METERS) {
                            lastBearing[0] = calculateBearing(
                                lastLocation[0], lastLocation[1], latitude, longitude
                            )
                            lastLocation[0] = latitude
                            lastLocation[1] = longitude
                        }
                    }
                    // Eased, not assigned: taking the new heading in one frame snapped
                    // the arrow through the whole turn instantly at every corner.
                    drawnBearing[0] = stepBearing(drawnBearing[0], lastBearing[0])
                    // The heading as it is — the artwork faces up, so there is nothing
                    // to correct for. markerRotationDegrees only absorbs the NaN of a
                    // vehicle that has not travelled yet, which Marker will not take.
                    val rotation = if (rotateWithHeading) {
                        markerRotationDegrees(drawnBearing[0].toDouble())
                    } else {
                        0.0
                    }
                    markerHolder[0]?.let { marker ->
                        marker.position = newLatLng
                        marker.rotation = rotation.toFloat()
                        // Identity, not equality: a new descriptor means the vehicle
                        // changed state, and setIcon uploads a bitmap, so it must not
                        // run on the frames in between.
                        if (appliedIcon[0] !== vehicleIcon) {
                            marker.setIcon(vehicleIcon)
                            appliedIcon[0] = vehicleIcon
                        }
                    }

                    // ── The phone's compass cone ─────────────────────
                    // Its own marker at the user's position, not the vehicle's: the
                    // two are usually kilometres apart. Flat, so it turns with the map
                    // and the heading stays measured from real north; drawn under the
                    // vehicle, which is the thing being looked at.
                    val cone = facingHolder[0]
                    if (facingIcon == null || facing == null || !facing.hasHeading) {
                        cone?.remove()
                        facingHolder[0] = null
                    } else {
                        val conePosition = LatLng(facing.latitude, facing.longitude)
                        if (cone == null) {
                            facingHolder[0] = map.addMarker(
                                MarkerOptions()
                                    .position(conePosition)
                                    .icon(facingIcon)
                                    .anchor(0.5f, 0.5f)
                                    .flat(true)
                                    .zIndex(-1f)
                            )
                        } else {
                            cone.position = conePosition
                        }
                        facingHolder[0]?.rotation = facing.headingDegrees.toFloat()
                    }

                    // ── Address chip ─────────────────────────────────
                    // Follows the marker every frame; its bitmap is only pushed when
                    // the address itself changed, for the same reason as the icon.
                    val label = labelHolder[0]
                    if (labelIcon == null) {
                        // The address went away (a fix with no geocode behind it) —
                        // drop the marker rather than leave a stale street on screen.
                        label?.remove()
                        labelHolder[0] = null
                        appliedLabel[0] = null
                    } else if (label == null) {
                        labelHolder[0] = addLabelMarker(map, newLatLng, labelIcon)
                        appliedLabel[0] = labelIcon
                    } else {
                        label.position = newLatLng
                        if (appliedLabel[0] !== labelIcon) {
                            label.setIcon(labelIcon)
                            appliedLabel[0] = labelIcon
                        }
                    }

                    // Camera follows the vehicle only when the route line is NOT
                    // active, and then only once the marker nears an edge.
                    //
                    // Re-centring on every frame — which is what this used to do —
                    // pins the marker to the centre pixel and slides the map underneath
                    // it: the vehicle is travelling the whole time but never moves on
                    // screen, which reads as a marker that does not move at all. Same
                    // fix as playback, same helper.
                    //
                    // An edge pan happens about once per screen-width of travel, so
                    // unlike the old per-frame animateCamera there is never an earlier
                    // animation for this one to cancel. The guard is still needed
                    // because the frame loop keeps running while the pan plays out.
                    if (navigationMode && !routeActive) {
                        // Driving view: the vehicle stays put on screen and the map
                        // turns under it, which is what makes a turn read as the car
                        // turning rather than as the icon spinning.
                        //
                        // moveCamera per frame, not animateCamera: the position and
                        // bearing are already eased by the walk, so animating on top
                        // of them would only lag behind. The zoom and tilt the user
                        // set are left alone.
                        val bearing = drawnBearing[0].takeIf { !it.isNaN() }
                            ?: map.cameraPosition.bearing
                        map.moveCamera(
                            CameraUpdateFactory.newCameraPosition(
                                com.google.android.gms.maps.model.CameraPosition.Builder()
                                    .target(newLatLng)
                                    .bearing(bearing)
                                    .zoom(map.cameraPosition.zoom)
                                    .tilt(map.cameraPosition.tilt)
                                    .build()
                            )
                        )
                        appliedMapBearing[0] = bearing
                    } else if (!routeActive && !cameraPanning[0]) {
                        val bounds = map.projection.visibleRegion.latLngBounds
                        if (!bounds.holdsComfortably(newLatLng)) {
                            cameraPanning[0] = true
                            map.animateCamera(
                                CameraUpdateFactory.newLatLng(newLatLng),
                                CAMERA_PAN_MS,
                                object : com.google.android.gms.maps.GoogleMap.CancelableCallback {
                                    override fun onFinish() { cameraPanning[0] = false }
                                    override fun onCancel() { cameraPanning[0] = false }
                                }
                            )
                        }
                    }

                    // Device moved → refresh route if active
                    if (routeActive) {
                        val userLatLng = userLatLngRef[0]
                        if (userLatLng != null) refreshRoute(map, userLatLng)
                    }
                }

                // Use the map we already hold; getMapAsync would queue a callback per frame.
                val readyMap = mapHolder[0]
                if (readyMap != null) applyFrame(readyMap)
                else mapView.getMapAsync { map -> applyFrame(map) }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Buttons ───────────────────────────────────────────────
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
                        if (routeActive) {
                            // Hide route
                            routePolyline[0]?.remove(); routePolyline[0] = null
                            routeActiveRef[0] = false
                            routeActive       = false
                            // Resume following device
                            mapHolder[0]?.animateCamera(
                                CameraUpdateFactory.newLatLng(LatLng(deviceLatLngRef[0], deviceLatLngRef[1]))
                            )
                        } else {
                            if (!hasPerm) {
                                permLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                return@clickable
                            }
                            // Get current location then show route
                            fusedClient.lastLocation.addOnSuccessListener { loc ->
                                if (loc == null) return@addOnSuccessListener
                                val map = mapHolder[0] ?: return@addOnSuccessListener
                                val userLatLng   = LatLng(loc.latitude, loc.longitude)
                                val deviceLatLng = LatLng(latitude, longitude)
                                userLatLngRef[0] = userLatLng
                                refreshRoute(map, userLatLng)
                                // Zoom to show both endpoints
                                val bounds = LatLngBounds.Builder()
                                    .include(userLatLng)
                                    .include(deviceLatLng)
                                    .build()
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                                routeActiveRef[0] = true
                                routeActive       = true
                            }
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
 * right the whole way down a straight road. Twenty metres is about a car's length
 * four times over — short enough to catch a turn as it happens, long enough that the
 * angle is the road's and not the receiver's.
 */
private const val HEADING_BASELINE_METERS = 20.0

/**
 * Fraction of the remaining turn applied per frame. At ~60 fps a turn settles in
 * roughly a fifth of a second: fast enough to agree with where the vehicle is going,
 * slow enough to read as a turn rather than a snap.
 */
private const val BEARING_EASE = 0.18f

/**
 * Eases [drawn] towards [target] along the shorter arc.
 *
 * Interpolating the raw numbers is not enough: 350° → 10° is a 20° turn to the right,
 * but the plain difference is -340°, which spins the arrow almost all the way round the
 * wrong way. Wrapping the delta into ±180° picks the direction the vehicle actually
 * turned.
 */
private fun stepBearing(drawn: Float, target: Float): Float {
    if (drawn.isNaN()) return target
    val delta = (target - drawn + 540f) % 360f - 180f
    if (Math.abs(delta) < 0.5f) return target
    return (drawn + delta * BEARING_EASE + 360f) % 360f
}

/** Flat-earth distance — accurate enough for a few-metre threshold and far cheaper than haversine. */
private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val meanLat = Math.toRadians((lat1 + lat2) / 2)
    val dLat = (lat2 - lat1) * 111_320.0
    val dLng = (lon2 - lon1) * 111_320.0 * Math.cos(meanLat)
    return Math.hypot(dLat, dLng)
}


// ─────────────────────────────────────────────────────────────────────────────
//  Address chip
//  Sizes are in bitmap pixels, like the marker above — BitmapDescriptor takes
//  the bitmap as-is, so these are not dp.
// ─────────────────────────────────────────────────────────────────────────────

private const val LABEL_TEXT_PX = 21f
private const val LABEL_PADDING_X = 14f
private const val LABEL_PADDING_Y = 9f
private const val LABEL_CORNER_PX = 13f
private const val LABEL_MAX_TEXT_PX = 340f
private const val LABEL_MAX_LINES = 2
private const val LABEL_DOT_RADIUS_PX = 5.5f
private const val LABEL_TAIL_PX = 8f

/**
 * Clearance between the chip's tail and the vehicle's coordinates, so the chip sits
 * above the glyph instead of on top of it.
 *
 * Half the marker bitmap — the marker is anchored at its centre, so that is how far
 * its top edge reaches — plus a few pixels of air. Keep it in step with
 * [VEHICLE_MARKER_PX]: too small and the tail lands on the glyph, too large and the
 * chip floats away from the vehicle it belongs to.
 */
private const val LABEL_GAP_PX = VEHICLE_MARKER_PX / 2f + 6f

/**
 * The marker that carries the address chip.
 *
 * Anchored at the bottom centre, so the bitmap — which holds the chip plus
 * [LABEL_GAP_PX] of transparent space under it — hangs above the coordinates.
 *
 * `flat(false)`: the chip is text, and it has to stay upright and screen-facing
 * however the map is rotated or tilted. The vehicle marker underneath is flat so that
 * its heading means something; this one must not be.
 */
private fun addLabelMarker(
    map: com.google.android.gms.maps.GoogleMap,
    position: LatLng,
    icon: BitmapDescriptor?
): com.google.android.gms.maps.model.Marker? {
    if (icon == null) return null
    return map.addMarker(
        MarkerOptions()
            .position(position)
            .icon(icon)
            .anchor(0.5f, 1f)
            .flat(false)
            // Above the vehicle marker, which would otherwise be drawn over the chip
            // whenever the two overlap.
            .zIndex(1f)
    )
}

/**
 * A white chip holding [text], with a tail pointing down at the vehicle and a dot in
 * [statusColor] — the status the ring round the marker used to carry.
 *
 * Wrapped to [LABEL_MAX_LINES] and ellipsised past that: a full Indian postal address
 * is long enough to span the screen, and a chip that wide stops being readable and
 * starts covering the road the vehicle is on.
 */
/**
 * Shared with the fleet map, which draws the same chip over its selected vehicle —
 * see `homeScreen/PlatformMapView.android.kt`. One builder so the two maps cannot
 * drift into two different-looking chips for the same address.
 */
internal fun addressLabelIcon(text: String, statusColor: Int): BitmapDescriptor {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1B1B1B.toInt()
        textSize = LABEL_TEXT_PX
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
        )
    }

    val lines = wrapToLines(text, textPaint, LABEL_MAX_TEXT_PX, LABEL_MAX_LINES)
    val metrics = textPaint.fontMetrics
    val lineHeight = metrics.descent - metrics.ascent
    val textWidth = lines.maxOf { textPaint.measureText(it) }

    // The dot and the gap after it sit to the left of the text, inside the padding.
    val dotColumn = LABEL_DOT_RADIUS_PX * 2 + 8f
    val chipWidth = dotColumn + textWidth + LABEL_PADDING_X * 2
    val chipHeight = lineHeight * lines.size + LABEL_PADDING_Y * 2

    val width = kotlin.math.ceil(chipWidth).toInt()
    val height = kotlin.math.ceil(chipHeight + LABEL_TAIL_PX + LABEL_GAP_PX).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val chip = android.graphics.RectF(0f, 0f, chipWidth, chipHeight)
    val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    }
    canvas.drawRoundRect(chip, LABEL_CORNER_PX, LABEL_CORNER_PX, chipPaint)

    // Tail, drawn as a triangle off the bottom edge rather than a rotated square, so
    // it keeps the chip's crisp edge where the two meet.
    canvas.drawPath(
        Path().apply {
            moveTo(chipWidth / 2f - LABEL_TAIL_PX, chipHeight - 1f)
            lineTo(chipWidth / 2f + LABEL_TAIL_PX, chipHeight - 1f)
            lineTo(chipWidth / 2f, chipHeight + LABEL_TAIL_PX)
            close()
        },
        chipPaint
    )

    // A hairline border: a plain white chip disappears into a snow-white building
    // footprint, and this map has a satellite mode where that is most of the frame.
    canvas.drawRoundRect(
        chip,
        LABEL_CORNER_PX,
        LABEL_CORNER_PX,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            color = 0x33000000
        }
    )

    canvas.drawCircle(
        LABEL_PADDING_X + LABEL_DOT_RADIUS_PX,
        chipHeight / 2f,
        LABEL_DOT_RADIUS_PX,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = statusColor }
    )

    var baseline = LABEL_PADDING_Y - metrics.ascent
    val textLeft = LABEL_PADDING_X + dotColumn
    for (line in lines) {
        canvas.drawText(line, textLeft, baseline, textPaint)
        baseline += lineHeight
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Greedy word wrap to at most [maxLines] lines no wider than [maxWidth], with the last
 * line ellipsised if the text runs past it.
 *
 * A word longer than the line — a run-on address with no spaces in it — is broken
 * mid-word rather than allowed to widen the chip.
 */
private fun wrapToLines(
    text: String,
    paint: Paint,
    maxWidth: Float,
    maxLines: Int
): List<String> {
    val lines = mutableListOf<String>()
    var current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            lines.add(current.toString())
            current = StringBuilder()
        }
    }

    for (word in text.split(' ').filter { it.isNotBlank() }) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) {
            current = StringBuilder(candidate)
            continue
        }
        flush()
        if (lines.size == maxLines) break
        // The word itself does not fit on an empty line — take as much of it as does.
        if (paint.measureText(word) > maxWidth) {
            var cut = word.length
            while (cut > 1 && paint.measureText(word.substring(0, cut)) > maxWidth) cut--
            lines.add(word.substring(0, cut))
            if (lines.size == maxLines) break
            current = StringBuilder(word.substring(cut))
        } else {
            current = StringBuilder(word)
        }
    }
    if (lines.size < maxLines) flush()

    if (lines.isEmpty()) return listOf(text)

    // Something was dropped: say so on the last line rather than ending mid-street.
    val rendered = lines.joinToString(" ")
    if (rendered.length < text.replace(Regex("\\s+"), " ").trim().length) {
        var last = lines.last()
        while (last.isNotEmpty() && paint.measureText("$last…") > maxWidth) {
            last = last.dropLast(1)
        }
        lines[lines.size - 1] = "$last…"
    }

    return lines
}

/** [fraction] of the way from [from] to [to], per channel. */
private fun blend(from: Int, to: Int, fraction: Float): Int = android.graphics.Color.rgb(
    (android.graphics.Color.red(from) + (android.graphics.Color.red(to) - android.graphics.Color.red(from)) * fraction).toInt(),
    (android.graphics.Color.green(from) + (android.graphics.Color.green(to) - android.graphics.Color.green(from)) * fraction).toInt(),
    (android.graphics.Color.blue(from) + (android.graphics.Color.blue(to) - android.graphics.Color.blue(from)) * fraction).toInt()
)

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val dLng = Math.toRadians(lon2 - lon1)
    val y = Math.sin(dLng) * Math.cos(Math.toRadians(lat2))
    val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
            Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLng)
    return ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360).toFloat()
}