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
import com.developer.ev2z.data.local.model.statusColorOf
import com.developer.ev2z.ui.screens.device_Report.LABEL_LIFT_POINTS
import com.developer.ev2z.ui.screens.device_Report.buildAddressLabelImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import com.developer.ev2z.ui.components.AddressChip
import com.developer.ev2z.ui.components.AddressChipAnnotations
import com.developer.ev2z.ui.components.UserFacingCone
import com.developer.ev2z.ui.components.VehicleAnnotations
import com.developer.ev2z.ui.components.VehicleMarker
import com.developer.ev2z.ui.components.userFacingImage
import com.developer.ev2z.ui.components.vehicleTopViewFor
import com.developer.ev2z.util.rememberDeviceFacing
import platform.MapKit.*
import platform.darwin.NSObject

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

    // Where the phone is and which way its holder is facing, for the compass cone —
    // which is about the person, not the vehicles. Null with location permission
    // refused, on a device with no magnetometer, and before the first reading.
    val facing = rememberDeviceFacing()
    val cone = remember { UserFacingCone(userFacingImage()) }

    // One annotation per device, kept and moved rather than replaced, each turned to
    // the direction its vehicle is travelling in.
    val vehicles = remember { VehicleAnnotations() }

    // The address chips.
    //
    // On the selected vehicle, or on all of them while nothing is selected — this
    // shipped as selected-only, and on this screen that meant *never*: HomeViewModel
    // starts with no selection and only sets one when a device is tapped, so the live
    // map showed no address anywhere. Nothing selected means the user has not narrowed
    // it down, so every vehicle answers; picking one quiets the rest.
    val chips = remember { AddressChipAnnotations() }

    // This map had no delegate at all, so every pin was MapKit's default — a red
    // balloon that says nothing about which way the vehicle is going. The cone and the
    // vehicles draw their own views now; anything else still falls through to null,
    // which is how MapKit is told to carry on drawing its own.
    val mapDelegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(
                mapView: MKMapView,
                viewForAnnotation: MKAnnotationProtocol
            ): MKAnnotationView? {
                cone.annotationView(mapView, viewForAnnotation)?.let { return it }


                chips.annotationView(mapView, viewForAnnotation)?.let { return it }
                return vehicles.annotationView(mapView, viewForAnnotation)
            }

            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                // Both are drawn at screen angles, so both have to be turned back when
                // the map itself turns — see VehicleAnnotations.reapplyRotations.
                cone.reapplyRotation(mapView)
                vehicles.reapplyRotations(mapView)
            }
        }
    }

    // Drawn positions and headings, gliding towards the polled ones instead of
    // snapping. Updated every animation frame.
    val smoothedPositions = rememberSmoothDevicePositions(devices)

    // Resolved up here, not in the update block below: that block is a plain lambda
    // handed to UIKitView, so it cannot call a composable. Tapping a pin shows the
    // address when there is one, which is the question a pin tap is really asking.
    val addresses = rememberDeviceAddresses(devices)

    // Rasterised once per address, not once per frame: this block re-runs on every
    // poll, and an address only changes when the vehicle leaves an ~11 m bucket.
    val chipImages = remember(addresses, devices, selectedDevice?.deviceId) {
        devices.mapNotNull { device ->
            val labelled = selectedDevice == null || device.deviceId == selectedDevice.deviceId
            if (!labelled) return@mapNotNull null
            val text = addresses[device.deviceId]?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val image = buildAddressLabelImage(text, statusColorOf(device.getStatusKind()))
                ?: return@mapNotNull null
            device.deviceId to image
        }.toMap()
    }

    // Auto-fit runs once, on the first coordinates to arrive; after that the viewport
    // is the user's. Positions refresh on every poll, and re-fitting on each tick
    // would wipe out their pan and zoom while they are looking at it.
    val hasFittedRegion = remember { booleanArrayOf(false) }
    // The device the region was last moved to, so a position refresh on the same
    // selection doesn't re-centre the map every tick.
    val centredOnDeviceId = remember { arrayOfNulls<String>(1) }

    Box(modifier = modifier) {

        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.delegate = mapDelegate
                mapView
            },
            update = { view ->
                // ✅ mapType update
                view.setMapType(
                    if (isHybrid) MKMapTypeHybrid else MKMapTypeStandard
                )

                // MapKit's own user-location view stays off: it draws a large
                // translucent accuracy circle along with its dot, and the two cannot be
                // separated. The cone carries the dot instead, circle and all gone.
                view.setShowsUserLocation(false)
                cone.update(view, facing)

            // Every vehicle with a usable fix, as the map needs it. Pins for devices
            // that have left the account or lost their fix are retired by update().
            val markers = devices.mapNotNull { device ->
                // The glided position when there is one; the raw coordinates on the
                // very first frame, before the animation has seeded itself.
                val drawn = smoothedPositions[device.deviceId]
                val latitude = drawn?.latitude ?: device.getLatitudeDouble()
                    ?: return@mapNotNull null
                val longitude = drawn?.longitude ?: device.getLongitudeDouble()
                    ?: return@mapNotNull null

                VehicleMarker(
                    deviceId = device.deviceId,
                    latitude = latitude,
                    longitude = longitude,
                    bearingDegrees = drawn?.bearingDegrees ?: Double.NaN,
                    // From `vehicle_id` where the fleet payload carries one — it
                    // gained that field on 2026-10-13, before which every marker here
                    // was an arrow. Null on a device with no vehicle assigned, which
                    // draws the heading arrow so it still shows which way it points.
                    vehicle = vehicleTopViewFor(device.iconSlug),
                    statusKind = device.getStatusKind(),
                    title = device.getDisplayName(),
                    subtitle = addresses[device.deviceId]
                        ?: "${device.getStatusDisplay()} - ${device.speed} km/h",
                )
            }
            vehicles.update(view, markers)

            // The chips follow their vehicles every frame, and come and go with the
            // address and the selection.
            chips.update(
                view,
                markers.mapNotNull { marker ->
                    val image = chipImages[marker.deviceId] ?: return@mapNotNull null
                    AddressChip(
                        deviceId = marker.deviceId,
                        latitude = marker.latitude,
                        longitude = marker.longitude,
                        image = image,
                        liftPoints = LABEL_LIFT_POINTS,
                    )
                }
            )

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
