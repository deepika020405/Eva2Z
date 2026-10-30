package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.data.local.DeviceTypeStore
import com.developer.ev2z.data.local.isWirelessDeviceType
import com.developer.ev2z.data.local.model.isMotionKind
import com.developer.ev2z.data.local.model.statusDisplayOf
import com.developer.ev2z.data.local.model.statusKindOf
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.markerVehicleSlug
import com.developer.ev2z.response.DeviceAnalyticsData
import com.developer.ev2z.ui.screens.trackTrace.rememberAnimatedLatLng
import com.developer.ev2z.util.Analytics
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import kotlin.math.min
import kotlin.math.roundToInt
import com.developer.ev2z.theme.AppPageBg


data class DeviceItem(
    val name: String,
    val deviceId: String,
    val status: String
)

// ── Theme Colors (Light Green) ───────────────────────────────────
private val DR_Primary = Color(0xFF6A9F50)
private val DR_PrimaryDark = Color(0xFF4A7D39)
private val DR_PrimaryBg = Color(0xFFF0F7EC)
private val DR_Background = AppPageBg
private val DR_Surface = Color(0xFFFFFFFF)
private val DR_SectionBg = Color(0xFFF3F6F0)
private val DR_Border = Color(0xFFE2E8DD)
private val DR_TextPrimary = Color(0xFF1C2418)
private val DR_TextSec = Color(0xFF5D6858)
private val DR_TextHint = Color(0xFFA0A99B)
private val DR_Running = Color(0xFF85B070)
private val DR_Idle = Color(0xFFF4C44E)
private val DR_Stopped = Color(0xFFE5565B)
private val DR_CardShadow = Color(0x12000000)

/**
 * The badge colour, in this screen's palette, for whatever the server called the state.
 *
 * Through [statusKindOf] rather than matching the raw string here. The gateway spells
 * this field a dozen ways — on, off, moving, running, idle, **ideal**, parked, stop,
 * stopped, offline, no_data — and the list this used to match on covered six of them,
 * so a vehicle reported as "running" (rather than "moving") fell through to grey, and
 * so did "stopped" and the server's "ideal" misspelling of idle. Grey next to a moving
 * vehicle reads as a dead tracker.
 */
fun statusColor(status: String) = when (statusKindOf(status)) {
    "running" -> DR_Running
    "idle" -> DR_Idle
    // A stop and a park are the same red here, as they are on the map.
    "parked", "stopped" -> DR_Stopped
    "offline", "no_data" -> Color.Gray
    else -> Color.Gray
}

/**
 * The badge text, from the same vocabulary every other screen reads — so "ideal" shows
 * as "Idle" rather than as "Ideal", and a spelling the app has never seen is shown as
 * the server wrote it instead of hiding behind "Unknown".
 */
fun statusLabel(status: String) = statusDisplayOf(status)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeviceReportScreen(
    viewModel: DeviceReportViewModel,
    token: String,
    username: String,
    onTodayReport: () -> Unit,
    onTrackTrace: () -> Unit,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val myDevices by viewModel.myDevices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val liveLocation by viewModel.liveLocation.collectAsState()
    val totalDevices by viewModel.totalDevices.collectAsState()

    BackPressHandler { onBack() }

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "device_report_screen",
                "screen_class" to "DeviceReportScreen"
            )
        )
        viewModel.fetchMyDevices(token)
    }

    when (uiState) {
        is DeviceReportUiState.Loading -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(DR_Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = DR_Primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Loading devices...",
                        fontSize = 14.sp,
                        color = DR_TextHint
                    )
                }
            }
        }

        is DeviceReportUiState.Error -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(DR_Background),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DR_Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Something went wrong",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DR_TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (uiState as DeviceReportUiState.Error).message,
                            fontSize = 14.sp,
                            color = DR_TextSec,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.refreshData(token) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DR_Primary)
                        ) {
                            Text("Try Again", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }

        is DeviceReportUiState.Success -> {
            DeviceReportContent(
                devices = myDevices,
                selectedDevice = selectedDevice,
                liveLocation = liveLocation,
                totalDevices = totalDevices,
                onDeviceSelected = { device -> viewModel.selectDevice(device, token) },
                onBack = onBack,
                username = username,
                onTodayReport = onTodayReport,
                onTrackTrace = onTrackTrace,
                token = token,
                viewModel = viewModel
            )
        }
    }
}


