// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.ios.kt
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalForeignApi::class)

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
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.Foundation.NSNotificationCenter
import platform.UIKit.*
import platform.Vision.*

private val primaryContainerLight = Color(0xFF00C853)

// ✅ iOS BottomSheet with scaffold (matching Android)
@Composable
actual fun BottomSheet(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    var currentScreen by remember { mutableStateOf("scan") }

    when (currentScreen) {
        "details" -> {
            AddDeviceDeteails(
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigate("home")
                        "addDevice" -> currentScreen = "scan"
                        else -> onNavigate(route)
                    }
                },
                viewModel = viewModel
            )
        }
        else -> {
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
                AddDevice(
                    onNavigate = { route ->
                        if (route == "addDeviceDeteails") {
                            currentScreen = "details"
                        } else {
                            onNavigate(route)
                        }
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

// ✅ iOS Camera Screen (matching Android UI)
@Composable
actual fun AddDevice(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    var scanStep by remember { mutableStateOf(1) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()

    // Check camera permission
    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (status) {
            AVAuthorizationStatusAuthorized -> {
                hasCameraPermission = true
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    hasCameraPermission = granted
                }
            }
            else -> {
                hasCameraPermission = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera view
        if (hasCameraPermission) {
            OpenCamera(
                scanStep = scanStep,
                onBarcodeDetected = { result ->
                    when (scanStep) {
                        1 -> viewModel.setDeviceId(result)
                        2 -> viewModel.setSimNumber(result)
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera permission required", color = Color.White)
            }
        }

        // Gallery button
        IconButton(
            onClick = {
                // TODO: Implement PHPicker for iOS
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = "Open Gallery",
                tint = Color.Black,
                modifier = Modifier
                    .size(width = 36.dp, height = 36.dp)
                    .background(color = Color.White)
                    .border(width = 2.dp, color = Color.Gray)
            )
        }

        // Overlay UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .background(color = Color.Black.copy(alpha = 0.6f))
                .padding(top = 120.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(75.dp)
                    .height(75.dp)
                    .padding(bottom = 36.dp)
            )

            Text(
                if (scanStep == 1) "Step 1: Scan Device Barcode" else "Step 2: Scan SIM Card QR",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 100.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            // Scan frame
            Box(
                modifier = Modifier
                    .width(358.dp)
                    .height(190.dp)
                    .border(
                        width = 4.dp,
                        color = primaryContainerLight,
                        shape = RoundedCornerShape(size = 28.dp)
                    )
            )

            Text(
                text = when {
                    scanStep == 1 && scannedDeviceId.isNotEmpty() ->
                        "Device ID: $scannedDeviceId"
                    scanStep == 2 && scannedSimNumber.isNotEmpty() ->
                        "SIM Number: $scannedSimNumber"
                    else -> "Fetching Data...."
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onNavigate("home") },
                    modifier = Modifier
                        .width(128.dp)
                        .height(36.dp)
                        .background(color = Color.Transparent, shape = RoundedCornerShape(100.dp)),
                ) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }

                TextButton(
                    onClick = {
                        when (scanStep) {
                            1 -> {
                                if (scannedDeviceId.isNotEmpty()) {
                                    scanStep = 2
                                }
                            }
                            2 -> {
                                if (scannedSimNumber.isNotEmpty()) {
                                    onNavigate("addDeviceDeteails")
                                }
                            }
                        }
                    },
                    enabled = (scanStep == 1 && scannedDeviceId.isNotEmpty()) ||
                            (scanStep == 2 && scannedSimNumber.isNotEmpty()),
                    modifier = Modifier
                        .width(128.dp)
                        .height(36.dp)
                        .background(color = primaryContainerLight, shape = RoundedCornerShape(100.dp)),
                ) {
                    Text("Next", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
            }
        }
    }
}

// ✅ iOS Camera with AVFoundation
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun OpenCamera(
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    val captureSession = remember { AVCaptureSession() }
    val previewLayer = remember { AVCaptureVideoPreviewLayer(session = captureSession) }

    UIKitView(
        factory = {
            val container = UIView()

            // Setup camera
            captureSession.sessionPreset = AVCaptureSessionPresetHigh

            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

            device?.let { camera ->
                val input = AVCaptureDeviceInput.deviceInputWithDevice(camera, null) as? AVCaptureDeviceInput

                if (captureSession.canAddInput(input)) {
                    captureSession.addInput(input!!)
                }

                val output = AVCaptureVideoDataOutput()
                output.setSampleBufferDelegate(
                    object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
                        override fun captureOutput(
                            output: AVCaptureOutput,
                            didOutputSampleBuffer: platform.CoreMedia.CMSampleBufferRef?,
                            fromConnection: AVCaptureConnection
                        ) {
                            didOutputSampleBuffer?.let { sampleBuffer ->
                                ScanBarcode(
                                    scanner = null,
                                    imageProxy = sampleBuffer,
                                    scanStep = scanStep,
                                    onBarcodeDetected = onBarcodeDetected
                                )
                            }
                        }
                    },
                    queue = platform.darwin.dispatch_get_main_queue()
                )

                if (captureSession.canAddOutput(output)) {
                    captureSession.addOutput(output)
                }
            }

            previewLayer.frame = CGRectMake(0.0, 0.0, container.bounds.size.width, container.bounds.size.height)
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            container.layer.addSublayer(previewLayer)

            captureSession.startRunning()

            container
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            captureSession.stopRunning()
        }
    }
}

// ✅ iOS Barcode Scanning with Vision
@OptIn(ExperimentalForeignApi::class)
actual fun ScanBarcode(
    scanner: Any?,
    imageProxy: Any,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    val sampleBuffer = imageProxy as platform.CoreMedia.CMSampleBufferRef

    CMSampleBufferGetImageBuffer(sampleBuffer)?.let { imageBuffer ->
        val request = VNDetectBarcodesRequest { request, error ->
            if (error == null) {
                val observations = request.results as? List<*>
                observations?.forEach { observation ->
                    val barcode = observation as? VNBarcodeObservation
                    barcode?.payloadStringValue?.let { data ->
                        val format = when (barcode.symbology) {
                            VNBarcodeSymbologyQR -> 256 // QR_CODE
                            else -> 0 // Other formats
                        }
                        ProcessBarcodeData(data, format, scanStep, onBarcodeDetected)
                    }
                }
            }
        }

        val handler = VNImageRequestHandler(imageBuffer, mapOf<Any?, Any>())
        handler.performRequests(listOf(request), null)
    }
}

// ✅ iOS Process Barcode Data
actual fun ProcessBarcodeData(
    data: String,
    format: Int,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    when (scanStep) {
        1 -> {
            // Device ID - non-QR barcode
            if (format != 256) { // Not QR code
                val deviceId = data.replace(Regex("[^0-9]"), "")
                if (deviceId.isNotEmpty()) {
                    onBarcodeDetected(deviceId)
                }
            }
        }
        2 -> {
            // SIM Number - QR code
            if (format == 256) { // QR code
                val simNoRegex = Regex("""simNo="([^"]+)"""")
                val match = simNoRegex.find(data)
                val simNumber = match?.groups?.get(1)?.value ?: data
                val cleanSim = simNumber.removeSuffix("U")
                if (cleanSim.isNotEmpty()) {
                    onBarcodeDetected(cleanSim + "U")
                }
            }
        }
    }
}

// ✅ iOS Gallery Picker - PHPicker implementation
@OptIn(ExperimentalForeignApi::class)
actual fun FillDeviceDetails(
    imageUri: Any,
    context: Any,
    scanStep: Int,
    onResult: (String) -> Unit
) {
    // PHPicker implementation for selecting image from gallery
    // Then process with Vision framework (similar to ScanBarcode)
    // TODO: Complete PHPicker implementation
}

// ✅ iOS AddDeviceDetails (same as before)
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
            null -> {}
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

        Image(
            imageVector = Icons.Filled.AccountCircle,
            contentDescription = "logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(75.dp)
        )

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
                    color = primaryContainerLight
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
                    focusedBorderColor = primaryContainerLight,
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
                colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight),
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