package com.developer.ev2z.ui.screens.device_Report

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.response.Device
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource


data class DeviceItem(
    val name: String,
    val deviceId: String,
    val status: String // Running, Idle, Stopped
)

fun statusColor(status: String) = when (status) {
    "Running", "moving" -> Color(0xFF6DBD6D)
    "Idle", "idle" -> Color(0xFFE8A93A)
    "Stopped", "parked", "offline" -> Color(0xFFE35E5E)
    else -> Color.Gray
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DeviceReportScreen(
    viewModel: DeviceReportViewModel,
    token: String,
    username: String,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val myDevices by viewModel.myDevices.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val liveLocation by viewModel.liveLocation.collectAsState()
    val totalDevices by viewModel.totalDevices.collectAsState()



    BackPressHandler { onBack() }

    LaunchedEffect(Unit) {
        viewModel.fetchMyDevices(token)
    }


    when (uiState) {
        is DeviceReportUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is DeviceReportUiState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((uiState as DeviceReportUiState.Error).message)
                    Button(onClick = { viewModel.refreshData(token) }) {
                        Text("Retry")
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
                token = token,
                viewModel = viewModel// 👈 NEW
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
    username: String,
    token: String,
    viewModel: DeviceReportViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isDropdownOpen by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFF3EF))
    ) {
        // Map Area (You can integrate Google Maps here later)
//        MapArea()
        PlatformMapView(
            modifier = Modifier.fillMaxSize(),
            latitude =  liveLocation?.data?.latestLocation?.latitude?.toDoubleOrNull() ?: 0.0,
            longitude =  liveLocation?.data?.latestLocation?.longitude?.toDoubleOrNull() ?: 0.0,
            zoom = 15.0
        )
        // Map Marker Chip showing selected device
//        if (selectedDevice != null && liveLocation != null) {
////            MapMarkerChipWithData(
////                deviceName = selectedDevice.device_name,
////                speed = liveLocation.data?.latestLocation?.speed?.toString() ?: "0"
////            )
//        }

        // TOP BAR
        TopDeviceBoxWithData(
            userName = username, // You can pass actual user name from preferences
            deviceCount = totalDevices,
            selectedDevice = selectedDevice?.device_name,
            onBackClick = onBack,
            onDropdownClick = { isDropdownOpen = true },
            onAddClick = { /* Navigate to Add Device Screen */ }
        )

        // FULL SCREEN DROPDOWN PANEL
        if (isDropdownOpen) {
            DeviceSelectorPanelWithData(
                devices = devices,
                onDeviceSelected = {
                    onDeviceSelected(it)
                    isDropdownOpen = false
                },
                onAddDevice = { /* Navigate to Add Device */ },
                onDismiss = { isDropdownOpen = false }
            )
        }

        // BOTTOM SHEET
        if (selectedDevice != null) {
            DeviceReportBottomSheetWithData(
                device = selectedDevice,
                liveLocation = liveLocation,
                isExpanded = isExpanded,
                token = token,
                onExpandToggle = { isExpanded = !isExpanded },
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
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(bottom = 100.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color(0xFF3EBB3E), RoundedCornerShape(50))
            )
            Spacer(Modifier.width(8.dp))
            Text(deviceName, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text("$speed km/h", fontSize = 13.sp, color = Color.Gray)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 35.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.clickable { onBackClick() }
                )
                Spacer(Modifier.width(10.dp))

                Text("Hello, $userName", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.weight(1f))

                Text("$deviceCount Devices Listed", fontSize = 13.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .clickable { onDropdownClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(selectedDevice ?: "Select Device", fontSize = 15.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.clickable { onAddClick() }
                    )
                }
            }
        }
    }
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
            .clickable { onDismiss() },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(top = 80.dp)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Select Device", fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(device) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(
                                    statusColor(device.status ?: "Stopped"),
                                    RoundedCornerShape(50)
                                )
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.device_name, fontSize = 16.sp)
                            Text(
                                "Device ID: ${device.device_id}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }

                        Text(
                            device.status ?: "Unknown",
                            color = statusColor(device.status ?: "Stopped"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onAddDevice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3EBB3E))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Device", color = Color.White)
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
    token : String,
    onExpandToggle: () -> Unit,
    viewModel: DeviceReportViewModel
) {
    val sheetHeight = if (isExpanded) 0.90f else 0.40f

    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .fillMaxWidth()
            .fillMaxHeight(sheetHeight)
            .animateContentSize(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color.White,
        tonalElevation = 6.dp
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            // Report Header
            ReportHeaderWithData(
                deviceName = device.device_name,
                speed = liveLocation?.data?.latestLocation?.speed?.toString() ?: "0"
            )

            // Device Basic Info
            DeviceBasicInfoWithData(
                liveLocation = liveLocation,
                token = token,
                device = device,
                viewModel = viewModel
            )

            Spacer(Modifier.height(8.dp))

            HorizontalDivider(Modifier.padding(horizontal = 18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Device Analysis", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(if (isExpanded) "Hide" else "View", color = Color(0xFF3EBB3E))
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color(0xFF3EBB3E)
                )
            }

            if (isExpanded) {
                DeviceAnalysisContentWithData(liveLocation,viewModel)
            }
        }
    }
}

