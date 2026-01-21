package com.developer.ev2z.ui.screens.device_Report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.developer.ev2z.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double
) {
    val context = LocalContext.current

    val mapView = remember { MapView(context) }
    val markerHolder = remember { arrayOfNulls<com.google.android.gms.maps.model.Marker>(1) }
    val lastLocation = remember { doubleArrayOf(latitude, longitude) }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                getMapAsync { map ->

                    val location = LatLng(latitude, longitude)

                    val bikeIcon = getResizedBikeIcon(context)

                    markerHolder[0] = map.addMarker(
                        MarkerOptions()
                            .position(location)
                            .icon(bikeIcon)
                            .anchor(0.5f, 0.5f)
                            .flat(true) // 🔥 important
                    )

                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(location, zoom.toFloat())
                    )
                }
            }
        },
        update = {
            val newLocation = LatLng(latitude, longitude)

            markerHolder[0]?.let { marker ->

                // 🔁 Bearing calculate
                val bearing = calculateBearing(
                    lastLocation[0],
                    lastLocation[1],
                    latitude,
                    longitude
                )

                marker.position = newLocation
                marker.rotation = bearing
            }

            lastLocation[0] = latitude
            lastLocation[1] = longitude

            mapView.getMapAsync { map ->
                map.animateCamera(
                    CameraUpdateFactory.newLatLng(newLocation)
                )
            }
        },
        modifier = modifier
    )
}

private fun getResizedBikeIcon(
    context: Context,
    width: Int = 120,
    height: Int = 120
): BitmapDescriptor {

    val drawable = ContextCompat.getDrawable(
        context,
        R.drawable.ic_vechile
    ) ?: return BitmapDescriptorFactory.defaultMarker()

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}


fun calculateBearing(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Float {
    val startLat = Math.toRadians(lat1)
    val startLng = Math.toRadians(lon1)
    val endLat = Math.toRadians(lat2)
    val endLng = Math.toRadians(lon2)

    val dLng = endLng - startLng

    val y = Math.sin(dLng) * Math.cos(endLat)
    val x =
        Math.cos(startLat) * Math.sin(endLat) -
                Math.sin(startLat) * Math.cos(endLat) * Math.cos(dLng)

    var bearing = Math.toDegrees(Math.atan2(y, x))
    bearing = (bearing + 360) % 360

    return bearing.toFloat()
}

