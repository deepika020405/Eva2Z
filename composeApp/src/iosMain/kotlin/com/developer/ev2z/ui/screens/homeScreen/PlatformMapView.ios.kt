// iosMain/ui/screens/homeScreen/PlatformMapView.ios.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.model.Device
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
) {
    val mapView = remember { MKMapView() }
    var isHybrid by remember { mutableStateOf(false) } // ✅ ADD

    // Drawn positions, gliding towards the polled ones over ~1.5s instead of
    // snapping. Updated every animation frame.
    val smoothedPositions = rememberSmoothDevicePositions(devices)

    // One annotation per device, kept and moved rather than replaced. Removing and
    // re-adding every pin is what made the marker breed instead of move, and at
    // animation speed it would also flicker the whole map.
    val annotations = remember { mutableMapOf<String, MKPointAnnotation>() }

    // Resolved up here, not in the update block below: that block is a plain lambda
    // handed to UIKitView, so it cannot call a composable. Tapping a pin shows the
    // address when there is one, which is the question a pin tap is really asking.
    val addresses = rememberDeviceAddresses(devices)

    // Auto-fit runs once, on the first coordinates to arrive; after that the viewport
    // is the user's. Positions refresh every 15s now, and re-fitting on each tick
    // would wipe out their pan and zoom while they are looking at it.
    val hasFittedRegion = remember { booleanArrayOf(false) }
    // The device the region was last moved to, so a position refresh on the same
    // selection doesn't re-centre the map every tick.
    val centredOnDeviceId = remember { arrayOfNulls<String>(1) }

    Box(modifier = modifier) {

        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.setShowsUserLocation(false)
                mapView
            },
            update = { view ->
                // ✅ mapType update
                view.setMapType(
                    if (isHybrid) MKMapTypeHybrid else MKMapTypeStandard
                )

            // Retire pins for devices that have left the account or lost their fix.
            val liveIds = devices.map { it.deviceId }.toSet()
            annotations.keys.toList().forEach { id ->
                if (id !in liveIds) {
                    annotations.remove(id)?.let { view.removeAnnotation(it) }
                }
            }

            // Move the existing pin, or add one the first time this device is seen.
            devices.forEach { device ->
                val position = smoothedPositions[device.deviceId]
                    ?: device.getLatitudeDouble()?.let { lat ->
                        device.getLongitudeDouble()?.let { lng -> lat to lng }
                    }
                    ?: return@forEach

                val coordinate = CLLocationCoordinate2DMake(position.first, position.second)
                val existing = annotations[device.deviceId]

                val subtitle = addresses[device.deviceId]
                    ?: "${device.getStatusDisplay()} - ${device.speed} km/h"

                if (existing == null) {
                    val annotation = MKPointAnnotation()
                    annotation.setCoordinate(coordinate)
                    annotation.setTitle(device.getDisplayName())
                    annotation.setSubtitle(subtitle)
                    annotations[device.deviceId] = annotation
                    view.addAnnotation(annotation)
                } else {
                    existing.setCoordinate(coordinate)
                    existing.setSubtitle(subtitle)
                }
            }

            // Zoom to show all devices — first batch only
            if (!hasFittedRegion[0]) {
                var minLat = Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE
                var minLng = Double.MAX_VALUE
                var maxLng = -Double.MAX_VALUE
                var located = 0

                devices.forEach { device ->
                    device.getLatitudeDouble()?.let { lat ->
                        device.getLongitudeDouble()?.let { lng ->
                            minLat = minOf(minLat, lat)
                            maxLat = maxOf(maxLat, lat)
                            minLng = minOf(minLng, lng)
                            maxLng = maxOf(maxLng, lng)
                            located++
                        }
                    }
                }

                if (located > 0) {
                    hasFittedRegion[0] = true

                    val centerLat = (minLat + maxLat) / 2
                    val centerLng = (minLng + maxLng) / 2
                    // A single device spans zero degrees, which would ask MapKit for an
                    // infinite zoom — give it a real window instead.
                    val spanLat = ((maxLat - minLat) * 1.3).takeIf { it > 0.0 } ?: 0.02
                    val spanLng = ((maxLng - minLng) * 1.3).takeIf { it > 0.0 } ?: 0.02

                    val coordinate = CLLocationCoordinate2DMake(centerLat, centerLng)
                    val span = MKCoordinateSpanMake(spanLat, spanLng)
                    val region = MKCoordinateRegionMake(coordinate, span)
                    view.setRegion(region, animated = true)
                }
            }

            // Zoom to selected device if any — only when the selection itself changed
            selectedDevice?.let { device ->
                if (centredOnDeviceId[0] == device.deviceId) return@let
                device.getLatitudeDouble()?.let { lat ->
                    device.getLongitudeDouble()?.let { lng ->
                        centredOnDeviceId[0] = device.deviceId
                        val coordinate = CLLocationCoordinate2DMake(lat, lng)
                        val span = MKCoordinateSpanMake(0.01, 0.01) // Closer zoom
                        val region = MKCoordinateRegionMake(coordinate, span)
                        view.setRegion(region, animated = true)
                    }
                }
            }
        }
    )


        // ✅ Toggle Button
        // ✅ Small toggle button — iOS
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 110.dp, end = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .clickable { isHybrid = !isHybrid }
                .padding(8.dp)
        ) {
            Text(
                text = if (!isHybrid) "🛰️" else "🗺️",
                fontSize = 18.sp
            )
        }
    }
}
