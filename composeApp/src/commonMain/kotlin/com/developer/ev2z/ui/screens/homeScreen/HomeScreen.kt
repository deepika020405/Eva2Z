// commonMain/ui/screens/homeScreen/HomeScreen.kt
package com.developer.ev2z.ui.screens.homeScreen

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import org.jetbrains.compose.ui.tooling.preview.Preview

//@OptIn(ExperimentalMaterial3Api::class)
//@Preview()

//@Composable
//fun HomeScreen() {
//    var showOverview by remember { mutableStateOf(false) }
//    var showDeviceDropdown by remember { mutableStateOf(false) }
//    var selectedDevice by remember { mutableStateOf<Device?>(null) }
//
//    // API response state -ViewModel se replace karna
//    var deviceResponse by remember { mutableStateOf<DeviceResponse?>(null) }
//
//    // Sample data from API response
//    LaunchedEffect(Unit) {
//        // Yahan API call hogi
//        // deviceResponse = viewModel.getDevices()
//
//        // Temporary sample data
//        deviceResponse = DeviceResponse(
//            success = true,
//            userId = 419,
//            name = "hshsa324",
//            totalDevices = 14,
//            runningCount = 2,
//            idleCount = 3,
//            parkedCount = 0,
//            offlineCount = 9,
//            data = listOf(
//                Device("0863656047642660", null, "28.244157", "80.107397", 0, "idle", "2025-12-02 08:54:39", 3),
//                Device("0863656047487991", null, "28.603788", "77.352734", 0, "idle", "2025-12-02 08:56:41", 1),
//                Device("0863656047491639", null, "22.707926", "75.917024", 0, "idle", "2025-12-02 08:53:15", 4),
//                Device("0863656047548878", null, null, null, 0, "no_data", null, null),
//                Device("08636560475488756", null, null, null, 0, "no_data", null, null),
//                Device("0863656047646372", null, "28.396273", "77.093408", 40, "moving", "2025-12-02 08:58:09", 0),
//                Device("456491790605", null, null, null, 0, "no_data", null, null),
//                Device("01359097370173471", null, null, null, 0, "no_data", null, null),
//                Device("0564891203289460", null, null, null, 0, "no_data", null, null),
//                Device("0863656047558650", null, "28.506051", "77.069514", 0, "offline", "2025-11-21 11:20:26", 15697),
//                Device("056585586575223", null, null, null, 0, "no_data", null, null),
//                Device("4564917906052243", null, null, null, 0, "no_data", null, null),
//                Device("0863656047649434", null, "28.603674", "77.352726", 0, "offline", "2025-12-02 08:08:16", 49),
//                Device("0863656047646497", null, "28.605106", "77.359708", 14, "moving", "2025-12-02 08:58:13", 0)
//            )
//        )
//    }
//
//    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
//
//    deviceResponse?.let { response ->
//        Box(modifier = Modifier.fillMaxSize()) {
//            // Map with markers - only devices with valid locations
//            val devicesWithLocation = response.data.filter { it.hasValidLocation() }
//
//            PlatformMapView(
//                modifier = Modifier.fillMaxSize(),
//                devices = devicesWithLocation,
//                selectedDevice = selectedDevice,
//                onMarkerClick = { device ->
//                    selectedDevice = device
//                    showOverview = true
//                }
//            )
//
//            // Top Card
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.TopCenter)
//                    .padding(16.dp)
//            ) {
//                TopDeviceCard(
//                    userName = response.name,
//                    deviceCount = response.totalDevices,
//                    selectedDevice = selectedDevice,
//                    onDropdownClick = { showDeviceDropdown = !showDeviceDropdown }
//                )
//
//                // Device Dropdown
//                if (showDeviceDropdown) {
//                    DeviceDropdownSheet(
//                        devices = response.data,
//                        onDeviceSelect = { device ->
//                            selectedDevice = device
//                            showDeviceDropdown = false
//                            if (device.hasValidLocation()) {
//                                showOverview = true
//                            }
//                        },
//                        onAddDevice = {
//                            // Add device action
//                        },
//                        onDismiss = { showDeviceDropdown = false }
//                    )
//                }
//            }
//
//            // Overview Bottom Sheet
//            if (showOverview) {
//                ModalBottomSheet(
//                    onDismissRequest = {
//                        showOverview = false
//                        selectedDevice = null
//                    },
//                    sheetState = sheetState,
//                    containerColor = Color.White,
//                    dragHandle = {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 12.dp),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .width(40.dp)
//                                    .height(4.dp)
//                                    .background(Color.LightGray, RoundedCornerShape(2.dp))
//                            )
//                        }
//                    }
//                ) {
//                    OverviewBottomSheet(
//                        response = response,
//                        onClose = { showOverview = false }
//                    )
//                }
//            }
//        }
//    } ?: Box(
//        modifier = Modifier.fillMaxSize(),
//        contentAlignment = Alignment.Center
//    ) {
//        CircularProgressIndicator()
//    }
//}
// ui/screens/homeScreen/HomeScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview()
fun HomeScreen(
    viewModel: HomeViewModel,
    token: String // LoginResponse se milega
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val showOverview by viewModel.showOverview.collectAsState()
    val showDeviceDropdown by viewModel.showDeviceDropdown.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(Unit) {
        viewModel.loadDevices(token)
    }

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is HomeUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error: ${state.message}",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshDevices(token) }) {
                        Text("Retry")
                    }
                }
            }
        }

        is HomeUiState.Success -> {
            val response = state.deviceResponse

            Box(modifier = Modifier.fillMaxSize()) {
                val devicesWithLocation = response.data.filter { it.hasValidLocation() }

                PlatformMapView(
                    modifier = Modifier.fillMaxSize(),
                    devices = devicesWithLocation,
                    selectedDevice = selectedDevice,
                    onMarkerClick = { device ->
                        viewModel.selectDevice(device)
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    TopDeviceCard(
                        userName = response.name,
                        deviceCount = response.totalDevices,
                        selectedDevice = selectedDevice,
                        onDropdownClick = { viewModel.toggleDeviceDropdown() }
                    )

                    if (showDeviceDropdown) {
                        DeviceDropdownSheet(
                            devices = response.data,
                            onDeviceSelect = { viewModel.selectDevice(it) },
                            onAddDevice = { /* Navigate */ },
                            onDismiss = { viewModel.toggleDeviceDropdown() }
                        )
                    }
                }

                if (showOverview) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.hideOverviewSheet() },
                        sheetState = sheetState,
                        containerColor = Color.White,
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .background(Color.LightGray, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    ) {
                        OverviewBottomSheet(
                            response = response,
                            onClose = { viewModel.hideOverviewSheet() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopDeviceCard(
    userName: String,
    deviceCount: Int,
    selectedDevice: Device?,
    onDropdownClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hello, $userName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "$deviceCount Devices Listed",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Select Device Dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .clickable { onDropdownClick() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedDevice?.getDisplayName() ?: "Select Device",
                        fontSize = 14.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceDropdownSheet(
    devices: List<Device>,
    onDeviceSelect: (Device) -> Unit,
    onAddDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select device drop down",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onDismiss() }
                )
            }

            Divider()

            // Device List
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(devices) { device ->
                    DeviceListItem(
                        device = device,
                        onClick = { onDeviceSelect(device) }
                    )
                }
            }

            // Add Device Button
            Button(
                onClick = onAddDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Device")
            }
        }
    }
}

@Composable
fun DeviceListItem(device: Device, onClick: () -> Unit) {
    val statusColor = Color(device.getStatusColor())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.getDisplayName(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Device ID: ${device.deviceId}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Text(
            text = device.getStatusDisplay(),
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OverviewBottomSheet(
    response: DeviceResponse,
    onClose: () -> Unit
) {
    // Calculate total stats
    val totalDistance = response.data.sumOf { (it.speed * (it.lastSeenMinutesAgo ?: 0)).toLong() }
    val avgSpeed = response.data.filter { it.speed > 0 }.map { it.speed }.average().takeIf { !it.isNaN() } ?: 0.0

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
            Text(
                text = "Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onClose() }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Average Speed", "${avgSpeed.toInt()} km")
            StatItem("Total Distance", "${totalDistance}km")
            StatItem("Carbon Saved", "40,000")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Color Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorIndicator(response.runningCount.toString(), Color(0xFF4CAF50), "Running")
            ColorIndicator(response.idleCount.toString(), Color(0xFFFFC107), "Ideal")
            ColorIndicator(response.parkedCount.toString(), Color(0xFFF44336), "Parked")
            ColorIndicator(response.offlineCount.toString(), Color(0xFFBDBDBD), "Offline")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Device List - show only devices with location
        response.data.filter { it.hasValidLocation() }.forEach { device ->
            DeviceOverviewItem(device)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // See All
        Text(
            text = "See All",
            color = Color(0xFF4CAF50),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.End)
                .clickable { }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DeviceOverviewItem(device: Device) {
    val statusColor = Color(device.getStatusColor())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.getDisplayName(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Device ID: ${device.deviceId}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Text(
            text = device.getStatusDisplay(),
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ColorIndicator(value: String, color: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(color, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

// Platform specific map
@Composable
expect fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
)

//package com.developer.ev2z.ui.screens.homeScreen
//
//import androidx.compose.runtime.Composable
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.ui.Modifier
//
//@Composable
//fun HomeScreen() {
//    PlatformMapView(
//        modifier = Modifier.fillMaxSize(),
//        latitude = 28.6139,
//        longitude = 77.2090,
//        zoom = 12.0
//    )
//}