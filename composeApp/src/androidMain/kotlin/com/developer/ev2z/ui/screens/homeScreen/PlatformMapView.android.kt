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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.statusColorOf
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.developer.ev2z.ui.components.UserFacingConeMarker
import com.developer.ev2z.ui.screens.device_Report.addressLabelIcon
import com.developer.ev2z.ui.components.vehicleArrowIcon
import com.developer.ev2z.ui.components.markerRotationDegrees
import com.developer.ev2z.ui.components.vehicleMarkerIcon
import com.developer.ev2z.ui.components.vehicleTopViewFor
import com.developer.ev2z.util.rememberDeviceFacing
import com.google.maps.android.compose.*

/*
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
    // the viewport belongs to the user: positions refresh on every poll, and
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
    // map on every poll, which is the follow-the-vehicle behaviour this screen does not
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
*/


@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
) {
    var isMapLoaded by remember { mutableStateOf(false) }
    var hasFittedCamera by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    val context = LocalContext.current

    // Where the phone is and which way its holder is facing, for the compass cone
    // below. Null with location permission refused or before the first reading, which
    // is also what turns the map's own blue dot off — there is nothing to show.
    val facing = rememberDeviceFacing()

    // Drawn positions and headings, walking towards the polled ones instead of
    // snapping to them. Updated every animation frame — read inside the GoogleMap
    // content lambda below, so only the markers recompose per frame.
    val smoothedPositions = rememberSmoothDevicePositions(devices)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(22.9734, 78.6569),
            5f
        )
    }


    // Derived once per fleet response, not on every recomposition: this list is read
    // by the effects below and by the marker loop, and recomputing it per recomposition
    // also reprinted the whole fleet to logcat each time.
    val locatedDevices = remember(devices) {
        devices.mapNotNull { device ->

            val lat = device.getLatitudeDouble()
            val lng = device.getLongitudeDouble()

            println(
                """
        ========== EV2Z DEVICE LOCATION ==========
        Device ID : ${device.deviceId}
        Device    : ${device.getDisplayName()}
        Raw Lat   : ${device.latitude}
        Raw Lng   : ${device.longitude}
        Parsed Lat: $lat
        Parsed Lng: $lng
        Status    : ${device.getStatusKind()}
        ==========================================
        """.trimIndent()
            )

            if (lat == null || lng == null) {
                println("❌ Invalid location: latitude or longitude is null")
                return@mapNotNull null
            }

            if (!lat.isFinite() || !lng.isFinite()) {
                println("❌ Invalid location: coordinates are not finite")
                return@mapNotNull null
            }

            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                println("❌ Invalid location: coordinates out of range")
                return@mapNotNull null
            }

            if (lat == 0.0 && lng == 0.0) {
                println("⚠️ Skipping (0,0): check whether API uses it as an invalid GPS fix")
                return@mapNotNull null
            }

            println("✅ Valid location: $lat, $lng")

            Triple(device, lat, lng)
        }
    }

    println("📍 Total devices from API: ${devices.size}")
    println("📍 Devices with valid coordinates: ${locatedDevices.size}")


    // Fit the camera to the first valid API coordinates only.
    //
    // Keyed on the coordinates as well as the map, so a fleet that arrives *after* the
    // map has loaded still gets fitted. This used to wait on
    // `snapshotFlow { locatedDevices }.first { it.isNotEmpty() }`, but that lambda reads
    // a plain local list rather than snapshot state: the flow emitted the value captured
    // when the effect launched and never again, so an empty first batch left the camera
    // parked on the India-wide view for the rest of the screen's life however many
    // polls later the positions turned up.
    LaunchedEffect(isMapLoaded, locatedDevices) {
        if (!isMapLoaded || hasFittedCamera || locatedDevices.isEmpty()) {
            return@LaunchedEffect
        }

        val coordinates = locatedDevices.map { (_, lat, lng) ->
            LatLng(lat, lng)
        }

        try {
            val cameraUpdate = if (coordinates.size == 1) {
                CameraUpdateFactory.newLatLngZoom(
                    coordinates.first(),
                    16f
                )
            } else {
                val bounds = LatLngBounds.Builder().apply {
                    coordinates.forEach { include(it) }
                }.build()

                CameraUpdateFactory.newLatLngBounds(bounds, 100)
            }

            cameraPositionState.animate(
                update = cameraUpdate,
                durationMs = 1000
            )

            hasFittedCamera = true
        } catch (e: Exception) {
            println("Camera fit error: ${e.message}")
        }
    }

    // Zoom to a selected vehicle using its latest available coordinates.
    LaunchedEffect(isMapLoaded, selectedDevice?.deviceId) {
        if (!isMapLoaded) return@LaunchedEffect

        val selectedId = selectedDevice?.deviceId
            ?: return@LaunchedEffect

        val selected = locatedDevices.firstOrNull {
            it.first.deviceId == selectedId
        } ?: return@LaunchedEffect

        val (_, lat, lng) = selected

        try {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(lat, lng),
                    16f
                ),
                durationMs = 800
            )
        } catch (e: Exception) {
            println("Selected vehicle zoom error: ${e.message}")
        }
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
                mapType = mapType
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                myLocationButtonEnabled = false
            ),
            onMapLoaded = {
                isMapLoaded = true
            }
        ) {
            UserFacingConeMarker(facing)

            locatedDevices.forEach { (device, lat, lng) ->
                key(device.deviceId) {
                    val statusKind = device.getStatusKind()

                    // The vehicle the server says this tracker is fitted to, from
                    // `vehicle_id` where the fleet payload carries one — it gained
                    // vehicle_id/vehicle_name/icon/icon_url on 2026-10-13, before which
                    // every marker here was an arrow for want of the field. Still null
                    // on a device with no vehicle assigned, and that falls back to the
                    // heading arrow so it at least shows which way it is pointing.
                    val vehicle = vehicleTopViewFor(device.iconSlug)

                    // Rasterised only when the vehicle or its state changes: the marker
                    // below moves on every animation frame, and drawing a bitmap per
                    // frame would allocate one 60 times a second. The artwork itself
                    // never changes with the heading — it is drawn from above, so the
                    // marker is simply turned, see VehicleTopView.
                    //
                    // Null if the Maps SDK cannot supply a descriptor, which falls back
                    // to the default pin rather than taking the screen down.
                    val markerIcon = remember(vehicle, statusKind) {
                        val color = statusColorOf(statusKind).toInt()
                        if (vehicle == null) vehicleArrowIcon(context, color)
                        else vehicleMarkerIcon(context, vehicle, color)
                    }

                    val address = rememberDeviceAddress(device)

                    // The glided position when there is one; the raw coordinates on the
                    // very first frame, before the animation has seeded itself.
                    val drawn = smoothedPositions[device.deviceId]

                    // How old this device's fix is, which the fleet payload reports per
                    // device and this map used to drop on the floor: two of three pins
                    // can be days old and looked exactly as live as the one moving.
                    val fixAge = device.fixAgeLabel()

                    Marker(
                        // rememberUpdatedMarkerState pushes each frame's position into
                        // the one remembered state, which is what actually walks the
                        // marker across the map. Constructing MarkerState inline here
                        // would throw the state away on every recomposition.
                        state = rememberUpdatedMarkerState(
                            position = drawn
                                ?.let { LatLng(it.latitude, it.longitude) }
                                ?: LatLng(lat, lng)
                        ),
                        title = device.getDisplayName(),
                        // Status and speed first, then where, then how long ago — a
                        // parked vehicle's address means something quite different if
                        // the tracker stopped reporting last Tuesday.
                        snippet = buildString {
                            append(device.getStatusDisplay())
                            if (device.speed > 0) append(" · ${device.speed} km/h")
                            if (address != null) append(" · $address")
                            if (fixAge != null) append(" · $fixAge")
                        },
                        icon = markerIcon
                            ?: BitmapDescriptorFactory.defaultMarker(
                                when (statusKind) {
                                    "running" -> BitmapDescriptorFactory.HUE_GREEN
                                    "idle" -> BitmapDescriptorFactory.HUE_YELLOW
                                    "parked", "stopped" -> BitmapDescriptorFactory.HUE_RED
                                    else -> BitmapDescriptorFactory.HUE_AZURE
                                }
                            ),
                        // Turns to the direction it is travelling in, so a vehicle
                        // crossing the map faces along the road rather than sliding
                        // sideways down it. The heading as it is, since the artwork is
                        // drawn from above and facing up — markerRotationDegrees only
                        // absorbs the NaN of a marker that has not travelled yet, which
                        // Marker will not take.
                        //
                        // Left upright on the fallback pin: a balloon turned to 137°
                        // reads as a pin falling over, not as a vehicle heading
                        // south-east.
                        rotation = if (markerIcon == null) 0f
                        else markerRotationDegrees(
                            drawn?.bearingDegrees ?: Double.NaN
                        ).toFloat(),
                        // Centred on the coordinate, not hanging above it by a pin's
                        // height: the vehicle is *at* the point. The fallback pin is a
                        // balloon and has to keep hanging by its tip.
                        anchor = if (markerIcon != null) Offset(0.5f, 0.5f)
                        else Offset(0.5f, 1f),
                        // Lies on the map rather than facing the screen, so it keeps
                        // pointing along its road when the user turns the map with two
                        // fingers. A billboard marker would hold its screen angle and
                        // end up pointing across the buildings.
                        flat = markerIcon != null,
                        onClick = {
                            onMarkerClick(device)
                            false
                        }
                    )

                    // The name chip: on the selected vehicle, or on all of them while
                    // nothing is selected.
                    //
                    // Selected-only on its own is what this shipped as, and on this
                    // screen that meant *never*: HomeViewModel starts with no selection
                    // and only ever sets one when a device is tapped, so opening the
                    // live map showed no address anywhere. Nothing selected means the
                    // user has not narrowed it down, so every vehicle answers; picking
                    // one quiets the rest, which is what keeps a large fleet from
                    // stacking chips on top of each other.
                    //
                    // Its own marker rather than text baked into the vehicle bitmap:
                    // the vehicle turns to its heading, and text inside it would be
                    // upside down every time the vehicle drove south.
                    val labelled = selectedDevice == null ||
                            device.deviceId == selectedDevice.deviceId
                    if (labelled) {
                        // The registration, not the street: this is the fleet map, and
                        // with no vehicle assigned to any tracker — `vehicle_id` is null
                        // for every device on some accounts — all three pins are the
                        // same grey arrow. The chip is the only thing that says which
                        // one is which. The address is one tap away in the callout.
                        val chipText = device.mapLabel()
                        val labelIcon = remember(chipText, statusKind) {
                            addressLabelIcon(chipText, statusColorOf(statusKind).toInt())
                        }
                        Marker(
                            state = rememberUpdatedMarkerState(
                                position = drawn
                                    ?.let { LatLng(it.latitude, it.longitude) }
                                    ?: LatLng(lat, lng)
                            ),
                            icon = labelIcon,
                            // Bottom centre: the bitmap carries the chip plus the
                            // transparent gap under it, so it hangs above the vehicle.
                            anchor = Offset(0.5f, 1f),
                            // Screen-facing, unlike the vehicle under it: this is text,
                            // and it has to stay upright however the map is turned.
                            flat = false,
                            zIndex = 1f,
                            // The chip is a label, not a target — a tap on it should
                            // select the vehicle it belongs to, same as the vehicle.
                            onClick = {
                                onMarkerClick(device)
                                false
                            }
                        )
                    }
                }
            }
        }

        // Normal / Hybrid map toggle.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 110.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(
                    1.dp,
                    Color(0xFFE0E0E0),
                    RoundedCornerShape(10.dp)
                )
                .clickable {
                    mapType = if (mapType == MapType.NORMAL) {
                        MapType.HYBRID
                    } else {
                        MapType.NORMAL
                    }
                }
                .padding(12.dp)
        ) {
            Text(
                text = if (mapType == MapType.NORMAL) {
                    "Satellite"
                } else {
                    "Normal Map"
                },
                color = Color.Black,
                fontSize = 14.sp
            )
        }
    }
}
