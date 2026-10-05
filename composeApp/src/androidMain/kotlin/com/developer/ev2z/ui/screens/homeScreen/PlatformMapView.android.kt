// androidMain/ui/screens/homeScreen/PlatformMapView.android.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.model.Device
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
) {
    var isMapLoaded by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) } // ✅ ADD

    // Drawn positions, which glide towards the polled ones over ~1.5s instead of
    // snapping. Updated every animation frame, so reading it here is what drives the
    // marker across the map.
    val smoothedPositions = rememberSmoothDevicePositions(devices)

    // 🇮🇳 Opening view, set once at construction. Doing it here rather than from an
    // effect keyed on `devices` means a polled coordinate update can't drag the
    // camera back to the start.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(22.9734, 78.6569), 4f)
    }

    // Auto-fit happens ONCE, on the first batch of coordinates to arrive. After that
    // the viewport belongs to the user: positions now refresh every 15s, and
    // re-fitting on each one would throw away their pan and zoom while they look.
    var hasFittedCamera by remember { mutableStateOf(false) }

    LaunchedEffect(isMapLoaded, devices) {
        if (hasFittedCamera || !isMapLoaded) return@LaunchedEffect

        val located = devices.mapNotNull { device ->
            val lat = device.getLatitudeDouble() ?: return@mapNotNull null
            val lng = device.getLongitudeDouble() ?: return@mapNotNull null
            LatLng(lat, lng)
        }
        if (located.isEmpty()) return@LaunchedEffect

        hasFittedCamera = true
        try {
            cameraPositionState.animate(
                // newLatLngBounds on a single point zooms to the maximum level, so a
                // one-device account gets an explicit sensible zoom instead.
                if (located.size == 1) {
                    CameraUpdateFactory.newLatLngZoom(located.first(), 15f)
                } else {
                    val bounds = LatLngBounds.Builder()
                        .apply { located.forEach { include(it) } }
                        .build()
                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                },
                durationMs = 1000
            )
        } catch (e: Exception) {
            println("❌ Error fitting camera: ${e.message}")
        }
    }

    // Zoom to a device the user picked. Keyed on the id, not the Device object —
    // keying on the object would re-run on every position refresh and re-centre the
    // map every 15s, which is the follow-the-vehicle behaviour this screen does not
    // want (Track & Trace is the screen for that).
    LaunchedEffect(isMapLoaded, selectedDevice?.deviceId) {
        if (!isMapLoaded) return@LaunchedEffect
        val device = selectedDevice ?: return@LaunchedEffect
        val lat = device.getLatitudeDouble() ?: return@LaunchedEffect
        val lng = device.getLongitudeDouble() ?: return@LaunchedEffect
        try {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f),
                durationMs = 1000
            )
        } catch (e: Exception) {
            println("❌ Error zooming to device: ${e.message}")
        }
    }


    Box(modifier = modifier) {  // ✅ Box mein wrap karo

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapType = mapType  // ✅ dynamic
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
            onMapLoaded = { isMapLoaded = true }
        ) {
        devices.forEach { device ->
            // The glided position when there is one; the raw coordinates on the very
            // first frame, before the animation has had a chance to seed itself.
            val position = smoothedPositions[device.deviceId]
                ?: device.getLatitudeDouble()?.let { lat ->
                    device.getLongitudeDouble()?.let { lng -> lat to lng }
                }
                ?: return@forEach
            val (lat, lng) = position

            // Keyed per device so each marker keeps its own state slot. Without a key
            // the slots are positional, and a device dropping out of the list would
            // hand its state — and its position — to whichever device shifted up.
            key(device.deviceId) {
                // Keyed off getStatusKind() so the pin agrees with the label and the
                // dot in the overview sheet. The raw `when (device.status)` this
                // replaces knew only three values and matched them case-sensitively,
                // so a parked vehicle drew the same azure pin as an offline one —
                // there was no way to tell a parked vehicle from a dead tracker.
                val markerColor = when (device.getStatusKind()) {
                    "running" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                    "idle" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW
                    "parked", "stopped" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                    else -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                }

                // Tapping a pin is the most direct way to ask "where is it?", so the
                // info window answers with the address for the coordinates of the
                // latest poll, falling back to status and speed until it resolves.
                val address = rememberDeviceAddress(device)

                Marker(
                    // rememberUpdatedMarkerState pushes each new position into the one
                    // remembered state, which is what actually walks the marker across
                    // the map. Constructing MarkerState inline here instead would throw
                    // the state away on every recomposition.
                    state = rememberUpdatedMarkerState(position = LatLng(lat, lng)),
                    title = device.getDisplayName(),
                    snippet = address
                        ?: "${device.getStatusDisplay()} - ${device.speed} km/h",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor),
                    onClick = {
                        onMarkerClick(device)
                        false
                    }
                )
            }
        }
    }



// ✅ Professional Map Type Toggle Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .clickable {
                    mapType = if (mapType == MapType.NORMAL) MapType.HYBRID
                    else MapType.NORMAL
                }
                .padding(8.dp)
        ) {
            Text(
                text = if (mapType == MapType.NORMAL) "🛰️" else "🗺️",
                fontSize = 18.sp
            )
        }


    }

}

