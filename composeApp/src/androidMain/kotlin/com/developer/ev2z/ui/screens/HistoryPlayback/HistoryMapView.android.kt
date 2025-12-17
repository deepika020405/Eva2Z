// composeApp/src/androidMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryMapView.android.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.developer.ev2z.response.RoutePoint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
actual fun HistoryMapView(
    routePoints: List<RoutePoint>,
    currentIndex: Int,
    modifier: Modifier
) {
    if (routePoints.isEmpty()) return

    // Get valid coordinates
    val latLngList = routePoints.mapNotNull { point ->
        val lat = point.getLatDouble()
        val lng = point.getLngDouble()
        if (lat != null && lng != null) LatLng(lat, lng) else null
    }

    if (latLngList.isEmpty()) return

    val currentPoint = if (currentIndex in latLngList.indices) {
        latLngList[currentIndex]
    } else {
        latLngList.firstOrNull() ?: return
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentPoint, 15f)
    }

    // ✅ Move camera to current playback position
    LaunchedEffect(currentIndex) {
        if (currentIndex in latLngList.indices) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(latLngList[currentIndex], 15f),
                durationMs = 500
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        // ✅ Draw route polyline
        Polyline(
            points = latLngList,
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            width = 10f
        )

        // ✅ Current position marker (animated)
        Marker(
            state = MarkerState(position = currentPoint),
            title = "Point ${currentIndex + 1}",
            snippet = routePoints.getOrNull(currentIndex)?.createdAt ?: ""
        )
    }
}