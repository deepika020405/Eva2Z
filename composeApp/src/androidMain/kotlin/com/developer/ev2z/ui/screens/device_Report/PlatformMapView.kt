package com.developer.ev2z.ui.screens.device_Report

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
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
import com.developer.ev2z.R
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
    trailPoints: List<TrailPoint>   // not rendered — kept for API compatibility
) {
    val context = LocalContext.current

    var isHybrid    by remember { mutableStateOf(false) }
    var routeActive by remember { mutableStateOf(false) }
    var hasPerm     by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val mapView        = remember { MapView(context) }
    val markerHolder   = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }
    val routePolyline  = remember { arrayOfNulls<Polyline>(1) }
    // Last point the marker's heading was computed from, and that heading. Both are
    // only updated once the vehicle has actually travelled BEARING_MIN_MOVE_METERS.
    val lastLocation   = remember { doubleArrayOf(latitude, longitude) }
    val lastBearing    = remember { floatArrayOf(0f) }
    val mapHolder      = remember { arrayOfNulls<com.google.android.gms.maps.GoogleMap>(1) }
    val fusedClient    = remember { LocationServices.getFusedLocationProviderClient(context) }
    // Mutable refs readable by the LocationCallback (main thread)
    val routeActiveRef  = remember { booleanArrayOf(false) }
    val deviceLatLngRef = remember { doubleArrayOf(latitude, longitude) }
    val userLatLngRef   = remember { arrayOfNulls<LatLng>(1) }

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
                        if (hasPerm) {
                            map.isMyLocationEnabled = true
                            map.uiSettings.isMyLocationButtonEnabled = false
                        }
                        val loc = LatLng(latitude, longitude)
                        markerHolder[0] = map.addMarker(
                            MarkerOptions()
                                .position(loc)
                                .icon(getNavigationIcon(ctx))
                                .anchor(0.5f, 0.5f)
                                .flat(true)
                        )
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
                    // Heading is only recomputed once the vehicle has really moved: a
                    // sub-metre interpolation step yields an essentially random angle,
                    // which made the arrow spin on the spot.
                    val moved = distanceMeters(
                        lastLocation[0], lastLocation[1], latitude, longitude
                    )
                    if (moved >= BEARING_MIN_MOVE_METERS) {
                        lastBearing[0] = calculateBearing(
                            lastLocation[0], lastLocation[1], latitude, longitude
                        )
                        lastLocation[0] = latitude
                        lastLocation[1] = longitude
                    }
                    markerHolder[0]?.let { it.position = newLatLng; it.rotation = lastBearing[0] }

                    // Camera follows device only when route is NOT active.
                    // moveCamera, not animateCamera: an animation started on every frame
                    // was cancelled 16 ms later by the next one, so the camera never
                    // reached its target and visibly dragged behind the marker.
                    if (!routeActive) {
                        map.moveCamera(CameraUpdateFactory.newLatLng(newLatLng))
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
 * Travel below this is interpolation noise rather than a direction the vehicle is
 * heading in, so the marker keeps the bearing it already has.
 */
private const val BEARING_MIN_MOVE_METERS = 3.0

/** Flat-earth distance — accurate enough for a few-metre threshold and far cheaper than haversine. */
private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val meanLat = Math.toRadians((lat1 + lat2) / 2)
    val dLat = (lat2 - lat1) * 111_320.0
    val dLng = (lon2 - lon1) * 111_320.0 * Math.cos(meanLat)
    return Math.hypot(dLat, dLng)
}

private fun getNavigationIcon(context: Context, width: Int = 120, height: Int = 120): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, R.drawable.navigation)
        ?: return BitmapDescriptorFactory.defaultMarker()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val dLng = Math.toRadians(lon2 - lon1)
    val y = Math.sin(dLng) * Math.cos(Math.toRadians(lat2))
    val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
            Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLng)
    return ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360).toFloat()
}