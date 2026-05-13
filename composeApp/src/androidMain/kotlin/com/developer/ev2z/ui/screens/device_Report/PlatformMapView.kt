package com.developer.ev2z.ui.screens.device_Report

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double,
    trailPoints: List<TrailPoint>
) {
    val context = LocalContext.current
    var isHybrid by remember { mutableStateOf(false) }

    val mapView        = remember { MapView(context) }
    val markerHolder   = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }
    val startMarker    = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }
    val polylineHolder = remember { arrayOfNulls<Polyline>(1) }
    val lastLocation   = remember { doubleArrayOf(latitude, longitude) }
    val mapHolder            = remember { arrayOfNulls<com.google.android.gms.maps.GoogleMap>(1) }
    val userToDevicePolyline = remember { arrayOfNulls<Polyline>(1) }
    val userMarkerHolder     = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }
    var routeActive          by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun drawUserToDeviceRoute() {
        val map = mapHolder[0] ?: return
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) return@addOnSuccessListener
            val userLatLng   = LatLng(loc.latitude, loc.longitude)
            val deviceLatLng = LatLng(latitude, longitude)

            userMarkerHolder[0]?.remove()
            userMarkerHolder[0] = map.addMarker(
                MarkerOptions()
                    .position(userLatLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .title("Your Location")
            )

            userToDevicePolyline[0]?.remove()
            userToDevicePolyline[0] = map.addPolyline(
                PolylineOptions()
                    .add(userLatLng, deviceLatLng)
                    .color(android.graphics.Color.parseColor("#2196F3"))
                    .width(10f)
                    .geodesic(true)
            )

            val bounds = LatLngBounds.Builder()
                .include(userLatLng)
                .include(deviceLatLng)
                .build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            routeActive = true
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) drawUserToDeviceRoute()
    }

    // ✅ mapType toggle
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
                        val location = LatLng(latitude, longitude)
                        val bikeIcon = getNavigationIcon(ctx)

                        markerHolder[0] = map.addMarker(
                            MarkerOptions()
                                .position(location)
                                .icon(bikeIcon)
                                .anchor(0.5f, 0.5f)
                                .flat(true)
                        )

                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(location, zoom.toFloat())
                        )
                    }
                }
            },
            update = { _ ->
                val newLocation = LatLng(latitude, longitude)

                mapView.getMapAsync { map ->
                    mapHolder[0] = map

                    // ── 1. Vehicle marker move + rotate ──────────────
                    val bearing = calculateBearing(
                        lastLocation[0], lastLocation[1],
                        latitude, longitude
                    )

                    markerHolder[0]?.let { marker ->
                        marker.position = newLocation
                        marker.rotation = bearing
                    }

                    lastLocation[0] = latitude
                    lastLocation[1] = longitude

                    // ── 2. Camera follow ──────────────────────────────
                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(newLocation)
                    )

                    // ── 3. Trail polyline ─────────────────────────────
                    if (trailPoints.size >= 2) {

                        polylineHolder[0]?.remove()

                        val latLngPoints = trailPoints.map {
                            LatLng(it.latitude, it.longitude)
                        }

                        polylineHolder[0] = map.addPolyline(
                            PolylineOptions()
                                .addAll(latLngPoints)
                                .color(android.graphics.Color.parseColor("#2196F3"))
                                .width(8f)
                                .geodesic(true)
                        )

                        // ── 4. Start point marker ─────────────────────
                        startMarker[0]?.remove()

                        val startPoint = latLngPoints.first()
                        startMarker[0] = map.addMarker(
                            MarkerOptions()
                                .position(startPoint)
                                .icon(
                                    BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_GREEN
                                    )
                                )
                                .title("Start")
                                .anchor(0.5f, 0.5f)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
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
                        if (routeActive) {
                            userToDevicePolyline[0]?.remove()
                            userMarkerHolder[0]?.remove()
                            userToDevicePolyline[0] = null
                            userMarkerHolder[0] = null
                            routeActive = false
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                drawUserToDeviceRoute()
                            } else {
                                locationPermissionLauncher.launch(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            }
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
// Navigation arrow icon
// ─────────────────────────────────────────────────────────────────
private fun getNavigationIcon(
    context: Context,
    width: Int  = 120,
    height: Int = 120
): BitmapDescriptor {
    val drawable = ContextCompat.getDrawable(context, R.drawable.navigation)
        ?: return BitmapDescriptorFactory.defaultMarker()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

// ─────────────────────────────────────────────────────────────────
// Bearing calculator
// ─────────────────────────────────────────────────────────────────
fun calculateBearing(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Float {
    val startLat = Math.toRadians(lat1)
    val startLng = Math.toRadians(lon1)
    val endLat   = Math.toRadians(lat2)
    val endLng   = Math.toRadians(lon2)

    val dLng = endLng - startLng

    val y = Math.sin(dLng) * Math.cos(endLat)
    val x = Math.cos(startLat) * Math.sin(endLat) -
            Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLng)

    var bearing = Math.toDegrees(Math.atan2(y, x))
    bearing = (bearing + 360) % 360

    return bearing.toFloat()
}