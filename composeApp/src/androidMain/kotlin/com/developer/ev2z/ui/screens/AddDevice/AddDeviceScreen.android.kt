// composeApp/src/androidMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.android.kt
@file:OptIn(ExperimentalMaterial3Api::class)

package com.developer.ev2z.ui.screens.AddDevice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.developer.ev2z.navigation.PlatformBackHandler
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
actual fun BottomSheet(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    BottomSheetCommon(
        onNavigate = onNavigate,
        viewModel = viewModel,
        cameraScreen = { AddDevice(onNavigate, viewModel) }
    )
}

@Composable
actual fun AddDevice(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    PlatformBackHandler(enabled = true) {
        onNavigate("home") // ya jo bhi tumhara back route ho
    }
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scanStep by remember { mutableStateOf(1) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
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

    AddDeviceCommon(
        onNavigate = onNavigate,
        viewModel = viewModel,
        cameraView = {
            if (hasCameraPermission) {
                OpenCamera(scanStep) { result ->
                    when (scanStep) {
                        1 -> viewModel.setDeviceId(result)
                        2 -> viewModel.setSimNumber(result)
                    }
                }
            }
        },
        onGalleryClick = { galleryLauncher.launch("image/*") }
    )
}

@Composable
actual fun AddDeviceDeteails(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    AddDeviceDetailsCommon(onNavigate, viewModel)
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
    /** The bound provider, so leaving the screen can let go of the camera. */
    val boundProvider = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    AndroidView(
        factory = {
            val previewView = PreviewView(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                boundProvider[0] = cameraProvider
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val barcodeScanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            ScanBarcode(barcodeScanner, imageProxy, scanStep, onBarcodeDetected)
                        }
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
            }, ContextCompat.getMainExecutor(context))

            previewView.apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        },
        modifier = Modifier
            .fillMaxSize()
            // ✅ STATUS BAR + NAV BAR SAFE
            .windowInsetsPadding(WindowInsets.systemBars)
    )

    DisposableEffect(Unit) {
        onDispose {
            // `unbindAll`, not just the executor.
            //
            // The use cases are bound to the *Activity* lifecycle, and this app
            // navigates with Voyager — one Activity for the whole session. So leaving
            // the scanner never stopped the camera: nothing in the Activity's lifecycle
            // changed, and CameraX had no reason to let go. An ANR trace on 2026-10-13,
            // taken seven minutes after the scanner was last on screen, still showed
            // `camerahalserver` burning 51-59% CPU behind a completely different screen.
            //
            // Releasing it here is what ties the camera to the *composable's* life
            // rather than the Activity's, which is what "leaving the scanner" means.
            boundProvider[0]?.unbindAll()
            boundProvider[0] = null
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

    proxy.image?.let { mediaImage ->
        val inputImage = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    barcode.rawValue?.let { data ->
                        ProcessBarcodeData(data, barcode.format, scanStep, onBarcodeDetected)
                    }
                }
            }
            .addOnCompleteListener { proxy.close() }
    } ?: proxy.close()
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
                if (deviceId.length >= 15) {
                    onBarcodeDetected(deviceId)
                    println("✅ Android: Device ID: $deviceId")
                }
            }
        }
        2 -> {
            if (format == Barcode.FORMAT_QR_CODE) {
                println("📱 Android QR: '$data'")

                val patterns = listOf(
                    Regex("""SIM\s*NO[.:\s]+([0-9]{19,20}U?)""", RegexOption.IGNORE_CASE),
                    Regex("""simNo["\s]*[:=]["'\s]*([0-9]{19,20}U?)""", RegexOption.IGNORE_CASE),
                    Regex("""([0-9]{19,20}U)"""),
                    Regex("""([0-9]{19,20})""")
                )

                var simNumber: String? = null
                for (pattern in patterns) {
                    pattern.find(data)?.let {
                        simNumber = it.groupValues[1]
                        println("✅ Android: Pattern matched")
                        return@let
                    }
                }

                simNumber?.let { sim ->
                    val cleanSim = sim.take(20).removeSuffix("U")
                    if (cleanSim.length >= 19) {
                        onBarcodeDetected("${cleanSim}U")
                        println("✅ Android: SIM: ${cleanSim}U")
                    }
                } ?: println("❌ Android: No SIM found")
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

        scanner.process(image).addOnSuccessListener { barcodes ->
            println("📱 Android Gallery: Found ${barcodes.size} barcodes")
            barcodes.forEach { barcode ->
                barcode.rawValue?.let { data ->
                    println("📱 Android Gallery Data: '$data'")
                    ProcessBarcodeData(data, barcode.format, scanStep, onResult)
                }
            }
        }.addOnFailureListener { e ->
            println("❌ Android Gallery Error: ${e.message}")
        }
    } catch (e: Exception) {
        println("❌ Android Exception: ${e.message}")
        e.printStackTrace()
    }
}