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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.response.RoutePoint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*


@Composable
actual fun HistoryMapView(
    routePoints: List<RoutePoint>,
    currentIndex: Int,
    modifier: Modifier
) {
    if (routePoints.isEmpty()) return

    val latLngList = routePoints.mapNotNull { point ->
        val lat = point.getLatDouble()
        val lng = point.getLngDouble()
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }
    if (latLngList.isEmpty()) return

    val currentPoint = latLngList.getOrElse(currentIndex) { latLngList.first() }
    val markerState = remember(currentPoint) { MarkerState(position = currentPoint) }
    val cameraPositionState = rememberCameraPositionState()

    var isHybrid by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapType = if (isHybrid) MapType.HYBRID else MapType.NORMAL
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            MapEffect(currentIndex, latLngList) { map ->
                val newLatLng = latLngList.getOrNull(currentIndex) ?: return@MapEffect
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(newLatLng, 15f)
                )
            }

            Polyline(
                points = latLngList,
                color = Color(0xFF4CAF50),
                width = 10f
            )

            Marker(
                state = markerState,
                title = "Point ${currentIndex + 1}",
                snippet = routePoints.getOrNull(currentIndex)?.createdAt ?: ""
            )
        }

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
