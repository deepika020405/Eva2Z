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
    val lastLocation   = remember { doubleArrayOf(latitude, longitude) }
    val mapHolder      = remember { arrayOfNulls<com.google.android.gms.maps.GoogleMap>(1) }
    val fusedClient    = remember { LocationServices.getFusedLocationProviderClient(context) }
    // Mutable refs readable by the LocationCallback (main thread)
    val routeActiveRef  = remember { booleanArrayOf(false) }
    val deviceLatLngRef = remember { doubleArrayOf(latitude, longitude) }
    val userLatLngRef   = remember { arrayOfNulls<LatLng>(1) }

    // Draw / refresh the blue route line on the map
    fun refreshRoute(map: com.google.android.gms.maps.GoogleMap, userLatLng: LatLng) {
        val deviceLatLng = LatLng(deviceLatLngRef[0], deviceLatLngRef[1])
        routePolyline[0]?.remove()
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
            update = { _ ->
                val newLatLng = LatLng(latitude, longitude)
                deviceLatLngRef[0] = latitude
                deviceLatLngRef[1] = longitude
                routeActiveRef[0]  = routeActive

                mapView.getMapAsync { map ->
                    mapHolder[0] = map

                    // ── Vehicle marker bearing + position ────────────
                    val bearing = calculateBearing(lastLocation[0], lastLocation[1], latitude, longitude)
                    markerHolder[0]?.let { it.position = newLatLng; it.rotation = bearing }
                    lastLocation[0] = latitude
                    lastLocation[1] = longitude

                    // Camera follows device only when route is NOT active
                    if (!routeActive) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(newLatLng))
                    }

                    // Device moved → refresh route if active
                    if (routeActive) {
                        val userLatLng = userLatLngRef[0]
                        if (userLatLng != null) refreshRoute(map, userLatLng)
                    }
                }
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