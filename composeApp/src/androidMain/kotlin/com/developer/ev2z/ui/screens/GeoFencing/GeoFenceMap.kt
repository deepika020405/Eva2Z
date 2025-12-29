package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.developer.ev2z.data.local.model.GeoFencePolygon
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun GeoFenceMap(
    polygon: GeoFencePolygon?
) {
    val cameraPositionState = rememberCameraPositionState()

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {

        polygon?.let { poly ->

            val latLngs = poly.points.map {
                com.google.android.gms.maps.model.LatLng(it.lat, it.lng)
            }

            // 🟢 MOVE CAMERA TO POLYGON
            LaunchedEffect(poly) {
                val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.builder()
                latLngs.forEach { boundsBuilder.include(it) }
                cameraPositionState.animate(
                    com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(
                        boundsBuilder.build(),
                        80
                    )
                )
            }

            Polygon(
                points = latLngs,
                fillColor = Color(0x5534C759),
                strokeColor = Color(0xFF34C759),
                strokeWidth = 3f
            )
        }
    }
}