@Composable
fun DeviceReportContent(
    devices: List<Device>,
    selectedDevice: Device?,
    liveLocation: LiveLocationResponse?,
    totalDevices: Int,
    onDeviceSelected: (Device) -> Unit,
    onBack: () -> Unit,
    onTodayReport: () -> Unit,
    onTrackTrace: () -> Unit,
    username: String,
    token: String,
    viewModel: DeviceReportViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DR_Background)
    ) {
        // Map. validPosition() rather than toDoubleOrNull(): a tracker with no
        // satellite fix reports "0"/"0", which is a real place in the Gulf of Guinea.
        val latestFix = liveLocation?.data?.latestLocation
        val position = latestFix?.validPosition()

        if (position != null) {
            // Each poll is handed to the map as interpolated frames, the same as Track
            // & Trace. Passing the polled coordinates straight through — which this did
            // — teleported the marker every interval instead of driving it.
            val (animatedLat, animatedLng) = rememberAnimatedLatLng(position.first, position.second)
            PlatformMapView(
                modifier = Modifier.fillMaxSize(),
                latitude = animatedLat,
                longitude = animatedLng,
                zoom = 15.0,
                // The same rule as Track & Trace, which this claimed to follow and did
                // not: motionKind(), not statusKind().
                //
                // This endpoint reports a tracker that has not checked in recently as
                // `"status":"offline"` while `"db_status"` still says `"parked"`, and
                // statusKind() prefers `status` — so the vehicle came out grey on the
                // map while the badge inches above it read "Parked" in red. Seen on
                // device 0869727076942661 on 2026-10-13, which is exactly that payload.
                statusKind = latestFix.motionKind().takeIf { isMotionKind(it) }
                    ?: latestFix.statusKind().takeIf { it != "unknown" }
                    ?: statusKindOf(selectedDevice?.engine_status),
                // Both descriptions of this tracker, merged field by field rather than
                // one preferred whole — `live-location` sends no `vehicle_id` and the
                // dropdown's device (from `latest-locations`) is what carries it. See
                // markerVehicleSlug.
                vehicleSlug = markerVehicleSlug(latestFix, selectedDevice)
            )
        }

        // Top Bar
        TopDeviceBoxWithData(
            userName = username,
            deviceCount = totalDevices,
            selectedDevice = selectedDevice?.device_name,
            onBackClick = onBack,
            onDropdownClick = { isDropdownOpen = true },
            onAddClick = { }
        )

        // Device Selector Overlay
        // NOTE: zIndex must be on the AnimatedVisibility itself (a direct child of
        // the outer Box) so the selector draws AND hit-tests above the bottom sheet,
        // which is composed later. Without this the bottom sheet swallows the taps.
        AnimatedVisibility(
            modifier = Modifier.zIndex(2000f),
            visible = isDropdownOpen,
            enter = fadeIn() + slideInVertically { -it / 3 },
            exit = fadeOut() + slideOutVertically { -it / 3 }
        ) {
            DeviceSelectorPanelWithData(
                devices = devices,
                onDeviceSelected = {
                    onDeviceSelected(it)
                    isDropdownOpen = false
                },
                onAddDevice = { },
                onDismiss = { isDropdownOpen = false }
            )
        }

        // Bottom Sheet
        if (selectedDevice != null) {
            DeviceReportBottomSheetWithData(
                device = selectedDevice,
                liveLocation = liveLocation,
                isExpanded = isExpanded,
                token = token,
                onExpandToggle = { isExpanded = !isExpanded },
                onTodayReport = onTodayReport,
                onTrackTrace = onTrackTrace,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun BoxScope.MapMarkerChipWithData(
    deviceName: String,
    speed: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(bottom = 100.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(DR_Primary, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                deviceName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DR_TextPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "$speed km/h",
                fontSize = 12.sp,
                color = DR_TextHint
            )
        }
    }
}

@Composable
fun TopDeviceBoxWithData(
    userName: String,
    deviceCount: Int,
    selectedDevice: String?,
    onBackClick: () -> Unit,
    onDropdownClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val isSingleDevice = deviceCount <= 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = WindowInsets.statusBars
                    .asPaddingValues()
                    .calculateTopPadding() + 8.dp,
                start = 14.dp,
                end = 14.dp
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DR_Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Back button
                Surface(
                    shape = CircleShape,
                    color = DR_SectionBg,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onBackClick() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = DR_TextPrimary
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                val firstName = remember(userName) { extractFirstName(userName) }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = firstName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DR_TextPrimary
                    )
                    Text(
                        text = "$deviceCount ${if (deviceCount == 1) "Device" else "Devices"}",
                        fontSize = 12.sp,
                        color = DR_TextHint
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Device Selector Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSingleDevice) { onDropdownClick() },
                shape = RoundedCornerShape(14.dp),
                color = DR_SectionBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    val deviceText = when {
                        deviceCount == 0 -> "No Devices Added"
                        selectedDevice.isNullOrBlank() -> "Select Device"
                        else -> selectedDevice
                    }

                    Text(
                        text = deviceText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = DR_TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isSingleDevice) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Device",
                            tint = DR_TextHint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

fun extractFirstName(fullName: String): String {
    return fullName
        .trim()
        .substringBefore("(")
        .trim()
        .split(" ")
        .firstOrNull()
        ?: fullName
}

@Composable
fun DeviceSelectorPanelWithData(
    devices: List<Device>,
    onDeviceSelected: (Device) -> Unit,
    onAddDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .zIndex(999f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(
                    top = WindowInsets.statusBars
                        .asPaddingValues()
                        .calculateTopPadding() + 80.dp
                )
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DR_Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Select Device",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DR_TextPrimary
                    )
                    Surface(
                        shape = CircleShape,
                        color = DR_SectionBg,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(16.dp),
                                tint = DR_TextSec
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    "${devices.size} devices available",
                    fontSize = 12.sp,
                    color = DR_TextHint
                )

                Spacer(Modifier.height(16.dp))

                HorizontalDivider(color = DR_Border, thickness = 0.5.dp)

                Spacer(Modifier.height(8.dp))

                Column(Modifier.verticalScroll(rememberScrollState())) {
                    devices.forEach { device ->
                        val statusClr = statusColor(device.engine_status ?: "offline")

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onDeviceSelected(device) },
                            shape = RoundedCornerShape(14.dp),
                            color = DR_SectionBg
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 14.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status indicator
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(statusClr, CircleShape)
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        device.device_name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DR_TextPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "ID: ${device.device_id}",
                                        fontSize = 11.sp,
                                        color = DR_TextHint,
                                        maxLines = 1
                                    )
                                }

                                // Status badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusClr.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = statusLabel(
                                            device.engine_status ?: "offline"
                                        ),
                                        color = statusClr,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 3.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun BoxScope.DeviceReportBottomSheetWithData(
    device: Device,
    liveLocation: LiveLocationResponse?,
    isExpanded: Boolean,
    token: String,
    onExpandToggle: () -> Unit,
    onTodayReport: () -> Unit,
    onTrackTrace: () -> Unit,
    viewModel: DeviceReportViewModel
) {
    val sheetHeight = if (isExpanded) 0.90f else 0.42f

    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 10.dp)
            .padding(
                bottom = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()
            )
            .fillMaxWidth()
            .fillMaxHeight(sheetHeight)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = DR_Surface,
        shadowElevation = 16.dp
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DR_Border)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Report Header
            ReportHeaderWithData(
                deviceName = device.device_name,
                speed = liveLocation?.data?.latestLocation?.speed?.toString() ?: "0",
                // The same two sources, in the same order, as the map marker above:
                // the live fix when there is one, the polled device list until then.
                // Reading only the live fix left the badge on "Unknown" for the first
                // poll and again after any tick that failed, while the marker beside
                // it already knew — one screen disagreeing with itself.
                //
                // motionKind(), not status: this endpoint sends "status":"offline"
                // next to "db_status":"parked" on the same vehicle, so the raw field
                // reports the connection and loses what the vehicle is doing.
                status = liveLocation?.data?.latestLocation?.motionKind()
                    ?.takeIf { it != "unknown" }
                    ?: device.engine_status.ifBlank { "offline" }
            )

            // Device Basic Info
            DeviceBasicInfoWithData(
                liveLocation = liveLocation,
                token = token,
                device = device,
                viewModel = viewModel
            )

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Today's Report
                OutlinedButton(
                    onClick = { onTodayReport() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(DR_Primary, DR_PrimaryDark)
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DR_Primary
                    )
                ) {
                    Text(
                        text = "Today's Report",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Track & Trace
                Button(
                    onClick = { onTrackTrace() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DR_Primary
                    )
                ) {
                    Text(
                        text = "Track & Trace",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = DR_Border,
                thickness = 0.5.dp
            )

            // Device Analysis Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpanded) {
                    Surface(
                        shape = CircleShape,
                        color = DR_SectionBg,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onExpandToggle() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Collapse",
                                modifier = Modifier.size(14.dp),
                                tint = DR_TextPrimary
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    text = "Device Analysis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DR_TextPrimary
                )

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isExpanded) DR_PrimaryBg else DR_SectionBg,
                    modifier = Modifier.clickable { onExpandToggle() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Hide" else "View",
                            color = DR_Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            imageVector = if (isExpanded)
                                Icons.Default.KeyboardArrowDown
                            else
                                Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = DR_Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isExpanded) {
                DeviceAnalysisContentWithData(liveLocation, viewModel, token)
            }
        }
    }
}

