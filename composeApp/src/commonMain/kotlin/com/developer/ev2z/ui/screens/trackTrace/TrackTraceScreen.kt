package com.developer.ev2z.ui.screens.trackTrace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Device
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.ui.screens.device_Report.PlatformMapView
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.text.ifEmpty

// ── Theme Colors (Light Green — App Theme) ───────────────────────
private val TT_Primary = Color(0xFF6A9F50)
private val TT_PrimaryDark = Color(0xFF4A7D39)
private val TT_PrimaryBg = Color(0xFFF0F7EC)
private val TT_Background = Color(0xFFF8FAF0)
private val TT_Surface = Color(0xFFFFFFFF)
private val TT_SectionBg = Color(0xFFF3F6F0)
private val TT_Border = Color(0xFFE2E8DD)
private val TT_TextPrimary = Color(0xFF1C2418)
private val TT_TextSec = Color(0xFF5D6858)
private val TT_TextHint = Color(0xFFA0A99B)
private val TT_Running = Color(0xFF85B070)
private val TT_Idle = Color(0xFFF4C44E)
private val TT_Stopped = Color(0xFFE5565B)

private fun ttStatusColor(status: String) = when (status) {
    "on", "moving" -> TT_Running
    "Idle", "idle" -> TT_Idle
    "off", "parked", "offline" -> TT_Stopped
    else -> Color.Gray
}

private fun ttStatusLabel(status: String) = when (status) {
    "on", "moving" -> "Running"
    "Idle", "idle" -> "Idle"
    "off" -> "Stopped"
    "parked" -> "Parked"
    "offline" -> "Offline"
    else -> status.replaceFirstChar { it.uppercase() }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TrackTraceScreen
//  liveLocation + trailPoints collected ONLY inside StableTrackMapSection
//  so location polling never recomposes the top-level screen.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TrackTraceScreen(
    onBack: () -> Unit
) {
    val viewModel = TrackTraceViewModelProvider.viewModel

    val devices by viewModel.devices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val username by viewModel.username.collectAsState()

    PlatformBackHandler(true) { onBack() }

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TT_Background)
    ) {
        // ── Map (isolated recomposition scope) ──────────────────────
        StableTrackMapSection(viewModel = viewModel)

        // ── Top overlay: Back + Title + Device card ─────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + 8.dp,
                    start = 14.dp,
                    end = 14.dp
                )
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Surface(
                    shape = CircleShape,
                    color = TT_Surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = TT_TextPrimary
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Title chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TT_Surface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            tint = TT_Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Track & Trace",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TT_TextPrimary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Device selector card
            DeviceDropdownCardSimple(
                userName = username.ifEmpty { "User" },
                devices = devices,
                selectedDevice = selectedDevice,
                onDeviceSelect = viewModel::selectDevice
            )
        }

        // ── Bottom info chip (speed + status) ───────────────────────
        BottomInfoChip(
            viewModel = viewModel,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 14.dp,
                    end = 14.dp,
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 16.dp
                )
        )

        // ── Floating contact ────────────────────────────────────────
        FloatingContactButton(
            phoneNumber = "919755273822",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 14.dp,
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 72.dp
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  StableTrackMapSection
//  liveLocation & trailPoints collected HERE only — never in the parent scope
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StableTrackMapSection(viewModel: TrackTraceViewModel) {
    val liveLocation by viewModel.liveLocation.collectAsState()
    val trailPoints by viewModel.trailPoints.collectAsState()

    val lat = liveLocation?.data?.latestLocation?.latitude?.toDoubleOrNull()
    val lng = liveLocation?.data?.latestLocation?.longitude?.toDoubleOrNull()

    if (lat != null && lng != null) {
        val (animatedLat, animatedLng) = rememberAnimatedLatLng(lat, lng)

        PlatformMapView(
            modifier = Modifier.fillMaxSize(),
            latitude = animatedLat,
            longitude = animatedLng,
            zoom = 15.0,
            trailPoints = trailPoints
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BottomInfoChip — shows speed + status, only recomposes on location changes
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BottomInfoChip(
    viewModel: TrackTraceViewModel,
    modifier: Modifier = Modifier
) {
    val liveLocation by viewModel.liveLocation.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val trailPoints by viewModel.trailPoints.collectAsState()

    val latest = liveLocation?.data?.latestLocation
    val speed = latest?.speed?.toString() ?: "0"
    val status = latest?.status ?: "offline"
    val statusClr = ttStatusColor(status)
    val deviceName = selectedDevice?.device_name ?: "—"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TT_Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device name + status
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TT_TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusClr.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 3.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(statusClr, CircleShape)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                ttStatusLabel(status),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = statusClr
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Trail points count
                    Text(
                        "${trailPoints.size} points",
                        fontSize = 11.sp,
                        color = TT_TextHint
                    )
                }
            }

            // Speed display
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = TT_SectionBg
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        speed,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TT_TextPrimary
                    )
                    Text(
                        "km/h",
                        fontSize = 10.sp,
                        color = TT_TextHint
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DeviceDropdownCardSimple — responsive device selector with app theme
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TT_Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(14.dp)) {

            // Header row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TT_TextPrimary
                    )
                    Text(
                        text = "${devices.size} ${if (devices.size == 1) "Device" else "Devices"}",
                        fontSize = 12.sp,
                        color = TT_TextHint
                    )
                }

                // LIVE indicator badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TT_PrimaryBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .background(TT_Primary, CircleShape)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TT_Primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Device selector row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSingleDevice) {
                        if (!isSingleDevice) expanded = true
                    },
                shape = RoundedCornerShape(14.dp),
                color = TT_SectionBg
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = selectedDevice?.device_name ?: "Select Device",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TT_TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isSingleDevice) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Device",
                            tint = TT_TextHint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dropdown menu (multi-device only)
            if (!isSingleDevice) {
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        surface = TT_Surface,
                        surfaceContainer = TT_Surface
                    )
                ) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        devices.forEach { device ->
                            val statusClr = ttStatusColor(
                                device.engine_status ?: "offline"
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .background(statusClr, CircleShape)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                device.device_name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TT_TextPrimary
                                            )
                                            Text(
                                                "ID: ${device.device_id}",
                                                fontSize = 11.sp,
                                                color = TT_TextHint
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusClr.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                ttStatusLabel(
                                                    device.engine_status ?: "offline"
                                                ),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = statusClr,
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    expanded = false
                                    onDeviceSelect(device)
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
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
        targetValue = latitude.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "latAnimation"
    )

    val animatedLng by animateFloatAsState(
        targetValue = longitude.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "lngAnimation"
    )

    return animatedLat.toDouble() to animatedLng.toDouble()
}