@Composable
fun ReportHeaderWithData(
    deviceName: String,
    speed: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("• $deviceName", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text("$speed km/h", fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

//@Composable
//fun DeviceBasicInfoWithData(
//    liveLocation: LiveLocationResponse?,
//    device: Device,
//    token: String,
//    viewModel: DeviceReportViewModel
//) {
//    val latest = liveLocation?.data?.latestLocation
//    val currentStatus = latest?.status ?: device.status
//    val dialogState by viewModel.showConfirmationDialog.collectAsState()
//    // jab bhi location change ho, address fetch karo
//    LaunchedEffect(latest?.latitude, latest?.longitude) {
//        latest?.let {
//            viewModel.getAddressFromLatLng(it.latitude.toDouble(), it.longitude.toDouble())
//        }
//    }
//
//    val address by viewModel.address.collectAsState()
//
//    Text(
//        "Live Location\n${address ?: "Fetching..."}",
//        fontSize = 14.sp,
//        modifier = Modifier.padding(horizontal = 18.dp),
//        color = Color.DarkGray
//    )
//
//    Spacer(Modifier.height(10.dp))
//
////    Row(
////        Modifier
////            .fillMaxWidth()
////            .padding(horizontal = 18.dp),
////        horizontalArrangement = Arrangement.spacedBy(14.dp)
////    ) {
////        val currentStatus = latest?.status ?: device.status
////
////        OutlinedButton(
////            onClick = { /* Handle Turn On */ },
////            enabled = currentStatus != "moving" && currentStatus != "Running",
////            modifier = Modifier.weight(1f)
////        ) {
////            Text("Turn On")
////        }
////
////        Button(
////            onClick = { /* Handle Turn Off */ },
////            modifier = Modifier.weight(1f),
////            enabled = currentStatus == "moving" || currentStatus == "Running",
////            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
////        ) {
////            Text("Turn Off")
////        }
////    }
//
//    Row(
//        Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 18.dp),
//        horizontalArrangement = Arrangement.spacedBy(14.dp)
//    ) {
//        val isMoving = currentStatus == "moving" || currentStatus == "Running"
//
//        // TURN ON → restore
//        OutlinedButton(
//            onClick = {
//                viewModel.openMobilizeDialog("restore", device.device_id)
//            },
//            enabled = !isMoving,   // cannot ON when already moving
//            modifier = Modifier.weight(1f),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = if (!isMoving) Color(0xFF3EBB3E) else Color.Gray
//            )
//        ) {
//            Text("Turn On")
//        }
//
//        // TURN OFF → cut
//        Button(
//            onClick = {
//                viewModel.openMobilizeDialog("cut", device.device_id)
//            },
//            enabled = isMoving,  // Off only allowed if running
//            modifier = Modifier.weight(1f),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = if (isMoving) Color(0xFFFF4E4E) else Color.Gray
//            )
//        ) {
//            Text("Turn Off", color = Color.White)
//        }
//    }
//
//    Spacer(Modifier.height(12.dp))
//
//    val activeTime = latest?.timestamp ?: "N/A"
//    val lastSeenMinutes = latest?.lastSeenMinutesAgo
//    val displayTime = if (lastSeenMinutes != null) {
//        when {
//            lastSeenMinutes == 0 -> "Just now"
//            lastSeenMinutes < 60 -> "$lastSeenMinutes minutes ago"
//            else -> "${lastSeenMinutes / 60} hours ago"
//        }
//    } else activeTime
//
//    Text(
//        "Active from: $displayTime",
//        color = Color.Gray,
//        fontSize = 12.sp,
//        modifier = Modifier.padding(horizontal = 18.dp)
//    )
//
//    val dialogData by viewModel.showConfirmationDialog.collectAsState()
//
//    if (dialogData != null) {
//        MobilizeConfirmationDialog(
//            command = dialogData!!.command,
//            onConfirm = {
//                viewModel.sendMobilizeCommand(
//                    token,
//                    dialogData!!.terminalId,
//                    dialogData!!.command
//                )
//                viewModel.closeMobilizeDialog()
//            },
//            onCancel = {
//                viewModel.closeMobilizeDialog()
//            }
//        )
//    }
//}


@Composable
fun DeviceBasicInfoWithData(
    liveLocation: LiveLocationResponse?,
    device: Device,
    token: String,
    viewModel: DeviceReportViewModel
) {
    val latest = liveLocation?.data?.latestLocation
    val currentStatus = latest?.status ?: device.status

    // jab bhi location change ho, address fetch karo
    LaunchedEffect(latest?.latitude, latest?.longitude) {
        latest?.let {
            viewModel.getAddressFromLatLng(
                it.latitude.toDouble(),
                it.longitude.toDouble()
            )
        }
    }

    val address by viewModel.address.collectAsState()

    Text(
        "Live Location\n${address ?: "Fetching..."}",
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 18.dp),
        color = Color.DarkGray
    )

    Spacer(Modifier.height(10.dp))

    // --------- BUTTON STATE LOGIC ----------
    // backend se initial status
    val backendIsOn = currentStatus == "moving" || currentStatus == "Running"

    // local UI state (optimistic) – status change hone par reset
    var isOnState by remember(currentStatus) { mutableStateOf(backendIsOn) }

    val isTurnOnEnabled = !isOnState   // OFF hai → Turn On enable
    val isTurnOffEnabled = isOnState   // ON hai → Turn Off enable

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // TURN ON → restore
        OutlinedButton(
            onClick = {
                viewModel.openMobilizeDialog("restore", device.device_id)
            },
            enabled = isTurnOnEnabled,
            modifier = Modifier.weight(1f),
            colors =
                if (isTurnOnEnabled) {
                    // Enabled: Green text
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF3EBB3E)
                    )
                } else {
                    // Disabled: default Material color (no custom color)
                    ButtonDefaults.outlinedButtonColors()
                }
        ) {
            Text("Turn On")
        }

        // TURN OFF → cut
        Button(
            onClick = {
                viewModel.openMobilizeDialog("cut", device.device_id)
            },
            enabled = isTurnOffEnabled,
            modifier = Modifier.weight(1f),
            colors =
                if (isTurnOffEnabled) {
                    // Enabled: Red background
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4E4E)
                    )
                } else {
                    // Disabled: default Material colors
                    ButtonDefaults.buttonColors()
                }
        ) {
            Text("Turn Off", color = Color.White)
        }
    }

    Spacer(Modifier.height(12.dp))

    val activeTime = latest?.timestamp ?: "N/A"
    val lastSeenMinutes = latest?.lastSeenMinutesAgo
    val displayTime = if (lastSeenMinutes != null) {
        when {
            lastSeenMinutes == 0 -> "Just now"
            lastSeenMinutes < 60 -> "$lastSeenMinutes minutes ago"
            else -> "${lastSeenMinutes / 60} hours ago"
        }
    } else activeTime

    Text(
        "Active from: $displayTime",
        color = Color.Gray,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 18.dp)
    )

    // --------- DIALOG HANDLING ----------
    val dialogData by viewModel.showConfirmationDialog.collectAsState()

    dialogData?.let { data ->
        MobilizeConfirmationDialog(
            command = data.command,
            onConfirm = { cmd ->
                // API call
                viewModel.sendMobilizeCommand(
                    token,
                    data.terminalId,
                    cmd
                )

                // Optimistic UI update:
                // restore => ON, cut => OFF
                isOnState = cmd == "restore"

                viewModel.closeMobilizeDialog()
            },
            onCancel = {
                viewModel.closeMobilizeDialog()
            }
        )
    }
}
@Composable
fun DeviceAnalysisContentWithData(liveLocation: LiveLocationResponse?,viewModel: DeviceReportViewModel) {
    Spacer(Modifier.height(8.dp))
    FilterChipsBar()
    Spacer(Modifier.height(16.dp))

    // You can calculate total distance from device data later
//    TotalDistanceCard()
    TotalDistanceCard(
        totalKm = viewModel.totalMilages.collectAsState().value?.total_distance_km ?: 0.0
    )
    Spacer(Modifier.height(16.dp))

    // Pass status count to the graph
    HeadingGraphCardWithData(liveLocation?.data?.statusCount)
    Spacer(Modifier.height(20.dp))
}

