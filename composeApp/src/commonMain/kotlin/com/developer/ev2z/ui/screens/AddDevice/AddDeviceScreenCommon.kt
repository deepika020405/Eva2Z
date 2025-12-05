// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreenCommon.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.developer.ev2z.ui.screens.AddDevice

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val primaryContainerLight = Color(0xFF00C853)

// ✅ COMMON BottomSheet with scaffold
@Composable
fun BottomSheetCommon(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel,
    cameraScreen: @Composable () -> Unit
) {
    var currentScreen by remember { mutableStateOf("scan") }

    if (currentScreen == "details") {
        AddDeviceDetailsCommon(
            onNavigate = { route ->
                when (route) {
                    "home" -> onNavigate("home")
                    "addDevice" -> currentScreen = "scan"
                    else -> onNavigate(route)
                }
            },
            viewModel = viewModel
        )
    } else {
        val sheetState = rememberBottomSheetScaffoldState()

        LaunchedEffect(sheetState.bottomSheetState.currentValue) {
            if (sheetState.bottomSheetState.currentValue == SheetValue.Expanded) {
                currentScreen = "details"
            }
        }

        BottomSheetScaffold(
            scaffoldState = sheetState,
            sheetContainerColor = Color.White,
            sheetDragHandle = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(top = 16.dp, bottom = 8.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Gray.copy(alpha = 0.6f))
                    )
                    Row {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "or enter details manually",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            modifier = Modifier.weight(4f)
                        )
                        IconButton(
                            onClick = { currentScreen = "details" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Open",
                                tint = Color.Black,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                }
            },
            sheetPeekHeight = 60.dp,
            sheetContent = {
                Box(modifier = Modifier.height(1.dp))
            }
        ) {
            cameraScreen()
        }
    }
}

// ✅ COMMON Camera Screen UI
@Composable
fun AddDeviceCommon(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel,
    cameraView: @Composable () -> Unit,
    onGalleryClick: () -> Unit
) {
    var scanStep by remember { mutableStateOf(1) }

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        cameraView()

        IconButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = "Gallery",
                tint = Color.Black,
                modifier = Modifier
                    .size(36.dp)
                    .background(color = Color.White)
                    .border(width = 2.dp, color = Color.Gray)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.6f))
                .padding(top = 120.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(75.dp).padding(bottom = 36.dp)
            )

            Text(
                if (scanStep == 1) "Step 1: Scan Device Barcode" else "Step 2: Scan SIM Card QR",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 100.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .width(358.dp)
                    .height(190.dp)
                    .border(4.dp, primaryContainerLight, RoundedCornerShape(28.dp))
            )

            Text(
                text = when {
                    scanStep == 1 && scannedDeviceId.isNotEmpty() -> "Device ID: $scannedDeviceId"
                    scanStep == 2 && scannedSimNumber.isNotEmpty() -> "SIM Number: $scannedSimNumber"
                    else -> "Fetching Data...."
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { onNavigate("home") },
                    modifier = Modifier.width(128.dp).height(36.dp)
                ) {
                    Text("Cancel", color = Color.White)
                }

                TextButton(
                    onClick = {
                        when (scanStep) {
                            1 -> if (scannedDeviceId.isNotEmpty()) scanStep = 2
                            2 -> if (scannedSimNumber.isNotEmpty()) onNavigate("addDeviceDeteails")
                        }
                    },
                    enabled = (scanStep == 1 && scannedDeviceId.isNotEmpty()) ||
                            (scanStep == 2 && scannedSimNumber.isNotEmpty()),
                    modifier = Modifier
                        .width(128.dp)
                        .height(36.dp)
                        .background(primaryContainerLight, RoundedCornerShape(100.dp))
                ) {
                    Text("Next", color = Color.White)
                }
            }
        }
    }
}

// ✅ COMMON AddDeviceDetails
@Composable
fun AddDeviceDetailsCommon(
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
            is AddDeviceResult.Success -> println("✅ Device added")
            is AddDeviceResult.Error -> println("❌ Error: ${result.message}")
            null -> {}
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        IconButton(
            onClick = { onNavigate("home") },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color.Gray, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardBackspace, "Close", tint = Color.Black)
        }

        Image(Icons.Filled.AccountCircle, "logo", contentScale = ContentScale.Fit, modifier = Modifier.size(75.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Device Details", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                Text("All fields required", style = MaterialTheme.typography.titleMedium, color = Color.Black)
            }

            TextButton(onClick = { onNavigate("addDevice") }, modifier = Modifier.weight(0.4f)) {
                Text("Scan", style = MaterialTheme.typography.titleMedium, color = primaryContainerLight)
            }
        }

        OutlinedTextField(
            deviceName,
            { deviceName = it },
            label = { Text("Enter Vehicle Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                deviceId,
                { if (it.all { c -> c.isDigit() } && it.length <= 16) deviceId = it },
                label = { Text("Device ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = deviceId.isNotEmpty() && deviceId.length < 15,
                supportingText = {
                    if (deviceId.isNotEmpty() && deviceId.length < 15) {
                        Text("Device ID must be 15-16 digits", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
            Icon(Icons.Outlined.Info, "Info", tint = Color.Black, modifier = Modifier.size(24.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                simNumber,
                { newValue ->
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
                        Text("SIM Number must be 19-20 digits", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
            Icon(Icons.Outlined.Info, "Info", tint = Color.Black, modifier = Modifier.size(24.dp))
        }

        ExposedDropdownMenuBox(expanded, { expanded = !expanded }, Modifier.fillMaxWidth()) {
            OutlinedTextField(
                deviceType,
                {},
                readOnly = true,
                label = { Text("Device Type") },
                placeholder = { Text("Select device type", color = Color.Gray) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryContainerLight,
                    unfocusedBorderColor = Color.Gray
                ),
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(expanded, { expanded = false }, Modifier.background(Color.White)) {
                deviceTypes.forEach { type ->
                    DropdownMenuItem(
                        { Text(type, style = MaterialTheme.typography.bodyLarge) },
                        { deviceType = type; expanded = false },
                        Modifier.fillMaxWidth()
                    )
                    if (type != deviceTypes.last()) {
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }
            }
        }

        Text(
            "* Your Device will be activated within in 24 hours",
            modifier = Modifier.alpha(0.7f).fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

        errorMessage?.let {
            Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                { onNavigate("home") },
                Modifier.weight(0.3f).border(1.dp, Color.Gray, RoundedCornerShape(100.dp))
            ) {
                Text("Cancel", color = Color.Black)
            }

            Spacer(Modifier.width(16.dp))

            Button(
                {
                    val isDeviceIdValid = deviceId.length in 15..16
                    val isSimNumberValid = simNumber.removeSuffix("U").length in 19..20

                    if (deviceName.isNotEmpty() && isDeviceIdValid && isSimNumberValid && deviceType.isNotEmpty()) {
                        viewModel.addDevice(authToken, deviceName, deviceId, simNumber, deviceType.lowercase())
                    }
                },
                enabled = !isLoading && deviceName.isNotEmpty() && deviceId.length in 15..16 &&
                        simNumber.removeSuffix("U").length in 19..20 && deviceType.isNotEmpty(),
                modifier = Modifier.weight(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(if (isLoading) "Adding..." else "Add Device", color = Color.White)
            }
        }
    }
}