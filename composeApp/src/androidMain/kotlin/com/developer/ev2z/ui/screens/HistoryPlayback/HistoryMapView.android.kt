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
import com.developer.ev2z.network.SnappedRoute
import com.developer.ev2z.response.RoutePoint
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.filterNotNull


@Composable
actual fun HistoryMapView(
    routePoints: List<RoutePoint>,
    snappedRoute: SnappedRoute,
    currentIndex: Int,
    modifier: Modifier
) {
    if (routePoints.isEmpty()) return

    // The road-snapped path when it is ready, the recorded fixes until then — the
    // snapped one bends with the street instead of cutting between fixes.
    val latLngList = remember(routePoints, snappedRoute) {
        if (snappedRoute.path.size >= 2) {
            snappedRoute.path.map { LatLng(it.latitude, it.longitude) }
        } else {
            routePoints.mapNotNull { point ->
                val lat = point.getLatDouble()
                val lng = point.getLngDouble()
                if (lat != null && lng != null) LatLng(lat, lng) else null
            }
        }
    }
    if (latLngList.isEmpty()) return

    // Updated every animation frame. Deliberately NOT read here — it is read inside
    // the GoogleMap content lambda, so only the marker recomposes per frame instead
    // of this whole composable and its route-list remember.
    val smoothPosition = rememberSmoothPlaybackPosition(routePoints, currentIndex, snappedRoute)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(latLngList.first(), 15f)
    }

    // Keep the camera on the marker as it glides, at whatever zoom the user has set.
    // The old MapEffect re-issued animateCamera(newLatLngZoom(.., 15f)) on every 500ms
    // step, which did two unwanted things: each animation interrupted the one still
    // running — the stutter — and the hardcoded 15f snatched the zoom back every step
    // so the map could not be zoomed out mid-playback.
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { smoothPosition.value }
            .filterNotNull()
            .collect { position ->
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(position.latitude, position.longitude),
                    cameraPositionState.position.zoom,
                )
            }
    }

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
            Polyline(
                points = latLngList,
                color = Color(0xFF4CAF50),
                width = 10f
            )

            Marker(
                // rememberUpdatedMarkerState pushes each frame's position into the one
                // remembered state, which is what actually carries the marker along the
                // line. The previous `remember(currentPoint) { MarkerState(...) }` threw
                // the state away and built a new one on every step instead.
                state = rememberUpdatedMarkerState(
                    // The fallback reads routePoints, not latLngList: the latter is the
                    // road path once snapping lands, where currentIndex is no longer the
                    // index of anything — it counts samples, and the path counts road
                    // vertices.
                    position = smoothPosition.value
                        ?.let { LatLng(it.latitude, it.longitude) }
                        ?: routePoints.getOrNull(currentIndex)
                            ?.let { point ->
                                val lat = point.getLatDouble()
                                val lng = point.getLngDouble()
                                if (lat != null && lng != null) LatLng(lat, lng) else null
                            }
                        ?: latLngList.first()
                ),
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


