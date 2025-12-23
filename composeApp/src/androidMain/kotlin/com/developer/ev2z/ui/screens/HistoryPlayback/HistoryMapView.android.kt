// composeApp/src/androidMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryMapView.android.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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

    // ✅ FIX: Remember marker state
    val markerState = remember(currentPoint) {
        MarkerState(position = currentPoint)
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex in latLngList.indices) {
            val newPosition = latLngList[currentIndex]
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(newPosition, 15f),
                durationMs = 500
            )
            // ✅ Update marker position
            markerState.position = newPosition
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = true)
    ) {
        Polyline(
            points = latLngList,
            color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
            width = 10f
        )

        // ✅ FIX: Use remembered state
        Marker(
            state = markerState,
            title = "Point ${currentIndex + 1}",
            snippet = routePoints.getOrNull(currentIndex)?.createdAt ?: ""
        )
    }
}