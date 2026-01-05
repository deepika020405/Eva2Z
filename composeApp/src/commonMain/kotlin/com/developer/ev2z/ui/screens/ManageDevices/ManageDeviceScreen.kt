package com.developer.ev2z.ui.screens.ManageDevices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit // ✅ Add this import
import androidx.compose.material.icons.sharp.Wifi
import androidx.compose.material.icons.sharp.Cable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.Device

// Theme colors
private val card_bg = Color(0xFFF5F5F5)
private val onPrimaryLight = Color.White
private val primaryContainerLight = Color(0xFF4CAF50)
private val secondaryContainerLight = Color(0xFFFF6B6B)
private val offline_status = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDeviceScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAddDeviceClick: () -> Unit = {},
    onEditDeviceClick: (deviceId: String, deviceName: String, simNumber: String) -> Unit = { _, _, _ -> },
    refreshTrigger: Int = 0 // ✅ Add this parameter
) {
    val viewModel = remember { ManageDeviceViewModel(apiService) }
    val uiState by viewModel.uiState.collectAsState()

    val userPrefs = remember { UserPrefs(storageManager) }
    var token by remember { mutableStateOf("") }

    // ✅ Reload devices when refreshTrigger changes
    LaunchedEffect(refreshTrigger) {
        token = userPrefs.getToken() ?: ""
        if (token.isNotEmpty()) {
            viewModel.loadDevices(token)
        }
    }
    PlatformBackHandler(enabled = true) {
        onBackClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(card_bg)
            .statusBarsPadding()
    ) {
        // Top Bar
        TopBar(
            onBackClick = onBackClick,
            onProfileClick = onProfileClick
        )

        // Add Device Section
        AddDeviceSection(onAddDeviceClick = onAddDeviceClick)

        // Content
        when {
            uiState.isLoading && uiState.devices.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryContainerLight)
                }
            }
            uiState.error != null && uiState.devices.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadDevices(token) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryContainerLight
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            uiState.devices.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add your first device to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    items(uiState.devices) { device ->
                        DeviceListItem(
                            device = device,
                            onDelete = {
                                viewModel.deleteDevice(token, device.device_id)
                            },
                            onTurnOff = {
                                viewModel.mobilizeDevice(token, device.device_id, "cut")
                            },
                            onTurnOn = {
                                viewModel.mobilizeDevice(token, device.device_id, "restore")
                            },
                            onEdit = {
                                // ✅ Call the callback with device details
                                onEditDeviceClick(
                                    device.device_id,
                                    device.device_name,
                                    device.sim_no ?: ""
                                )
                            }
                        )
                    }

                    // Add bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Text(
            text = "Manage Devices",
            style = MaterialTheme.typography.titleLarge,
        )

        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Profile",
                tint = Color.Black
            )
        }
    }
}

@Composable
private fun AddDeviceSection(onAddDeviceClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "You can add devices here",
            style = MaterialTheme.typography.bodyMedium
        )

        TextButton(
            onClick = onAddDeviceClick,
            modifier = Modifier.background(
                primaryContainerLight,
                RoundedCornerShape(100.dp)
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add",
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Text(
                text = "Add Device",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                color = Color.White
            )
        }
    }
}

@Composable
private fun DeviceListItem(
    device: Device,
    onDelete: () -> Unit,
    onTurnOff: () -> Unit,
    onTurnOn: () -> Unit,
    onEdit: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTurnOffDialog by remember { mutableStateOf(false) }
    var showTurnOnDialog by remember { mutableStateOf(false) }

    val dotColor = when (device.status) {
        "Active" -> Color(0xFF4CAF50)
        "In progress" -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val isEngineOn = device.engine_status == "on"
    val isEngineOff = device.engine_status == "off"
    val isEngineUnknown = device.engine_status == "unknown"

    val turnOffEnabled = isEngineOn || isEngineUnknown
    val turnOnEnabled = isEngineOff

    val turnOffTextColor = if (turnOffEnabled) secondaryContainerLight else offline_status
    val turnOnTextColor = if (turnOnEnabled) primaryContainerLight else offline_status

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(onPrimaryLight, shape = RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Device Name and Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = dotColor)
                }
                Text(
                    text = device.device_name.ifEmpty { "Unnamed Device" },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row {
                // ✅ Edit Button
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit Device",
                        tint = primaryContainerLight
                    )
                }

                // Delete Button
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove Device",
                        tint = Color.Red
                    )
                }
            }
        }

        // Device Details
        DeviceDetailRow(label = "Device ID: ", value = device.device_id)

        // Only show SIM No if it's not null, empty, or blank
        if (!device.sim_no.isNullOrBlank()) {
            DeviceDetailRow(label = "SIM No: ", value = device.sim_no)
        }

        DeviceDetailRow(label = "Added on: ", value = formatDate(device.created_at))

        // Only show Device Type if it's not null
        if (!device.device_type.isNullOrBlank()) {
            DeviceTypeRow(deviceType = device.device_type)
        }

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { showTurnOffDialog = true },
                enabled = turnOffEnabled,
                modifier = Modifier
                    .weight(1f)
                    .background(onPrimaryLight)
                    .border(1.dp, turnOffTextColor, RoundedCornerShape(100.dp))
            ) {
                Text("Turn Off", color = turnOffTextColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextButton(
                onClick = { showTurnOnDialog = true },
                enabled = turnOnEnabled,
                modifier = Modifier
                    .weight(1f)
                    .background(onPrimaryLight)
                    .border(1.dp, turnOnTextColor, RoundedCornerShape(100.dp))
            ) {
                Text("Turn On", color = turnOnTextColor)
            }
        }
    }

    // Confirmation Dialogs
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Device",
            message = "Are you sure you want to delete '${device.device_name}'?",
            confirmText = "Delete",
            confirmColor = Color.Red,
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showTurnOffDialog) {
        ConfirmationDialog(
            title = "Turn Off Device",
            message = "Are you sure you want to turn off '${device.device_name}'?",
            confirmText = "Turn Off",
            confirmColor = secondaryContainerLight,
            onConfirm = {
                onTurnOff()
                showTurnOffDialog = false
            },
            onDismiss = { showTurnOffDialog = false }
        )
    }

    if (showTurnOnDialog) {
        ConfirmationDialog(
            title = "Turn On Device",
            message = "Are you sure you want to turn on '${device.device_name}'?",
            confirmText = "Turn On",
            confirmColor = primaryContainerLight,
            onConfirm = {
                onTurnOn()
                showTurnOnDialog = false
            },
            onDismiss = { showTurnOnDialog = false }
        )
    }
}

@Composable
private fun DeviceDetailRow(label: String, value: String) {
    Row {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DeviceTypeRow(deviceType: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = "Device Type: ", style = MaterialTheme.typography.bodyMedium)

        when (deviceType.lowercase()) {
            "wireless" -> {
                Icon(
                    imageVector = Icons.Sharp.Wifi,
                    contentDescription = "Wireless",
                    modifier = Modifier.size(16.dp),
                    tint = primaryContainerLight
                )
                Text(text = "Wireless", style = MaterialTheme.typography.bodyMedium)
            }
            "wired" -> {
                Icon(
                    imageVector = Icons.Sharp.Cable,
                    contentDescription = "Wired",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF2196F3)
                )
                Text(text = "Wired", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text(text = "Unknown", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(dateString: String): String {
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    return try {
        val parts = dateString.split(" ")[0].split("-")
        val day = parts[0]
        val month = months[parts[1].toInt() - 1]
        val year = parts[2]
        "$day $month $year"
    } catch (e: Exception) {
        dateString
    }
}