@Composable
fun HeadingGraphCardWithData(statusCount: com.developer.ev2z.data.local.remote.StatusCount?) {
    Surface(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF2F4F2)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Device Status Summary", fontSize = 18.sp, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .size(160.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Color.LightGray, RoundedCornerShape(100.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Chart Placeholder", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(12.dp))

            if (statusCount != null) {
                val total = statusCount.moving + statusCount.idle + statusCount.parked + statusCount.offline
                if (total > 0) {
                    Text("Moving - ${statusCount.moving} (${(statusCount.moving * 100 / total)}%)")
                    Text("Idle - ${statusCount.idle} (${(statusCount.idle * 100 / total)}%)")
                    Text("Parked - ${statusCount.parked} (${(statusCount.parked * 100 / total)}%)")
                    Text("Offline - ${statusCount.offline} (${(statusCount.offline * 100 / total)}%)")
                } else {
                    Text("No status data available", color = Color.Gray)
                }
            } else {
                Text("Loading status data...", color = Color.Gray)
            }
        }
    }
}




@Composable
fun FilterChipsBar() {
    Row(
        Modifier.padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Chip("Today")
        Chip("Week")
        Chip("Month")
    }
}

@Composable
fun Chip(text: String) {
    Surface(
        color = Color(0xFFDBF2D9),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            fontSize = 13.sp
        )
    }
}


@Composable
fun TotalDistanceCard(totalKm: Double) {

    Surface(
        Modifier
            .padding(horizontal = 18.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF2F4F2)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Total Distance", color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text("${(totalKm)} km", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
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

    AlertDialog(
        onDismissRequest = { onCancel() },
        icon = {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null
            )
        },
        title = {
            Text(
                text = if (isCut) "Turn Off Device?" else "Turn On Device?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = if (isCut)
                        "Do you really want to turn off this device?"
                    else
                        "Do you want to restore engine power for this device?"
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You can change this later from the device report screen.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(command) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCut) Color(0xFFFF4E4E) else Color(0xFF3EBB3E)
                )
            ) {
                Text(
                    text = if (isCut) "Yes, Turn Off" else "Yes, Turn On"
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}