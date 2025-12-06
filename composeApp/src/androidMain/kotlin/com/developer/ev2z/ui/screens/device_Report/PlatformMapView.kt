package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
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

    AndroidView(factory = {
        mapView.apply {
            onCreate(null)
            getMapAsync { map ->
                val location = LatLng(latitude, longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom.toFloat()))
                map.addMarker(
                    MarkerOptions().position(location).title("Device Location")
                )
            }
        }
    }, modifier = modifier)
}