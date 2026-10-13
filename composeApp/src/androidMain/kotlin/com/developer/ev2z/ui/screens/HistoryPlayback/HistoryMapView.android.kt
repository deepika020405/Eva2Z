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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.network.SnappedRoute
import com.developer.ev2z.util.rememberDeviceFacing
import com.developer.ev2z.ui.components.CAMERA_PAN_MS
import com.developer.ev2z.ui.components.UserFacingConeMarker
import com.developer.ev2z.ui.components.holdsComfortably
import com.developer.ev2z.ui.components.vehicleArrowIcon
import com.developer.ev2z.ui.components.markerRotationDegrees
import com.developer.ev2z.ui.components.vehicleMarkerIcon
import com.developer.ev2z.ui.components.vehicleTopViewFor
import com.developer.ev2z.response.RoutePoint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.filterNotNull


@Composable
actual fun HistoryMapView(
    routePoints: List<RoutePoint>,
    snappedRoute: SnappedRoute,
    currentIndex: Int,
    modifier: Modifier,
    vehicleSlug: String?
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

    // Keep the marker on screen as it glides, at whatever zoom the user has set — but
    // only move the camera when the marker nears the edge.
    //
    // Re-centring on every frame, which is what this used to do, pins the marker to the
    // middle of the screen and slides the map underneath it. The marker is travelling
    // the whole time, but it never leaves the centre pixel, so playback reads as a
    // marker that does not move at all. Panning only at the edge lets it cross the
    // screen, which is what makes the movement visible.
    //
    // The old MapEffect before that re-issued animateCamera(newLatLngZoom(.., 15f)) on
    // every 500 ms step, which stuttered (each animation interrupted the one still
    // running) and snatched the zoom back so the map could not be zoomed out
    // mid-playback. Neither is reintroduced here.
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { smoothPosition.value }
            .filterNotNull()
            .collect { position ->
                val target = LatLng(position.latitude, position.longitude)
                // Null until the map has drawn a frame, which is also when there is
                // nothing on screen to keep the marker inside of.
                val bounds = cameraPositionState.projection?.visibleRegion?.latLngBounds
                if (bounds != null && bounds.holdsComfortably(target)) return@collect

                // Animated, not assigned: this now runs once per screen-width of
                // travel rather than 60 times a second, so there is no animation left
                // running for the next one to interrupt. runCatching because a camera
                // animation throws when the user takes hold of the map mid-pan.
                runCatching {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLng(target),
                        CAMERA_PAN_MS
                    )
                }
            }
    }

    var isHybrid by remember { mutableStateOf(false) }

    // Where the phone is and which way its holder is facing, for the compass cone.
    // Worth having on playback too: it answers "was it anywhere near me" without
    // reading coordinates off the screen.
    val facing = rememberDeviceFacing()

    // The vehicle the history response named, drawn exactly as the live map draws it —
    // playback used to show Google's default red pin here, so the same vehicle looked
    // like two different things depending on which screen you were on.
    val vehicle = vehicleTopViewFor(vehicleSlug)

    // The colour comes from the fix being played back, so the marker reports what the
    // vehicle was doing at that moment rather than what it is doing now. It changes as
    // playback runs through the day: green over the stretches it was moving, red over
    // the ones where it had stopped.
    val statusKind = routePoints.getOrNull(currentIndex)?.statusKind() ?: "unknown"

    // Rebuilt only when the vehicle or that state changes: the marker below moves on
    // every animation frame, and rasterising a bitmap per frame would allocate one
    // 60 times a second. The heading never changes it — the artwork is drawn from
    // above, so the marker is simply turned, see VehicleTopView.
    //
    // Null if the Maps SDK cannot supply a descriptor, which falls back to the default
    // pin below rather than crashing the screen.
    val context = LocalContext.current
    val markerIcon = remember(vehicle, statusKind) {
        val color = statusColorOf(statusKind).toInt()
        if (vehicle == null) vehicleArrowIcon(context, color)
        else vehicleMarkerIcon(context, vehicle, color)
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                // Off: the SDK's my-location layer draws a large translucent accuracy
                // circle along with its dot, and the two cannot be separated. The dot
                // is drawn by UserFacingConeMarker instead, circle and all gone.
                isMyLocationEnabled = false,
                mapType = if (isHybrid) MapType.HYBRID else MapType.NORMAL
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            UserFacingConeMarker(facing)

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
                snippet = routePoints.getOrNull(currentIndex)?.createdAt ?: "",
                icon = markerIcon,
                // Turns with the road: the vehicle comes round a roundabout facing the
                // way it is going instead of sliding round it pointing north. The
                // heading as it is, since the artwork is drawn from above and facing up
                // — markerRotationDegrees only absorbs the NaN of a marker that has not
                // travelled yet, which Marker will not take.
                rotation = markerRotationDegrees(
                    smoothPosition.value?.bearingDegrees ?: Double.NaN
                ).toFloat(),
                // Centred on the coordinate, not hanging above it by a pin's height:
                // the vehicle is at the point, so its middle belongs there. The
                // fallback pin is a balloon and has to keep hanging by its tip.
                anchor = if (markerIcon != null) Offset(0.5f, 0.5f) else Offset(0.5f, 1f),
                // Lies on the map, so it turns when the user rotates the map — the
                // same as the live map's marker.
                flat = markerIcon != null
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


