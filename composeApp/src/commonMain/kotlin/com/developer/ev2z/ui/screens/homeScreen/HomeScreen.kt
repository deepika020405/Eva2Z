// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/homeScreen/HomeScreen.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.graphicsLayer
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.Analytics
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.InternalResourceApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    token: String,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onOpenDeviceStatus: () -> Unit,
    onOpenTrackTrace: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val showOverview by viewModel.showOverview.collectAsState()
    val showDeviceDropdown by viewModel.showDeviceDropdown.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()


    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "home_screen",
                "screen_class" to "HomeScreen"
            )
        )
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
            val devices = response.data ?: emptyList()
            val showDropdownAllowed = devices.size > 1


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
                        userName = response.name ?: "User",
                        deviceCount = response.totalDevices ?: 0,
                        selectedDevice = selectedDevice,
                        onDropdownClick = {
                            if (showDropdownAllowed) {
                                viewModel.toggleDeviceDropdown()
                            }
                        },
                        onLocationClick = {
                            viewModel.showOverviewSheet()
                        },
                        showDropdown = showDropdownAllowed
                    )

                    if (showDeviceDropdown) {
                        DeviceDropdownSheet(
                            devices = response.data ?: emptyList(),
                            onDeviceSelect = {
                                viewModel.selectDevice(it)
//                                viewModel.toggleDeviceDropdown()
                                onNavigateToDeviceReport()
                            },
                            onAddDevice = {
                                println("➕ Add Device clicked from dropdown")
                                viewModel.toggleDeviceDropdown()
                                onNavigateToAddDevice()
                            },
                            onDismiss = { viewModel.toggleDeviceDropdown() }
                        )
                    }
                }
                // 🔥 FLOATING CONTACT BUTTON (COMMON)
                FloatingContactButton(
                    phoneNumber = "919999999999",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 20.dp)
                )

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
                            onClose = { viewModel.hideOverviewSheet() },
                            onNavigateToDeviceReport = onNavigateToDeviceReport,
                            onOpenDeviceStatus = onOpenDeviceStatus,
                            onOpenTrackTrace = onOpenTrackTrace,
                            token = token,
                            viewModel = viewModel,
                            isRefreshing = isRefreshing
                        )
                    }
                }
            }
        }
    }
}

@OptIn(InternalResourceApi::class)
@Composable
fun TopDeviceCard(
    userName: String,
    deviceCount: Int,
    selectedDevice: Device?,
    onDropdownClick: () -> Unit,
    onLocationClick: () -> Unit,
    showDropdown: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Overview",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onLocationClick() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$deviceCount Devices Listed",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showDropdown) {
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
                        Image(
                            painter = painterResource(Res.drawable.app_logo),
                            contentDescription = "App Logo",
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
    onClose: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onOpenDeviceStatus: () -> Unit,
    onOpenTrackTrace: () -> Unit,
    token: String,
    viewModel : HomeViewModel,
    isRefreshing : Boolean
) {
    val totalDistance = response.data.sumOf { (it.speed * (it.lastSeenMinutesAgo ?: 0)).toLong() }
    val avgSpeed = response.data.filter { it.speed > 0 }.map { it.speed }.average().takeIf { !it.isNaN() } ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Device Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RefreshIcon(
                    isRefreshing = isRefreshing,
                    onClick = {
                        viewModel.refreshDevices(token)
                    }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Average Speed", "${avgSpeed.toInt()} km")
            StatItem("Total Distance", "${totalDistance}km")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 🔹 Secondary
            Button(
                onClick = { onOpenDeviceStatus() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Today's Report",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 🔹 Primary
            Button(
                onClick = { onOpenTrackTrace() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Track & Trace",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }




        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorIndicator(response.runningCount.toString(), Color(0xFF86b071), "Running")
            ColorIndicator(response.idleCount.toString(), Color(0xFFfcd158), "Ideal")
            ColorIndicator(response.parkedCount.toString(), Color(0xFFfa7575), "Parked")
            ColorIndicator(response.offlineCount.toString(), Color(0xFFe8e8e8), "Offline")
        }

        Spacer(modifier = Modifier.height(24.dp))

        response.data
            .filter { it.hasValidLocation() }
            .forEach { device ->
                DeviceOverviewItem(
                    device = device,
                    onClick = {
                        onNavigateToDeviceReport()
                    }
                )
            }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DeviceOverviewItem(
    device: Device,
    onClick: () -> Unit
) {
    val statusColor = Color(device.getStatusColor())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(45.dp)
                .background(color, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RefreshIcon(
    isRefreshing: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = if (isRefreshing)
            infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        else tween(0),
        label = "refresh_rotation"
    )

    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "Refresh",
        modifier = Modifier
            .size(24.dp)
            .graphicsLayer { rotationZ = rotation }
            .clickable(enabled = !isRefreshing) {
                onClick()
            },
        tint = if (isRefreshing) Color.Gray else Color.Black
    )
}

@Composable
expect fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
)