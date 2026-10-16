// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/homeScreen/LiveLocationScreen.kt
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.ui.components.FloatingContactButton
import com.developer.ev2z.util.Analytics
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.InternalResourceApi
import com.developer.ev2z.util.BottomBarController
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.style.TextOverflow

// ─────────────────────────────────────────────────────────────────────────────
//  FIX 1: Stable wrapper so the map never sees a new object reference unless
//          the actual device data changes.
// ─────────────────────────────────────────────────────────────────────────────
@Stable
private data class MapState(
    val devices: List<Device>,
    val selectedDevice: Device?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLocationScreen(
    viewModel: HomeViewModel,
    token: String,
    onBack: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToDeviceReport: () -> Unit,
    onOpenCommunity: () -> Unit,
    onSessionExpired: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()
    val showDeviceDropdown by viewModel.showDeviceDropdown.collectAsState()
    var autoNavigated by remember { mutableStateOf(false) }

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

    // 🔥 Keep the coordinates coming while the map is on screen.
    // loadDevices above is a single shot, so without this the marker stays wherever
    // it first appeared no matter how far the vehicle actually moves. Stopped on the
    // way out so it doesn't keep polling behind other screens.
    DisposableEffect(token) {
        viewModel.startDevicesPolling(token)
        onDispose { viewModel.stopDevicesPolling() }
    }

    // 🔥 Hide bottom navigation when this screen opens
    LaunchedEffect(Unit) {
        BottomBarController.visible = false
    }

    // 🔥 Restore bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            BottomBarController.visible = true
        }
    }

    PlatformBackHandler(enabled = true) {
        onBack()
    }

    when (val state = uiState) {
        is HomeUiState.Unauthorized -> {
            LaunchedEffect(Unit) { onSessionExpired() }
        }

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
                        text = "  Server Error ",
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

/*
        is HomeUiState.Success -> {
            val response = state.deviceResponse
            val devices = response.data ?: emptyList()
            val deviceCount = devices.size

            // ─────────────────────────────────────────────────────────────
            //  FIX 2: Compute filtered list ONCE per data change, not on
            //         every recomposition.
            // ─────────────────────────────────────────────────────────────
            val devicesWithLocation = remember(devices) {
                devices.filter { it.hasValidLocation() }
            }

            // ─────────────────────────────────────────────────────────────
            //  FIX 3: Stable MapState so PlatformMapView only recomposes
            //         when the map-relevant data actually changes.
            // ─────────────────────────────────────────────────────────────
            val mapState = remember(devicesWithLocation, selectedDevice?.deviceId) {
                MapState(
                    devices = devicesWithLocation,
                    selectedDevice = selectedDevice
                )
            }

            LaunchedEffect(deviceCount) {
                if (deviceCount == 1 && !autoNavigated) {
                    autoNavigated = true
                    viewModel.selectDevice(devices.first())
                    onNavigateToDeviceReport()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {

                // ─────────────────────────────────────────────────────────
                //  FIX 4: Map is isolated in its own composable so that
                //         dropdown / refresh state changes don't trigger a
                //         map redraw.
                // ─────────────────────────────────────────────────────────
                StableMapSection(
                    mapState = mapState,
                    onMarkerClick = { device -> viewModel.selectDevice(device) }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    TopDeviceCard(
                        // displayName, not name: the payload carries the account
                        // holder under both `name` and `username`, and the elvis
                        // here never fired because `name` is not nullable.
                        userName = response.displayName.ifBlank { "User" },
                        deviceCount = deviceCount,
                        selectedDevice = selectedDevice,
                        onBackClick = { onBack() },
                        onDropdownClick = { viewModel.toggleDeviceDropdown() },
                        onLocationClick = {
                            when (deviceCount) {
                                0 -> {}
                                1 -> onNavigateToDeviceReport()
                                else -> viewModel.showOverviewSheet()
                            }
                        },
                        showDropdown = deviceCount > 1
                    )

                    Spacer(Modifier.height(4.dp))

                    CommunityEntryCard(onClick = { onOpenCommunity() })

                    // ─────────────────────────────────────────────────────
                    //  FIX 5: Dropdown only rendered when visible, extracted
                    //         to its own key so it doesn't dirty the map.
                    // ─────────────────────────────────────────────────────
                    if (showDeviceDropdown && deviceCount > 1) {
                        key("device_dropdown") {
                            DeviceDropdownSheet(
                                devices = devices,
                                onDeviceSelect = {
                                    viewModel.selectDevice(it)
                                    onNavigateToDeviceReport()
                                },
                                onAddDevice = {
                                    viewModel.toggleDeviceDropdown()
                                    onNavigateToAddDevice()
                                },
                                onDismiss = { viewModel.toggleDeviceDropdown() }
                            )
                        }
                    }
                }

                FloatingContactButton(
                    phoneNumber = "919755273822",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 20.dp)
                )
            }
        }
*/



       is HomeUiState.Success -> {
            val response = state.deviceResponse

            // Latest device list from API state.
            val devices = response.data ?: emptyList()
            val deviceCount = devices.size

            // Filter valid coordinates.
            val devicesWithLocation = remember(devices) {
                devices.filter { it.hasValidLocation() }
            }

            // Resolve selected device from the latest API list.
            // This prevents stale selected-device coordinates.
            val latestSelectedDevice = remember(
                devicesWithLocation,
                selectedDevice?.deviceId
            ) {
                devicesWithLocation.firstOrNull {
                    it.deviceId == selectedDevice?.deviceId
                } ?: selectedDevice
            }

            // Build map state from the latest API data.
            val mapState = remember(
                devicesWithLocation,
                latestSelectedDevice
            ) {
                MapState(
                    devices = devicesWithLocation,
                    selectedDevice = latestSelectedDevice
                )
            }

            LaunchedEffect(deviceCount) {
                if (deviceCount == 1 && !autoNavigated) {
                    autoNavigated = true
                    viewModel.selectDevice(devices.first())
                    onNavigateToDeviceReport()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {

                // Map gets updated when API state provides new coordinates.
                StableMapSection(
                    mapState = mapState,
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
                        userName = response.displayName.ifBlank { "User" },
                        deviceCount = deviceCount,
                        selectedDevice = latestSelectedDevice,
                        onBackClick = { onBack() },
                        onDropdownClick = {
                            viewModel.toggleDeviceDropdown()
                        },
                        onLocationClick = {
                            when (deviceCount) {
                                0 -> Unit
                                1 -> onNavigateToDeviceReport()
                                // The overview sheet this used to open is gone, and an
                                // icon that does nothing is how that sheet came to be
                                // unreachable in the first place. The device list is the
                                // part of it worth keeping on a fleet of more than one.
                                else -> viewModel.toggleDeviceDropdown()
                            }
                        },
                        showDropdown = deviceCount > 1
                    )

                    Spacer(Modifier.height(4.dp))

                    CommunityEntryCard(
                        onClick = { onOpenCommunity() }
                    )

                    if (showDeviceDropdown && deviceCount > 1) {
                        key("device_dropdown") {
                            DeviceDropdownSheet(
                                devices = devices,
                                onDeviceSelect = { device ->
                                    viewModel.selectDevice(device)
                                    onNavigateToDeviceReport()
                                },
                                onAddDevice = {
                                    viewModel.toggleDeviceDropdown()
                                    onNavigateToAddDevice()
                                },
                                onDismiss = {
                                    viewModel.toggleDeviceDropdown()
                                }
                            )
                        }
                    }
                }

                FloatingContactButton(
                    phoneNumber = "919755273822",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 20.dp)
                )
            }

        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FIX 4 (continued): Isolated, stable map composable.
//  Uses @Stable MapState so Compose skips recomposition unless map data changes.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StableMapSection(
    mapState: MapState,
    onMarkerClick: (Device) -> Unit
) {
    PlatformMapView(
        modifier = Modifier.fillMaxSize(),
        devices = mapState.devices,
        selectedDevice = mapState.selectedDevice,
        onMarkerClick = onMarkerClick
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  TopDeviceCard — unchanged logic, no performance issue here
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(InternalResourceApi::class)
@Composable
fun TopDeviceCard(
    userName: String,
    deviceCount: Int,
    selectedDevice: Device?,
    onBackClick: () -> Unit,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onBackClick() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedDevice?.getDisplayName() ?: "Select Device",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

// ─────────────────────────────────────────────────────────────────────────────
//  DeviceDropdownSheet — unchanged logic
// ─────────────────────────────────────────────────────────────────────────────
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

            // ─────────────────────────────────────────────────────────────
            //  FIX 6: key() on each LazyColumn item prevents full list
            //         rebind when selection changes.
            // ─────────────────────────────────────────────────────────────
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(devices, key = { it.deviceId ?: it.hashCode() }) { device ->
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
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
    val address = rememberDeviceAddress(device)
    // Null while the fix is current. A five-day-old position and a live one read
    // identically without it, which is the one thing a live-location list must not do.
    val fixAge = device.fixAgeLabel()

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
            // Same reasoning as DeviceOverviewItem: the picker is where you choose
            // between vehicles, and "which one is at the office" is the question it
            // has to answer — the device id never helped with that.
            Text(
                text = address ?: "Locating…",
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = device.getStatusDisplay(),
                fontSize = 12.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            if (fixAge != null) {
                Text(
                    text = fixAge,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CommunityEntryCard — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CommunityEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF7B61FF).copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Community",
                    tint = Color(0xFF7B61FF),
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Community", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Connect with other EVA2Z users",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Platform expect — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Composable
expect fun PlatformMapView(
    modifier: Modifier,
    devices: List<Device>,
    selectedDevice: Device?,
    onMarkerClick: (Device) -> Unit
)