@Composable
fun ReportHeaderWithData(
    deviceName: String,
    speed: String,
    status: String
) {
    val color = statusColor(status)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deviceName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = DR_TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            // Status badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusLabel(status),
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Speed display
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DR_SectionBg
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    speed,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DR_TextPrimary
                )
                Text(
                    "km/h",
                    fontSize = 11.sp,
                    color = DR_TextHint
                )
            }
        }
    }
}


@Composable
fun DeviceBasicInfoWithData(
    liveLocation: LiveLocationResponse?,
    device: Device,
    token: String,
    viewModel: DeviceReportViewModel
) {
    val latest = liveLocation?.data?.latestLocation
    val currentStatus = latest?.status ?: ""

    // Goes through the ViewModel's distance throttle, not straight to the geocoder:
    // this fires on every poll, and `latitude.toDouble()` — which this used — throws
    // on a non-numeric field rather than returning null.
    //
    // Skipped entirely when the endpoint sent an address of its own: the ViewModel has
    // already published that one, and geocoding the same fix would overwrite the
    // server's answer with the device's.
    LaunchedEffect(latest?.latitude, latest?.longitude) {
        if (latest?.serverAddress() != null) return@LaunchedEffect
        latest?.validPosition()?.let { (lat, lng) ->
            viewModel.refreshAddressIfMoved(lat, lng)
        }
    }

    val address by viewModel.address.collectAsState()

    // Location card
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        val isCompact = maxWidth < 360.dp
        val labelSize = if (isCompact) 12.sp else 13.sp
        val addressSize = if (isCompact) 14.sp else 15.sp
        val iconBoxSize = if (isCompact) 7.dp else 8.dp
        val iconSize = if (isCompact) 16.dp else 20.dp

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DR_SectionBg
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top row: Icon + "Live Location" label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DR_PrimaryBg
                    ) {
                        Box(
                            modifier = Modifier.padding(iconBoxSize),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DR_Primary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Live Location",
                        fontSize = labelSize,
                        fontWeight = FontWeight.SemiBold,
                        color = DR_TextHint
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Address text — full width, starts from left
                Text(
                    address ?: "Fetching address...",
                    fontSize = addressSize,
                    fontWeight = FontWeight.Normal,
                    color = DR_TextPrimary,
                    lineHeight = (addressSize.value * 1.4f).sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Engine controls
    val backendIsOn = currentStatus == "moving" || currentStatus == "Running"
    var isOnState by remember(currentStatus) { mutableStateOf(backendIsOn) }
    val isTurnOnEnabled = !isOnState
    val isTurnOffEnabled = isOnState

    // Only a wired tracker has a relay in line with the ignition, so Engine On/Off is
    // shown for wired devices and hidden for wireless ones — the command would be
    // accepted and do nothing. The server's type is preferred; the local cache covers a
    // device added on this phone before that call has returned, and a device whose type
    // neither knows keeps the controls rather than losing them to a guess.
    val deviceTypes by viewModel.deviceTypes.collectAsState()
    val deviceType = deviceTypes[device.device_id]
        ?: remember(device.device_id) { DeviceTypeStore.get(device.device_id) }

    if (!isWirelessDeviceType(deviceType)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Engine On
            OutlinedButton(
                onClick = {
                    viewModel.openMobilizeDialog("restore", device.device_id)
                },
                enabled = isTurnOnEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (isTurnOnEnabled) {
                    ButtonDefaults.outlinedButtonColors(contentColor = DR_Running)
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = DR_TextHint,
                        disabledContentColor = DR_TextHint.copy(alpha = 0.5f)
                    )
                }
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Engine On", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            // Engine Off
            Button(
                onClick = {
                    viewModel.openMobilizeDialog("cut", device.device_id)
                },
                enabled = isTurnOffEnabled,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (isTurnOffEnabled) {
                    ButtonDefaults.buttonColors(containerColor = DR_Stopped)
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = DR_TextHint.copy(alpha = 0.2f),
                        disabledContainerColor = DR_TextHint.copy(alpha = 0.12f),
                        disabledContentColor = DR_TextHint.copy(alpha = 0.5f)
                    )
                }
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isTurnOffEnabled) Color.White else DR_TextHint.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Engine Off",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isTurnOffEnabled) Color.White else DR_TextHint.copy(alpha = 0.5f)
                )
            }
        }
    }

    // Dialog Handling
    val dialogData by viewModel.showConfirmationDialog.collectAsState()

    dialogData?.let { data ->
        MobilizeConfirmationDialog(
            command = data.command,
            onConfirm = { cmd ->
                viewModel.sendMobilizeCommand(token, data.terminalId, cmd)
                isOnState = cmd == "restore"
                viewModel.closeMobilizeDialog()
            },
            onCancel = { viewModel.closeMobilizeDialog() }
        )
    }
}

