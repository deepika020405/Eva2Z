package com.developer.ev2z.ui.screens.GPSData

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler

// ✅ FIXED: Only 3 parameters now
@Composable
fun GPSDataScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit = {}
) {
    val viewModel = remember { GPSDataViewModel(apiService, storageManager) }
    val state by viewModel.state.collectAsState()

    PlatformBackHandler(enabled = true) {
        if (state.selectedDevice != null) {
            // 👈 SAME behaviour as toolbar back
            viewModel.deselectDevice()
        } else {
            onBackPressed()
        }
    }

    if (state.selectedDevice != null) {
        DeviceDetailFullScreen(
            device = state.selectedDevice!!,
            onBackClick = { viewModel.deselectDevice() },
            onRefresh = {
                viewModel.refreshDeviceData(state.selectedDevice!!.deviceId)
            }
        )
    } else {
        DeviceListScreen(
            state = state,
            onDeviceClick = { device -> viewModel.selectDevice(device) },
            onBackClick = onBackPressed,
            onRefresh = { viewModel.loadDevices() },
            onDismissError = { viewModel.clearError() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceListScreen(
    state: GPSDataState,
    onDeviceClick: (GPSDevice) -> Unit,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onDismissError: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GPS Device Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF6F6F6)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF4CAF50)
                    )
                }

                state.devices.isEmpty() -> {
                    EmptyDevicesView(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.devices) { device ->
                            GPSDeviceCard(
                                device = device,
                                onClick = { onDeviceClick(device) }
                            )
                        }
                    }
                }
            }

            // Error Snackbar
//            state.error?.let { error ->
//                Snackbar(
//                    modifier = Modifier
//                        .align(Alignment.BottomCenter)
//                        .padding(16.dp),
//                    action = {
//                        TextButton(onClick = onDismissError) {
//                            Text("Dismiss")
//                        }
//                    }
//                ) {
//                    Text(error)
//                }
//            }
        }
    }
}

@Composable
private fun GPSDeviceCard(
    device: GPSDevice,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.deviceName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = device.deviceId,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

//                StatusChip(status = device.status)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickStat(
                    icon = Icons.Default.ElectricBike,
                    label = "Vehicle Status",
                    value = device.ignition.replaceFirstChar { it.uppercase() },
                    color = statusColor(device.ignition)
                )


                val batteryIcon = when {
                    device.battery >= 90 -> Icons.Default.BatteryFull
                    device.battery >= 70 -> Icons.Default.Battery6Bar
                    device.battery >= 50 -> Icons.Default.Battery5Bar
                    device.battery >= 30 -> Icons.Default.Battery3Bar
                    device.battery >= 15 -> Icons.Default.Battery2Bar
                    else -> Icons.Default.BatteryAlert
                }

                QuickStat(
                    icon = batteryIcon,
                    label = "Battery",
                    value = batteryText(device),
                    color = when {
                        device.battery == 0 && device.speed > 0 -> Color(0xFF1976D2) // blue (moving)
                        device.battery == 0 -> Color.Gray
                        device.battery > 50 -> Color(0xFF4CAF50)
                        device.battery > 20 -> Color(0xFFFFC107)
                        else -> Color(0xFFE53935)
                    }

                )


                QuickStat(
                    icon = Icons.Default.Speed,
                    label = "Speed",
                    value = "${device.speed} km/h",
                    color = when {
                        device.speed > 45 -> Color(0xFFD32F2F)   // red = very fast
                        device.speed > 25 -> Color(0xFFFFA000)   // amber = medium
                        device.speed > 0  -> Color(0xFF1976D2)   // blue = moving
                        else -> Color.Gray
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Updated
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last Updated: ${device.lastUpdated}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                TextButton(onClick = onClick) {
                    Text("View Details", color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceDetailFullScreen(
    device: GPSDevice,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        device.deviceName,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF6F6F6)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Details
            item {
                SectionCard(title = "Device Details") {
                    DeviceDetailsSection(device)
                }
            }

            // Device Status
            item {
                SectionCard(title = "Device Status") {
                    DeviceStatusSection(device)
                }
            }

            // GPS Data
            item {
                SectionCard(title = "GPS Data") {
                    GPSDataSection(device)
                }
            }
        }
    }
}

@Composable
private fun DeviceDetailsSection(device: GPSDevice) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val isOffline = device.status.lowercase() == "offline"
        // Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (!isOffline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text =  "GPS Device Connected",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color =  Color(0xFF4CAF50)
                )
            }
        }

        DetailRow("Device ID", device.deviceId)
        DetailRow("Device Name", device.deviceName)
    }
}

