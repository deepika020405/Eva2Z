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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ✅ Use common BottomSheet
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

// ✅ Use common Camera UI
@Composable
actual fun AddDevice(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scanStep by remember { mutableStateOf(1) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

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

// ✅ Use common details
@Composable
actual fun AddDeviceDeteails(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    AddDeviceDetailsCommon(onNavigate, viewModel)
}

// ✅ Platform-specific camera
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

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
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
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
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
                if (deviceId.isNotEmpty()) onBarcodeDetected(deviceId)
            }
        }
        2 -> {
            if (format == Barcode.FORMAT_QR_CODE) {
                val simNoRegex = Regex("""simNo="([^"]+)"""")
                val match = simNoRegex.find(data)
                val simNumber = match?.groups?.get(1)?.value ?: data
                val cleanSim = simNumber.removeSuffix("U")
                if (cleanSim.isNotEmpty()) onBarcodeDetected(cleanSim + "U")
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
            barcodes.forEach { barcode ->
                barcode.rawValue?.let { rawValue ->
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
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}