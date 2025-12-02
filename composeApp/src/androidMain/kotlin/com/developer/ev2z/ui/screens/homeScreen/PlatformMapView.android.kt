package com.developer.ev2z.ui.screens.homeScreen

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
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

    // remember the MapView instance across recompositions
    val mapView = remember {
        MapView(context).apply {
            // Important: do not call lifecycle methods here except onCreate; we'll handle lifecycle in DisposableEffect
            onCreate(null)
        }
    }

    // Manage MapView lifecycle to avoid memory leaks
    DisposableEffect(mapView) {
        // resume right after creation so map starts loading
        mapView.onResume()
        onDispose {
            try {
                mapView.onPause()
                mapView.onDestroy()
            } catch (_: Throwable) { /* ignore */ }
        }
    }

    // Place the MapView in Compose hierarchy with explicit generic to avoid type inference issue
    AndroidView<MapView>(
        modifier = modifier,
        factory = { mv ->
            // factory receives a Context; we create/return the remembered mapView
            mapView
        },
        update = { mv ->
            // getMapAsync is safe to call repeatedly; if map already ready, callback runs immediately
            mv.getMapAsync { googleMap: GoogleMap ->
                val pos = LatLng(latitude, longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, zoom.toFloat()))
                googleMap.uiSettings?.isZoomControlsEnabled = true
                googleMap.addMarker(MarkerOptions().position(pos).title("Delhi"))
            }
        }
    )
}