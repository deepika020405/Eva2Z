// composeApp/src/androidMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.android.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.developer.ev2z.ui.screens.AddDevice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val onPrimaryLight = Color.White
private val transprent60 = Color.Black.copy(alpha = 0.6f)
private val transprentfull = Color.Transparent
private val primaryContainerLight = Color(0xFF00C853)
private val iconbcakground = Color.Gray

// ✅ Fixed: Added viewModel parameter
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

@Composable
actual fun AddDevice(onNavigate: (String) -> Unit, viewModel: AddDeviceViewModel) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var scanStep by remember { mutableStateOf(1) }

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            FillDeviceDetails(uri, context, scanStep) { result ->
                when (scanStep) {
                    1 -> viewModel.setDeviceId(result)
                    2 -> viewModel.setSimNumber(result)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            Box(modifier = Modifier.fillMaxSize())
        }

        IconButton(
            onClick = { galleryLauncher.launch("image/*") },
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
                .padding(top = screenWidth * 0.30f, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "image description",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(75.dp)
                    .height(75.dp)
                    .padding(bottom = 36.dp)
            )

            Text(
                if (scanStep == 1) "Step 1: Scan Device Barcode" else "Step 2: Scan SIM Card QR",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = screenWidth * 0.25f),
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
                    .width(screenWidth * 1f)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onNavigate("home") },
                    modifier = Modifier
                        .width(128.dp)
                        .height(36.dp)
                        .background(color = transprentfull, shape = RoundedCornerShape(size = 100.dp)),
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
                        .background(color = primaryContainerLight, shape = RoundedCornerShape(size = 100.dp)),
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

@SuppressLint("MissingPermission")
@Composable
actual fun OpenCamera(
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = {
            val previewView = PreviewView(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    val barcodeScanner = BarcodeScanning.getClient()
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                ScanBarcode(
                                    scanner = barcodeScanner,
                                    imageProxy = imageProxy,
                                    scanStep = scanStep,
                                    onBarcodeDetected = onBarcodeDetected
                                )
                            }
                        }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(context))

            previewView.apply {
                this.scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
actual fun ScanBarcode(
    scanner: Any,
    imageProxy: Any,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    val barcodeScanner = scanner as com.google.mlkit.vision.barcode.BarcodeScanner
    val proxy = imageProxy as ImageProxy

    val mediaImage = proxy.image
    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            proxy.imageInfo.rotationDegrees
        )
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue
                    ProcessBarcodeData(rawValue, barcode.format, scanStep, onBarcodeDetected)
                }
            }
            .addOnCompleteListener {
                proxy.close()
            }
    } else {
        proxy.close()
    }
}

