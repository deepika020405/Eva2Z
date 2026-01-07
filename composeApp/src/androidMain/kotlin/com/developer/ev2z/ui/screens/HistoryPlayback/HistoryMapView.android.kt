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

    val currentPoint = latLngList.getOrElse(currentIndex) { latLngList.first() }

//    val cameraPositionState = rememberCameraPositionState {
//        position = CameraPosition.fromLatLngZoom(currentPoint, 15f)
//    }

    val markerState = remember(currentPoint) { MarkerState(position = currentPoint) }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(currentIndex, latLngList) {
        if (currentIndex in latLngList.indices) {
            val newLatLng = latLngList[currentIndex]

            // ✅ Animate camera safely
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(newLatLng, 15f)
            )
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

        Marker(
            state = markerState,
            title = "Point ${currentIndex + 1}",
            snippet = routePoints.getOrNull(currentIndex)?.createdAt ?: ""
        )
    }
}

