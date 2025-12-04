// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.ios.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.developer.ev2z.ui.screens.AddDevice

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
actual fun BottomSheet(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    // iOS simple implementation
    AddDeviceDeteails(onNavigate, viewModel)
}

@Composable
actual fun AddDevice(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    // iOS camera implementation placeholder
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Camera scanning - iOS implementation pending")
    }
}

@Composable
actual fun OpenCamera(
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    // iOS camera placeholder
}

actual fun ScanBarcode(
    scanner: Any,
    imageProxy: Any,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    // iOS barcode scanning placeholder
}

actual fun ProcessBarcodeData(
    data: String,
    format: Int,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    // iOS barcode processing placeholder
}

actual fun FillDeviceDetails(
    imageUri: Any,
    context: Any,
    scanStep: Int,
    onResult: (String) -> Unit
) {
    // iOS gallery picking placeholder
}

@Composable
actual fun AddDeviceDeteails(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    var deviceName by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var simNumber by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val deviceTypes = listOf("Wired", "Wireless")

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val addDeviceResult by viewModel.addDeviceResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val storage = remember { com.developer.ev2z.data.local.createStorageManager() }
    val userPrefs = remember { com.developer.ev2z.data.local.UserPrefs(storage) }
    val authToken = remember { userPrefs.getToken() ?: "" }

    LaunchedEffect(scannedDeviceId, scannedSimNumber) {
        deviceId = scannedDeviceId
        simNumber = scannedSimNumber
    }

    LaunchedEffect(addDeviceResult) {
        when (val result = addDeviceResult) {
            is AddDeviceResult.Success -> {
                println("✅ Device added successfully (iOS)")
            }
            is AddDeviceResult.Error -> {
                println("❌ Error: ${result.message}")
            }
            null -> { /* No result yet */ }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        IconButton(
            onClick = { onNavigate("home") },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color = Color.White)
                .border(width = 2.dp, color = Color.Gray, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                contentDescription = "Close",
                tint = Color.Black
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Add Device Details",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
                Text(
                    text = "All fields required",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
            }

            TextButton(
                onClick = { onNavigate("addDevice") },
                modifier = Modifier.weight(0.4f)
            ) {
                Text(
                    text = "Scan",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF00C853)
                )
            }
        }

        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Enter Vehicle Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = deviceId,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 16) {
                        deviceId = newValue
                    }
                },
                label = { Text("Device ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = deviceId.isNotEmpty() && deviceId.length < 15,
                supportingText = {
                    if (deviceId.isNotEmpty() && deviceId.length < 15) {
                        Text(
                            text = "Device ID must be 15-16 digits",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = simNumber,
                onValueChange = { newValue ->
                    val withoutU = newValue.removeSuffix("U")
                    if (withoutU.all { it.isDigit() } && withoutU.length <= 20) {
                        if (newValue.matches(Regex("^[0-9]*U?$"))) {
                            simNumber = newValue
                        }
                    }
                },
                label = { Text("Sim Number") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = simNumber.isNotEmpty() && simNumber.removeSuffix("U").length < 19,
                supportingText = {
                    if (simNumber.isNotEmpty() && simNumber.removeSuffix("U").length < 19) {
                        Text(
                            text = "SIM Number must be 19-20 digits",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Info",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = deviceType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Device Type") },
                placeholder = { Text("Select device type", color = Color.Gray) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00C853),
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                deviceTypes.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            deviceType = type
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (type != deviceTypes.last()) {
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }
            }
        }

        Text(
            text = "* Your Device will be activated within in 24 hours",
            modifier = Modifier.alpha(0.7f).fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

        errorMessage?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                onClick = { onNavigate("home") },
                modifier = Modifier
                    .weight(0.3f)
                    .border(1.dp, Color.Gray, RoundedCornerShape(100.dp)),
            ) {
                Text("Cancel", color = Color.Black)
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = {
                    val isDeviceIdValid = deviceId.length in 15..16
                    val isSimNumberValid = simNumber.removeSuffix("U").length in 19..20

                    if (deviceName.isNotEmpty() &&
                        isDeviceIdValid &&
                        isSimNumberValid &&
                        deviceType.isNotEmpty()) {

                        println("🔹 Calling addDevice API (iOS)")
                        viewModel.addDevice(
                            token = authToken,
                            deviceName = deviceName,
                            deviceId = deviceId,
                            simNumber = simNumber,
                            deviceType = deviceType.lowercase()
                        )
                    }
                },
                enabled = !isLoading &&
                        deviceName.isNotEmpty() &&
                        deviceId.length in 15..16 &&
                        simNumber.removeSuffix("U").length in 19..20 &&
                        deviceType.isNotEmpty(),
                modifier = Modifier.weight(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = if (isLoading) "Adding..." else "Add Device",
                    color = Color.White
                )
            }
        }
    }
}