@Composable
private fun DeviceStatusSection(device: GPSDevice) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailRow(
            label = "Status",
            value = device.status.replaceFirstChar { it.uppercase() },
            valueColor = statusColor(device.status)
        )


//        DetailRow(
//            label = "Battery",
//            value = "${device.battery}%",
//            valueColor = when {
//                device.battery > 50 -> Color(0xFF4CAF50)
//                device.battery > 20 -> Color(0xFFFFC107)
//                else -> Color(0xFFE53935)
//            }
//        )
        DetailRow(
            label = "Battery",
            value = batteryText(device),
            valueColor = when {
                device.battery == 0 && device.speed > 0 -> Color(0xFF1976D2) // Fetching → blue
                device.battery == 0 -> Color.Gray
                else -> statusColor(device.status)
            }
        )




        DetailRow(
            label = "Battery Status",
            value = device.batteryStatus.name.replace("_", " "),
            valueColor = if (device.batteryStatus == BatteryStatus.CHARGING)
                Color(0xFF4CAF50) else Color.Gray
        )
    }
}

@Composable
private fun GPSDataSection(device: GPSDevice) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
//        DetailRow("Distance", "${device.distance} km")
        DetailRow("Speed", "${device.speed} km/h")
        DetailRow("Location", device.location, valueColor = Color(0xFF4CAF50))
        DetailRow("Latitude", device.latitude.toString())
        DetailRow("Longitude", device.longitude.toString())
        DetailRow("Fix Time", device.fixTime)
        DetailRow("Server Time", device.serverTime)
    }
}

// Reusable Components
@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun StatusChip(status: String) {

    val statusLower = status.lowercase()

    val (bgColor, dotColor, textColor) = when (statusLower) {
        "moving" -> Triple(
            Color(0xFFE3F2FD),   // light blue
            Color(0xFF1976D2),   // blue
            Color(0xFF1976D2)
        )

        "idle" -> Triple(
            Color(0xFFFFF8E1),   // light yellow
            Color(0xFFFFA000),   // amber
            Color(0xFFFFA000)
        )

        "parked" -> Triple(
            Color(0xFFE8F5E9),   // light green
            Color(0xFF4CAF50),   // green
            Color(0xFF4CAF50)
        )

        "offline" -> Triple(
            Color(0xFFFFEBEE),   // light red
            Color(0xFFE53935),   // red
            Color(0xFFE53935)
        )

        else -> Triple(
            Color(0xFFF5F5F5),   // default gray
            Color.Gray,
            Color.Gray
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Text(
                text = statusLower.replaceFirstChar { it.uppercase() },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
private fun QuickStat(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            modifier = Modifier.weight(0.65f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EmptyDevicesView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📡", fontSize = 64.sp)
        Text(
            text = "No Devices Found",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = "Add a device to see GPS data",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}


fun statusColor(status: String): Color =
    when (status.lowercase()) {
        "on", "moving" -> Color(0xFF6DBD6D)      // Green
        "idle" -> Color(0xFFE8A93A)              // Amber
        "off", "parked", "offline" -> Color(0xFFE35E5E) // Red
        else -> Color.Gray
    }


fun batteryText(device: GPSDevice): String {
    val status = device.status.lowercase()

    return when {
        // moving but battery not yet received
        device.battery == 0 && device.speed > 0.0 -> {
            "Fetching"
        }

        // not moving / offline
        device.battery == 0 &&
                device.speed <= 0.0 &&
                status in listOf("offline", "parked", "idle") -> {
            "Vehicle not moving"
        }

        // normal case
        else -> {
            "${device.battery}%"
        }
    }
}