@Composable
fun DeviceAnalysisContentWithData(
    liveLocation: LiveLocationResponse?,
    viewModel: DeviceReportViewModel,
    token: String
) {
    val selectedRange by viewModel.selectedAnalyticsRange.collectAsState()
    val analytics by viewModel.deviceAnalytics.collectAsState()
    val breakdown by viewModel.activityBreakdown.collectAsState()
    val isLoading by viewModel.isAnalyticsLoading.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    LaunchedEffect(selectedDevice, selectedRange) {
        val deviceId = selectedDevice?.device_id
        if (!deviceId.isNullOrBlank()) {
            viewModel.fetchDeviceAnalytics(token, deviceId, selectedRange)
        }
    }

    Spacer(Modifier.height(4.dp))

    FilterChipsBar(
        selectedRange = selectedRange,
        onRangeSelected = { range ->
            viewModel.onAnalyticsRangeSelected(token, range)
        }
    )

    Spacer(Modifier.height(14.dp))

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = DR_Primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    } else {
        TotalDistanceCard(totalKm = analytics?.totalDistanceKm ?: 0.0)

        Spacer(Modifier.height(12.dp))

        HeadingGraphCardWithData(analytics = analytics, breakdown = breakdown)

        Spacer(Modifier.height(20.dp))
    }
}


