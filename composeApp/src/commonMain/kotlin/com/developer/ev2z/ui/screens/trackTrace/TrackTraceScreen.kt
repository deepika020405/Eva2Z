package com.developer.ev2z.ui.screens.trackTrace

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Device
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.ui.screens.device_Report.PlatformMapView
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.text.ifEmpty

// ─────────────────────────────────────────────────────────────────────────────
//  TrackTraceScreen
//  FIX: liveLocation + trailPoints collected ONLY inside StableTrackMapSection
//       so location polling never recomposes the top-level screen.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackTraceScreen(
    onBack: () -> Unit
) {
    val viewModel = TrackTraceViewModelProvider.viewModel

    // ── Only device-list state collected here — location polling doesn't
    //    touch this scope at all.
    val devices        by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val username       by viewModel.username.collectAsState()

    PlatformBackHandler(true) { onBack() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    Scaffold(
        containerColor = Color(0xFFFFFFFF),
        topBar = {
            TopAppBar(
                title = { Text("Track & Trace") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            // ── FIX: Map + animation + trail isolated here.
            //         Only this composable recomposes on every location poll.
            StableTrackMapSection(viewModel = viewModel)

            // ── Device selector — never recomposed by location updates ───────
            DeviceDropdownCardSimple(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                userName       = username.ifEmpty { "User" },
                devices        = devices,
                selectedDevice = selectedDevice,
                onDeviceSelect = viewModel::selectDevice
            )

            FloatingContactButton(
                phoneNumber = "919755273822",
                modifier    = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  StableTrackMapSection
//  - liveLocation & trailPoints collected HERE only
//  - animation lives here — never in the parent scope
//  - PlatformMapView receives stable inputs
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StableTrackMapSection(viewModel: TrackTraceViewModel) {

    val liveLocation by viewModel.liveLocation.collectAsState()
    val trailPoints  by viewModel.trailPoints.collectAsState()

    val lat = liveLocation?.data?.latestLocation?.latitude?.toDoubleOrNull()
    val lng = liveLocation?.data?.latestLocation?.longitude?.toDoubleOrNull()

    if (lat != null && lng != null) {

        // rememberAnimatedLatLng — smooth camera movement between polls
        val (animatedLat, animatedLng) = rememberAnimatedLatLng(lat, lng)

        PlatformMapView(
            modifier    = Modifier.fillMaxSize(),
            latitude    = animatedLat,
            longitude   = animatedLng,
            zoom        = 15.0,
            trailPoints = trailPoints
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DeviceDropdownCardSimple — unchanged logic
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DeviceDropdownCardSimple(
    modifier: Modifier = Modifier,
    userName: String,
    devices: List<Device>,
    selectedDevice: Device?,
    onDeviceSelect: (Device) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isSingleDevice = devices.size <= 1

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = userName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1
                )
                Text(
                    text     = "${devices.size} Devices",
                    fontSize = 12.sp,
                    color    = Color.Gray
                )
            }

            Spacer(Modifier.height(12.dp))

            // Device selector row
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F4F4), RoundedCornerShape(12.dp))
                    .clickable(enabled = !isSingleDevice) {
                        if (!isSingleDevice) expanded = true
                    }
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.weight(1f)
                ) {
                    Image(
                        painter            = painterResource(Res.drawable.app_logo),
                        contentDescription = null,
                        modifier           = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = selectedDevice?.device_name ?: "Select Device",
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                if (!isSingleDevice) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            // Dropdown menu (multi-device only)
            if (!isSingleDevice) {
                DropdownMenu(
                    expanded          = expanded,
                    onDismissRequest  = { expanded = false }
                ) {
                    devices.forEach { device ->
                        DropdownMenuItem(
                            text    = { Text(device.device_name) },
                            onClick = {
                                expanded = false
                                onDeviceSelect(device)
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  rememberAnimatedLatLng — smooth lat/lng animation helper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun rememberAnimatedLatLng(
    latitude: Double,
    longitude: Double
): Pair<Double, Double> {

    val animatedLat by animateFloatAsState(
        targetValue   = latitude.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label         = "latAnimation"
    )

    val animatedLng by animateFloatAsState(
        targetValue   = longitude.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label         = "lngAnimation"
    )

    return animatedLat.toDouble() to animatedLng.toDouble()
}


