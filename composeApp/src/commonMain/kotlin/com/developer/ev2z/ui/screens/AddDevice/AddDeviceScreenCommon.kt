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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.ui.components.dismissKeyboardOnTap
import com.developer.ev2z.ui.components.screenHeightDp
import com.developer.ev2z.util.Analytics
import kotlinx.coroutines.launch
import com.developer.ev2z.theme.AppPageBg

private val primaryContainerLight = Color(0xFF00C853)
private val BottomSheetPeek = 72.dp




@Composable
fun BottomSheetCommon(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel,
    cameraScreen: @Composable () -> Unit
) {
    var currentScreen by remember { mutableStateOf("scan") }
    val sheetState = rememberBottomSheetScaffoldState()

    // ✅ Coroutine scope for events
    val scope = rememberCoroutineScope()

    // ✅ SYSTEM BACK HANDLER (MASTER)
    PlatformBackHandler(enabled = true) {
        when {
            // 1️⃣ Sheet expanded → collapse
            sheetState.bottomSheetState.currentValue == SheetValue.Expanded -> {
                scope.launch {
                    sheetState.bottomSheetState.partialExpand()
                }
            }

            // 2️⃣ On details screen → back to scan
            currentScreen == "details" -> {
                currentScreen = "scan"
            }

            // 3️⃣ On scan screen → exit Add Device flow
            else -> {
                onNavigate("back") // navigator.pop() / go home
            }
        }
    }


    if (currentScreen == "details") {
        AddDeviceDetailsCommon(
            onNavigate = { route ->
                when (route) {
                    "scan" -> currentScreen = "scan"
                    else -> onNavigate(route)
                }
            },
            viewModel = viewModel
        )
    } else {

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
                            text = "Enter Device Id Manually",
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
            sheetPeekHeight = 105.dp,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                WindowInsets.systemBars
                    .only(WindowInsetsSides.Bottom)
                    .asPaddingValues()
            )
    ) {
        cameraView()

        IconButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    WindowInsets.systemBars
                        .only(WindowInsetsSides.Top)
                        .asPaddingValues()
                )
                .padding(top = 12.dp, end = 24.dp)
                .size(48.dp)
                .background(Color.White, shape = RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = "Gallery",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
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

// SIM numbers are 19 or 20 digits followed by a "U" suffix. Both lengths are valid.
private const val SIM_MIN_DIGITS = 19
private const val SIM_MAX_DIGITS = 20

/** The digits without the trailing "U", so length checks are independent of the suffix. */
private fun String.simDigits() = removeSuffix("U")

private fun String.isValidSimNumber() = simDigits().let { digits ->
    digits.length in SIM_MIN_DIGITS..SIM_MAX_DIGITS && digits.all { it.isDigit() }
}

/** Allows partially-typed values (digits, optional trailing "U") while blocking overlong input. */
private fun String.isTypableSimNumber() =
    simDigits().let { it.length <= SIM_MAX_DIGITS && it.all { c -> c.isDigit() } } &&
            (!endsWith("U") || length > 1)

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
    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val addDeviceResult by viewModel.addDeviceResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val vehicleTypes by viewModel.vehicleTypes.collectAsState()
    val isLoadingVehicleTypes by viewModel.isLoadingVehicleTypes.collectAsState()
    val vehicleTypesError by viewModel.vehicleTypesError.collectAsState()

    val storage = remember { com.developer.ev2z.data.local.createStorageManager() }
    val userPrefs = remember { com.developer.ev2z.data.local.UserPrefs(storage) }
    val authToken = remember { userPrefs.getToken() ?: "" }

    // The catalogue is fetched as the form opens rather than when the picker is
    // tapped: `vehicle_id` is required, so an empty catalogue blocks the whole form
    // and the user should not discover that at the last field.
    LaunchedEffect(Unit) { viewModel.loadVehicleTypes(authToken) }

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

    val focusManager = LocalFocusManager.current
    // Measured on the window so the sizing is about the device, not this composable's slot.
    val isCompact = screenHeightDp() < 700.dp
    val verticalGap = if (isCompact) 14.dp else 24.dp
    val logoSize = if (isCompact) 56.dp else 75.dp

    BoxWithConstraints(Modifier.fillMaxSize().background(AppPageBg)) {
        val horizontalPad = (maxWidth * 0.06f).coerceIn(16.dp, 32.dp)

        Column(
            Modifier
                .fillMaxSize()
                // systemBarsPadding keeps content clear of the iOS notch and home indicator,
                // which Compose does not inset automatically there.
                .systemBarsPadding()
                // imePadding before the scroll so the keyboard shrinks the viewport — that
                // is what lets the focused field scroll itself into view.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .dismissKeyboardOnTap(focusManager)
                .padding(horizontal = horizontalPad, vertical = if (isCompact) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(verticalGap)
        ) {
            Image(Icons.Filled.AccountCircle, "logo", contentScale = ContentScale.Fit, modifier = Modifier.size(logoSize))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(0.6f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Device Details", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                    Text("All fields required", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }

    //            TextButton(onClick = { onNavigate("addDevice") }, modifier = Modifier.weight(0.4f)) {
    //                Text("Scan", style = MaterialTheme.typography.titleMedium, color = primaryContainerLight)
    //            }
            }

            OutlinedTextField(
                deviceName,
                { deviceName = it },
                label = { Text("Enter Vehicle Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            // Same ExposedDropdownMenuBox shape as Device Type below, so the two
            // pickers on this form read as one control and not two. The list is the
            // server's catalogue — 41 types — so the menu is scrollable and the field
            // says what it is doing while that catalogue is still arriving.
            ExposedDropdownMenuBox(
                expanded = vehicleExpanded && vehicleTypes.isNotEmpty(),
                onExpandedChange = { if (vehicleTypes.isNotEmpty()) vehicleExpanded = !vehicleExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedVehicle?.name.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    enabled = vehicleTypes.isNotEmpty(),
                    label = { Text("Vehicle Type Icon") },
                    placeholder = {
                        Text(
                            when {
                                isLoadingVehicleTypes -> "Loading vehicle types…"
                                vehicleTypesError != null -> "Vehicle types unavailable"
                                else -> "Select vehicle type"
                            },
                            color = Color.Gray
                        )
                    },
                    // The picked icon shown in the field itself — the point of the
                    // field is the icon, so the name alone would not confirm it.
                    leadingIcon = selectedVehicle?.let { vehicle ->
                        {
                            Icon(
                                imageVector = vehicle.displayIcon,
                                contentDescription = null,
                                tint = primaryContainerLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        when {
                            isLoadingVehicleTypes -> CircularProgressIndicator(
                                color = primaryContainerLight,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            // A failed catalogue is a dead end without this: the field
                            // cannot be typed into, so a retry has to live in it.
                            vehicleTypesError != null -> IconButton(
                                onClick = { viewModel.loadVehicleTypes(authToken) }
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Retry loading vehicle types",
                                    tint = primaryContainerLight
                                )
                            }
                            else -> ExposedDropdownMenuDefaults.TrailingIcon(vehicleExpanded)
                        }
                    },
                    isError = vehicleTypesError != null,
                    supportingText = vehicleTypesError?.let {
                        { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryContainerLight,
                        unfocusedBorderColor = Color.Gray,
                        // Disabled only means "catalogue not here yet", so the field
                        // keeps reading as a normal one rather than greying out.
                        disabledBorderColor = Color.Gray,
                        disabledTextColor = Color.Black,
                        disabledLabelColor = Color.Gray,
                        disabledTrailingIconColor = Color.Gray
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                ExposedDropdownMenu(
                    expanded = vehicleExpanded && vehicleTypes.isNotEmpty(),
                    onDismissRequest = { vehicleExpanded = false },
                    modifier = Modifier
                        .background(Color.White)
                        // Capped so the menu cannot run off a short screen — 41 rows
                        // is far more than fits.
                        .heightIn(max = 320.dp)
                ) {
                    vehicleTypes.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text(vehicle.name, style = MaterialTheme.typography.bodyLarge) },
                            onClick = {
                                selectedVehicle = vehicle
                                vehicleExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = vehicle.displayIcon,
                                    contentDescription = null,
                                    tint = if (selectedVehicle?.vehicleId == vehicle.vehicleId) {
                                        primaryContainerLight
                                    } else {
                                        Color.Black
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                        if (vehicle != vehicleTypes.last()) {
                            HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                        }
                    }
                }
            }

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
                        if (newValue.isTypableSimNumber()) {
                            simNumber = newValue
                        }
                    },
                    label = { Text("Sim Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    isError = simNumber.isNotEmpty() && !simNumber.isValidSimNumber(),
                    supportingText = {
                        if (simNumber.isNotEmpty() && !simNumber.isValidSimNumber()) {
                            Text("SIM Number must be 19 or 20 digits", color = Color.Red, style = MaterialTheme.typography.bodySmall)
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

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    { onNavigate("home") },
                    Modifier.weight(0.35f).border(1.dp, Color.Gray, RoundedCornerShape(100.dp))
                ) {
                    Text("Cancel", color = Color.Black, maxLines = 1)
                }

                Button(
                    {
                        val isDeviceIdValid = deviceId.length in 15..16
                        val vehicle = selectedVehicle

                        if (deviceName.isNotEmpty() && isDeviceIdValid && simNumber.isValidSimNumber() &&
                            deviceType.isNotEmpty() && vehicle != null
                        ) {
                            viewModel.addDevice(
                                authToken,
                                deviceName,
                                deviceId,
                                simNumber,
                                deviceType.lowercase(),
                                vehicle.vehicleId
                            )
                        }
                    },
                    // The vehicle is part of this list because the server requires
                    // `vehicle_id`: without it the call comes back 422 and the user
                    // would only find out after pressing the button.
                    enabled = !isLoading && deviceName.isNotEmpty() && deviceId.length in 15..16 &&
                            simNumber.isValidSimNumber() && deviceType.isNotEmpty() &&
                            selectedVehicle != null,
                    modifier = Modifier.weight(0.65f),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(if (isLoading) "Adding..." else "Add Device", color = Color.White, maxLines = 1)
                }
            }

            // Keeps the action row clear of the keyboard once it is scrolled to the bottom.
            Spacer(Modifier.height(if (isCompact) 8.dp else 16.dp))
        }
    }
}