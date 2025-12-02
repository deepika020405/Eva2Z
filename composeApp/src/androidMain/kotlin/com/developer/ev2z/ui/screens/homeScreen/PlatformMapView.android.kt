// androidMain/ui/screens/homeScreen/PlatformMapView.android.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.developer.ev2z.data.local.model.Device
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
) {
    val defaultLocation = LatLng(28.6139, 77.2090)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = false)
    ) {
        devices.forEach { device ->
            device.getLatitudeDouble()?.let { lat ->
                device.getLongitudeDouble()?.let { lng ->
                    val markerColor = when (device.status) {
                        "moving" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                        "idle" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_YELLOW
                        "offline" -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                        else -> com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                    }

                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = device.getDisplayName(),
                        snippet = "${device.getStatusDisplay()} - ${device.speed} km/h",
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(markerColor),
                        onClick = {
                            onMarkerClick(device)
                            true
                        }
                    )
                }
            }
        }
    }
}