/**
 * The activity donut and its legend.
 *
 * Two possible sources, and [breakdown] wins: it is worked out from the vehicle's own
 * fixes, while `device/analytics` has been answering zero for running, stopped and
 * parked alike — a chart drawn from that is empty whatever the vehicle did. The server's
 * segments stay as the fallback for when the history call fails or a range holds too few
 * fixes to measure.
 */
@Composable
fun HeadingGraphCardWithData(
    analytics: DeviceAnalyticsData?,
    breakdown: ActivityBreakdown? = null
) {
    val running = breakdown?.running ?: analytics?.running
    val stopped = breakdown?.stopped ?: analytics?.stopped
    val parked = breakdown?.parked ?: analytics?.parked

    Card(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DR_SectionBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Activity Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = DR_TextPrimary
            )

            Spacer(Modifier.height(18.dp))

            Box(
                Modifier
                    .size(150.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                if (running != null && stopped != null && parked != null) {
                    DonutChart(
                        runningPercent = running.percent,
                        stoppedPercent = stopped.percent,
                        parkedPercent = parked.percent,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No data",
                            fontSize = 13.sp,
                            color = DR_TextHint
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (running != null && stopped != null && parked != null) {
                LegendRow(
                    label = "Running",
                    color = DR_Running,
                    hours = running.hours,
                    percent = running.percent
                )
                Spacer(Modifier.height(6.dp))
                LegendRow(
                    label = "Stopped",
                    color = DR_Stopped,
                    hours = stopped.hours,
                    percent = stopped.percent
                )
                Spacer(Modifier.height(6.dp))
                LegendRow(
                    label = "Parked",
                    color = DR_Idle,
                    hours = parked.hours,
                    percent = parked.percent
                )

                // Time the tracker did not report, named rather than hidden. The
                // percentages are a share of what *is* known, so without this line a day
                // with four hours of silence looks like a fully accounted-for day.
                breakdown?.takeIf { it.unknownMinutes > 0 }?.let { data ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Based on ${formatHoursShort(data.knownHours)} of reported activity · " +
                                "${formatHoursShort(data.unknownMinutes / 60.0)} not reported",
                        fontSize = 11.sp,
                        color = DR_TextHint
                    )
                }
            } else {
                Text("No analytics data available", color = DR_TextHint, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Hours for the footnote under the legend — "45 min", "1.5 hr", "6 hr".
 *
 * Minutes below the hour, because "0.3 hr" of reporting silence is not a figure anyone
 * reads as twenty minutes, and a whole number loses its ".0".
 */
private fun formatHoursShort(hours: Double): String {
    if (hours < 1.0) return "${(hours * 60).roundToInt()} min"
    val rounded = (hours * 10).roundToInt() / 10.0
    val whole = rounded.toInt()
    return if (rounded == whole.toDouble()) "$whole hr" else "$rounded hr"
}

@Composable
private fun LegendRow(
    label: String,
    color: Color,
    hours: Double,
    percent: Int
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DR_TextPrimary
            )

            Spacer(Modifier.weight(1f))

            Text(
                "$hours hr",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DR_TextSec
            )

            Spacer(Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    "$percent%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}


@Composable
fun DonutChart(
    runningPercent: Int,
    stoppedPercent: Int,
    parkedPercent: Int,
    modifier: Modifier = Modifier
) {
    val total = (runningPercent + stoppedPercent + parkedPercent)
        .takeIf { it > 0 } ?: 1

    val runningSweep = 360f * runningPercent / total
    val stoppedSweep = 360f * stoppedPercent / total
    val parkedSweep = 360f * parkedPercent / total

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.14f
            val radius = size.minDimension / 2f - strokeWidth / 2f
            val gap = 3f

            var startAngle = -90f

            fun drawSlice(color: Color, sweep: Float) {
                if (sweep <= 0f) return
                val actualSweep = if (sweep > gap * 2) sweep - gap else sweep
                drawArc(
                    color = color,
                    startAngle = startAngle + gap / 2,
                    sweepAngle = actualSweep,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
                startAngle += sweep
            }

            // Background ring
            drawArc(
                color = Color(0xFFE8EDE4),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )

            drawSlice(DR_Running, runningSweep)
            drawSlice(DR_Stopped, stoppedSweep)
            drawSlice(DR_Idle, parkedSweep)
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${runningPercent + parkedPercent}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DR_TextPrimary
            )
            Text(
                text = "Active",
                fontSize = 11.sp,
                color = DR_TextHint
            )
        }
    }
}

@Composable
fun FilterChipsBar(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsRange.entries.forEach { range ->
            val label = when (range) {
                AnalyticsRange.TODAY -> "Today"
                AnalyticsRange.WEEK -> "Week"
                AnalyticsRange.MONTH -> "Month"
            }
            Chip(
                text = label,
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) }
            )
        }
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) DR_Primary else DR_SectionBg,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else DR_TextSec
        )
    }
}


@Composable
fun TotalDistanceCard(totalKm: Double) {
    Card(
        modifier = Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DR_PrimaryBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DR_Primary.copy(alpha = 0.12f)
            ) {
                Box(modifier = Modifier.padding(10.dp)) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = DR_Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    "Total Distance",
                    color = DR_TextHint,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${totalKm}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = DR_TextPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "km",
                        fontSize = 14.sp,
                        color = DR_TextSec,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun MobilizeConfirmationDialog(
    command: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val isCut = command == "cut"
    val accentColor = if (isCut) DR_Stopped else DR_Running

    AlertDialog(
        onDismissRequest = { onCancel() },
        icon = {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = if (isCut) "Turn Off Engine?" else "Turn On Engine?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isCut)
                        "This will remotely cut the engine power for this vehicle."
                    else
                        "This will restore engine power for this vehicle.",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = DR_TextSec
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You can change this later from the device report.",
                    fontSize = 12.sp,
                    color = DR_TextHint,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(command) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isCut) "Yes, Turn Off" else "Yes, Turn On",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel", color = DR_TextSec)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = DR_Surface
    )
}