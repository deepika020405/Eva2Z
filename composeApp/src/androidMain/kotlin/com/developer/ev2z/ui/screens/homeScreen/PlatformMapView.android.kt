// androidMain/ui/screens/homeScreen/PlatformMapView.android.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    val cameraPositionState = rememberCameraPositionState()
    val indiaLatLng = LatLng(22.9734, 78.6569)

    // Initial camera position - will be updated when map loads
    LaunchedEffect(Unit, devices) {
        val firstValidDevice = devices.firstOrNull { it.hasValidLocation() }

        cameraPositionState.position =
            if (firstValidDevice != null) {
                CameraPosition.fromLatLngZoom(
                    LatLng(
                        firstValidDevice.getLatitudeDouble()!!,
                        firstValidDevice.getLongitudeDouble()!!
                    ),
                    10f
                )
            } else {
                // 🇮🇳 No device / no valid location
                CameraPosition.fromLatLngZoom(indiaLatLng, 4f)
            }
    }

    // Zoom to all devices when map is loaded
    LaunchedEffect(isMapLoaded, devices) {
        if (isMapLoaded && devices.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.Builder()
            var hasValidLocation = false

            devices.forEach { device ->
                device.getLatitudeDouble()?.let { lat ->
                    device.getLongitudeDouble()?.let { lng ->
                        boundsBuilder.include(LatLng(lat, lng))
                        hasValidLocation = true
                    }
                }
            }

            if (hasValidLocation) {
                try {
                    val bounds = boundsBuilder.build()
                    val padding = 100
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, padding),
                        durationMs = 1000
                    )
                } catch (e: Exception) {
                    println("❌ Error zooming to bounds: ${e.message}")
                }
            }
        }
    }

    // Zoom to selected device
    LaunchedEffect(isMapLoaded, selectedDevice) {
        if (isMapLoaded) {
            selectedDevice?.let { device ->
                device.getLatitudeDouble()?.let { lat ->
                    device.getLongitudeDouble()?.let { lng ->
                        try {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f),
                                durationMs = 1000
                            )
                        } catch (e: Exception) {
                            println("❌ Error zooming to device: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(zoomControlsEnabled = true),
        onMapLoaded = {
            println("✅ Google Map loaded successfully")
            isMapLoaded = true
        }
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
                            false
                        }
                    )
                }
            }
        }
    }
}