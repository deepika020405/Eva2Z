// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.ios.kt
package com.developer.ev2z.ui.screens.AddDevice

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.Foundation.*
import platform.UIKit.*
import platform.Vision.*

private val onPrimaryLight = Color.White
private val transprent60 = Color.Black.copy(alpha = 0.6f)
private val transprentfull = Color.Transparent
private val primaryContainerLight = Color(0xFF00C853)
private val iconbcakground = Color.Gray

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalForeignApi::class)
@Composable
actual fun BottomSheet(onNavigate: (String) -> Unit) {
    val viewModel: AddDeviceViewModel = viewModel { AddDeviceViewModel() }
    var currentScreen by remember { mutableStateOf("scan") } // "scan" or "details"

    when (currentScreen) {
        "details" -> {
            // Full screen details
            AddDeviceDeteails(
                onNavigate = { route ->
                    when (route) {
                        "home" -> onNavigate("home")
                        "addDevice" -> currentScreen = "scan"  // Back to scan
                        else -> onNavigate(route)
                    }
                },
                viewModel = viewModel
            )
        }
        else -> {
            // Scan screen with bottom sheet
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
                sheetMaxWidth = Dp.Unspecified,
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

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AddDevice(onNavigate: (String) -> Unit, viewModel: AddDeviceViewModel) {
    var scanStep by remember { mutableStateOf(1) }
    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }

    // Request camera permission
    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        if (status == AVAuthorizationStatusAuthorized) {
            hasCameraPermission = true
        } else {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                hasCameraPermission = granted
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                Text(
                    "Camera permission required",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Gallery button
        IconButton(
            onClick = {
                // iOS photo picker implementation needed
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
                    .background(color = onPrimaryLight)
                    .border(width = 2.dp, color = iconbcakground)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .background(color = transprent60)
                .padding(top = 120.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo placeholder
            Box(
                modifier = Modifier
                    .width(75.dp)
                    .height(75.dp)
                    .padding(bottom = 36.dp)
                    .background(Color.White, CircleShape)
            )

            Text(
                if (scanStep == 1) "Scan Device Barcode" else "Scan SIM Card QR",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 100.dp),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .width(358.dp)
                    .height(190.dp)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            width = 4.dp,
                            color = primaryContainerLight,
                            shape = RoundedCornerShape(size = 28.dp)
                        )
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(358.dp)
                            .height(190.dp)
                            .align(Alignment.Center)
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    color = Color.Black,
                                    cornerRadius = CornerRadius(16.dp.toPx()),
                                    blendMode = BlendMode.DstOut
                                )
                            }
                    )
                }
            }

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
                        .background(
                            color = transprentfull,
                            shape = RoundedCornerShape(size = 100.dp)
                        ),
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
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
                        .background(
                            color = primaryContainerLight,
                            shape = RoundedCornerShape(size = 100.dp)
                        ),
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun OpenCamera(
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    val captureSession = remember { AVCaptureSession() }
    val previewLayer = remember { AVCaptureVideoPreviewLayer(session = captureSession) }

    DisposableEffect(Unit) {
        // Setup camera
        captureSession.sessionPreset = AVCaptureSessionPresetHigh

        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

        device?.let {
            val input = try {
                AVCaptureDeviceInput.deviceInputWithDevice(it, null)
            } catch (e: Exception) {
                null
            }

            input?.let { deviceInput ->
                if (captureSession.canAddInput(deviceInput)) {
                    captureSession.addInput(deviceInput)
                }
            }
        }

        // Setup metadata output for barcode scanning
        val metadataOutput = AVCaptureMetadataOutput()
        if (captureSession.canAddOutput(metadataOutput)) {
            captureSession.addOutput(metadataOutput)

            // Set barcode types based on scan step
            val types = mutableListOf<Any>()
            if (scanStep == 1) {
                // For device ID - all barcode formats except QR
                types.add(AVMetadataObjectTypeEAN13Code)
                types.add(AVMetadataObjectTypeEAN8Code)
                types.add(AVMetadataObjectTypeCode128Code)
                types.add(AVMetadataObjectTypeCode39Code)
                types.add(AVMetadataObjectTypeCode93Code)
            } else {
                // For SIM - QR code only
                types.add(AVMetadataObjectTypeQRCode)
            }

            metadataOutput.metadataObjectTypes = types

            // Set delegate for barcode detection
            val delegate = object : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
                override fun captureOutput(
                    output: AVCaptureOutput,
                    didOutputMetadataObjects: List<*>,
                    fromConnection: AVCaptureConnection
                ) {
                    didOutputMetadataObjects.firstOrNull()?.let { metadata ->
                        if (metadata is AVMetadataMachineReadableCodeObject) {
                            metadata.stringValue?.let { value ->
                                ProcessBarcodeData(
                                    data = value,
                                    format = when (metadata.type) {
                                        AVMetadataObjectTypeQRCode -> 256 // QR code
                                        else -> 1 // Barcode
                                    },
                                    scanStep = scanStep,
                                    onBarcodeDetected = onBarcodeDetected
                                )
                            }
                        }
                    }
                }
            }

            val queue = dispatch_get_main_queue()
            metadataOutput.setMetadataObjectsDelegate(delegate, queue = queue)
        }

        captureSession.startRunning()

        onDispose {
            captureSession.stopRunning()
        }
    }

    UIKitView(
        factory = {
            val view = UIView()
            previewLayer.frame = view.layer.bounds
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            view.layer.addSublayer(previewLayer)
            view
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            previewLayer.frame = view.layer.bounds
        }
    )
}

actual fun ScanBarcode(
    scanner: Any,
    imageProxy: Any,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    // iOS uses AVFoundation's built-in metadata detection
    // This function is not needed for iOS
}

actual fun ProcessBarcodeData(
    data: String,
    format: Int,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    when (scanStep) {
        1 -> {
            // Step 1: Scan Device ID (Barcode)
            if (format != 256) { // Not QR code
                val deviceId = data.replace(Regex("[^0-9]"), "")
                if (deviceId.isNotEmpty()) {
                    onBarcodeDetected(deviceId)
                    return
                }
            }
        }
        2 -> {
            // Step 2: Scan SIM Number (QR Code only)
            if (format == 256) { // QR code
                val simNoRegex = Regex("""simNo="([^"]+)"""")
                val match = simNoRegex.find(data)
                val simNumber = match?.groups?.get(1)?.value ?: data
                val cleanSim = simNumber.removeSuffix("U")
                if (cleanSim.isNotEmpty()) {
                    onBarcodeDetected(cleanSim + "U")
                    return
                }
            }
        }
    }
}

actual fun FillDeviceDetails(
    imageUri: Any,
    context: Any,
    scanStep: Int,
    onResult: (String) -> Unit
) {
    // iOS photo gallery barcode scanning
    // Would need UIImagePickerController implementation
    // This is placeholder for now
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

    LaunchedEffect(scannedDeviceId, scannedSimNumber) {
        deviceId = scannedDeviceId
        simNumber = scannedSimNumber
    }

    LaunchedEffect(addDeviceResult) {
        addDeviceResult?.let { result ->
            result.onSuccess { message ->
                viewModel.clearData()
                onNavigate("home")
            }
            result.onFailure { error ->
                // Handle error
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(color = onPrimaryLight)
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
                .background(color = onPrimaryLight)
                .border(width = 2.dp, color = iconbcakground, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                contentDescription = "Close",
                tint = Color.Black
            )
        }

        // App Logo placeholder
        Box(
            modifier = Modifier
                .width(75.dp)
                .height(75.dp)
                .background(Color.Gray, CircleShape)
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
            label = { Text("Device Name") },
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
                    if (newValue.all { it.isDigit() }) {
                        deviceId = newValue
                    }
                },
                label = { Text("Device ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
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
                    if (newValue.matches(Regex("^[0-9]*U?$"))) {
                        simNumber = newValue
                    }
                },
                label = { Text("Sim Number") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
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
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = deviceType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Device Type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                deviceTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            deviceType = type
                            expanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "* Your Device will be activated within in 24 hours",
            modifier = Modifier.alpha(0.7f).fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )

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
                    if (deviceName.isNotEmpty() && deviceId.isNotEmpty() && simNumber.isNotEmpty()) {
                        viewModel.addDevice(
                            token = "YOUR_TOKEN_HERE",
                            deviceName = deviceName,
                            deviceId = deviceId,
                            simNumber = simNumber
                        )
                    }
                },
                enabled = !isLoading && deviceName.isNotEmpty() && deviceId.isNotEmpty() && simNumber.isNotEmpty(),
                modifier = Modifier.weight(0.7f),
                colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = if (isLoading) "Adding.." else "Add Device",
                    color = Color.White
                )
            }
        }
    }
}