actual fun ProcessBarcodeData(
    data: String,
    format: Int,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    when (scanStep) {
        1 -> {
            if (format != Barcode.FORMAT_QR_CODE) {
                val deviceId = data.replace(Regex("[^0-9]"), "")
                if (deviceId.isNotEmpty()) {
                    onBarcodeDetected(deviceId)
                    return
                }
            }
        }
        2 -> {
            if (format == Barcode.FORMAT_QR_CODE) {
                val simNoRegx = Regex("""simNo="([^"]+)"""")
                val match = simNoRegx.find(data)
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
    val uri = imageUri as Uri
    val ctx = context as Context

    try {
        val source = ImageDecoder.createSource(ctx.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source)

        val image = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient()

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isEmpty()) {
                    Log.e("FillDeviceDetails", "No barcodes found")
                    return@addOnSuccessListener
                }

                for (barcode in barcodes) {
                    val rawValue = barcode.rawValue ?: continue

                    when (scanStep) {
                        1 -> {
                            if (barcode.format != Barcode.FORMAT_QR_CODE) {
                                val deviceId = rawValue.replace(Regex("[^0-9]"), "")
                                if (deviceId.isNotEmpty()) {
                                    onResult(deviceId)
                                    return@addOnSuccessListener
                                }
                            }
                        }
                        2 -> {
                            if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                val simNORegex = Regex("""simNo="([^"]+)"""")
                                val match = simNORegex.find(rawValue)
                                val simNumber = match?.groups?.get(1)?.value ?: rawValue
                                val cleanSim = simNumber.removeSuffix("U")
                                if (cleanSim.isNotEmpty()) {
                                    onResult(cleanSim + "U")
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FillDeviceDetails", "Error: ${e.message}")
            }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// composeApp/src/androidMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.android.kt

@Composable
actual fun AddDeviceDeteails(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val context = LocalContext.current

    var deviceName by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf("") }
    var simNumber by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("") } // ✅ Empty initially
    var expanded by remember { mutableStateOf(false) }
    val deviceTypes = listOf("Wired", "Wireless") // ✅ Capital case for display

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val addDeviceResult by viewModel.addDeviceResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // ✅ Get token from DataStore
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
                println("✅ Device added successfully")
            }
            is AddDeviceResult.Error -> {
                Log.e("AddDevice", "Error: ${result.message}")
            }
            null -> { /* No result yet */ }
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

//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            OutlinedTextField(
//                value = deviceId,
//                onValueChange = { newValue ->
//                    if (newValue.all { it.isDigit() }) {
//                        deviceId = newValue
//                    }
//                },
//                label = { Text("Device ID") },
//                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                modifier = Modifier.weight(1f),
//                singleLine = true,
//                shape = RoundedCornerShape(8.dp)
//            )
//            Icon(
//                imageVector = Icons.Outlined.Info,
//                contentDescription = "Info",
//                tint = Color.Black,
//                modifier = Modifier.size(24.dp)
//            )
//        }
//
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            OutlinedTextField(
//                value = simNumber,
//                onValueChange = { newValue ->
//                    if (newValue.matches(Regex("^[0-9]*U?$"))) {
//                        simNumber = newValue
//                    }
//                },
//                label = { Text("Sim Number") },
//                modifier = Modifier.weight(1f),
//                singleLine = true,
//                shape = RoundedCornerShape(8.dp)
//            )
//            Icon(
//                imageVector = Icons.Outlined.Info,
//                contentDescription = "Info",
//                tint = Color.Black,
//                modifier = Modifier.size(24.dp)
//            )
//        }

        // Device ID field with validation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = deviceId,
                onValueChange = { newValue ->
                    // ✅ Max 16 digits for Device ID
                    if (newValue.all { it.isDigit() } && newValue.length <= 16) {
                        deviceId = newValue
                    }
                },
                label = { Text("Device ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                // ✅ Show error if less than 15 digits
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

// SIM Number field with validation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = simNumber,
                onValueChange = { newValue ->
                    // ✅ Max 20 digits + 'U' for SIM Number
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
                // ✅ Show error if less than 19 digits (excluding U)
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

        // ✅ Better looking dropdown
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
                        Divider(color = Color.LightGray, thickness = 0.5.dp)
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

        // ✅ Show error message
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

//            Button(
//                onClick = {
//                    // ✅ Check all fields including deviceType
//                    if (deviceName.isNotEmpty() &&
//                        deviceId.isNotEmpty() &&
//                        simNumber.isNotEmpty() &&
//                        deviceType.isNotEmpty()) {
//
//                        println("🔹 Calling addDevice API")
//                        println("🔹 Token: Bearer $authToken")
//                        println("🔹 Device Name: $deviceName")
//                        println("🔹 Device ID: $deviceId")
//                        println("🔹 SIM Number: $simNumber")
//                        println("🔹 Device Type: $deviceType")
//
//                        viewModel.addDevice(
//                            token = authToken,
//                            deviceName = deviceName,
//                            deviceId = deviceId,
//                            simNumber = simNumber,
//                            deviceType = deviceType.lowercase() // ✅ Send as lowercase
//                        )
//                    } else {
//                        println("❌ Please fill all fields")
//                    }
//                },
//                // ✅ Enable only when ALL fields are filled
//                enabled = !isLoading &&
//                        deviceName.isNotEmpty() &&
//                        deviceId.isNotEmpty() &&
//                        simNumber.isNotEmpty() &&
//                        deviceType.isNotEmpty(),
//                modifier = Modifier.weight(0.7f),
//                colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight),
//                shape = RoundedCornerShape(100.dp)
//            ) {
//                Text(
//                    text = if (isLoading) "Adding..." else "Add Device",
//                    color = Color.White
//                )
//            }
            Button(
                onClick = {
                    // ✅ Validate all fields including length
                    val isDeviceIdValid = deviceId.length in 15..16
                    val isSimNumberValid = simNumber.removeSuffix("U").length in 19..20

                    if (deviceName.isNotEmpty() &&
                        deviceId.isNotEmpty() &&
                        simNumber.isNotEmpty() &&
                        deviceType.isNotEmpty() &&
                        isDeviceIdValid &&
                        isSimNumberValid) {

                        println("🔹 Calling addDevice API")
                        println("🔹 Token: Bearer $authToken")
                        println("🔹 Device Name: $deviceName")
                        println("🔹 Device ID: $deviceId (${deviceId.length} digits)")
                        println("🔹 SIM Number: $simNumber (${simNumber.removeSuffix("U").length} digits)")
                        println("🔹 Device Type: $deviceType")

                        viewModel.addDevice(
                            token = authToken,
                            deviceName = deviceName,
                            deviceId = deviceId,
                            simNumber = simNumber,
                            deviceType = deviceType.lowercase()
                        )
                    } else {
                        when {
                            deviceName.isEmpty() -> println("❌ Device name is empty")
                            deviceId.isEmpty() -> println("❌ Device ID is empty")
                            !isDeviceIdValid -> println("❌ Device ID must be 15-16 digits")
                            simNumber.isEmpty() -> println("❌ SIM number is empty")
                            !isSimNumberValid -> println("❌ SIM number must be 19-20 digits")
                            deviceType.isEmpty() -> println("❌ Device type not selected")
                        }
                    }
                },
                // ✅ Enable only when ALL validations pass
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