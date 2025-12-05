// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.ios.kt
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalForeignApi::class)

package com.developer.ev2z.ui.screens.AddDevice

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.AVFoundation.*
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.Foundation.NSError
import platform.PhotosUI.*
import platform.UIKit.UIImage
import platform.UIKit.UIView
import platform.Vision.*
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue

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
    val uiViewController = LocalUIViewController.current
    var hasCameraPermission by remember { mutableStateOf(false) }

    val scannedDeviceId by viewModel.scannedDeviceId.collectAsState()
    val scannedSimNumber by viewModel.scannedSimNumber.collectAsState()

    val scanStep = remember(scannedDeviceId, scannedSimNumber) {
        when {
            scannedDeviceId.isEmpty() -> 1
            scannedSimNumber.isEmpty() -> 2
            else -> 2
        }
    }

    val pickerDelegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>
            ) {
                picker.dismissViewControllerAnimated(true, null)

                val results = didFinishPicking as List<PHPickerResult>
                val result = results.firstOrNull()

                result?.itemProvider?.let { provider ->
                    if (provider.hasItemConformingToTypeIdentifier("public.image")) {
                        provider.loadDataRepresentationForTypeIdentifier(
                            "public.image"
                        ) { data, error ->
                            data?.let { imageData ->
                                UIImage.imageWithData(imageData)?.let { uiImage ->
                                    processImageForBarcode(uiImage, scanStep, viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (status) {
            AVAuthorizationStatusAuthorized -> hasCameraPermission = true
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    hasCameraPermission = granted
                }
            }
            else -> hasCameraPermission = false
        }
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
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Camera permission required", color = Color.White)
                }
            }
        },
        onGalleryClick = {
            val configuration = PHPickerConfiguration().apply {
                selectionLimit = 1
                filter = PHPickerFilter.imagesFilter
            }
            val picker = PHPickerViewController(configuration)
            picker.delegate = pickerDelegate
            uiViewController.presentViewController(picker, true, null)
        }
    )
}

@Composable
actual fun AddDeviceDeteails(
    onNavigate: (String) -> Unit,
    viewModel: AddDeviceViewModel
) {
    AddDeviceDetailsCommon(onNavigate, viewModel)
}

@Composable
actual fun OpenCamera(
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    val captureSession = remember { AVCaptureSession() }
    val previewLayer = remember { AVCaptureVideoPreviewLayer(session = captureSession) }

    val delegate = remember(scanStep, onBarcodeDetected) {
        object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
            override fun captureOutput(
                output: AVCaptureOutput,
                didOutputSampleBuffer: platform.CoreMedia.CMSampleBufferRef?,
                fromConnection: AVCaptureConnection
            ) {
                didOutputSampleBuffer?.let { buffer ->
                    processCameraFrame(buffer, scanStep, onBarcodeDetected)
                }
            }
        }
    }

    UIKitView(
        factory = {
            val container = UIView()
            captureSession.sessionPreset = AVCaptureSessionPresetHigh

            AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)?.let { device ->
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val input = AVCaptureDeviceInput.deviceInputWithDevice(device, errorPtr.ptr)

                    if (input != null && captureSession.canAddInput(input)) {
                        captureSession.addInput(input)
                    }
                }

                val output = AVCaptureVideoDataOutput()
                output.setSampleBufferDelegate(delegate, dispatch_get_main_queue())

                if (captureSession.canAddOutput(output)) {
                    captureSession.addOutput(output)
                }
            }

            previewLayer.frame = CGRectMake(
                0.0, 0.0,
                container.bounds.useContents { size.width },
                container.bounds.useContents { size.height }
            )
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            container.layer.addSublayer(previewLayer)

            captureSession.startRunning()
            container
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose { captureSession.stopRunning() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun processCameraFrame(
    sampleBuffer: platform.CoreMedia.CMSampleBufferRef,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    CMSampleBufferGetImageBuffer(sampleBuffer)?.let { pixelBuffer ->
        val request = VNDetectBarcodesRequest { vnRequest, _ ->
            vnRequest?.results?.let { results ->
                for (i in 0 until results.count.toInt()) {
                    val observation = results.objectAtIndex(i.toULong()) as? VNBarcodeObservation
                    observation?.payloadStringValue?.let { data ->
                        val isQR = observation.symbology == VNBarcodeSymbologyQR
                        extractBarcodeData(data, isQR, scanStep, onBarcodeDetected)
                    }
                }
            }
        }

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val handler = VNImageRequestHandler(pixelBuffer, mapOf<Any?, Any>())
            handler.performRequests(listOf(request), errorPtr.ptr)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun processImageForBarcode(
    image: UIImage,
    scanStep: Int,
    viewModel: AddDeviceViewModel
) {
    image.CGImage?.let { cgImage ->
        val request = VNDetectBarcodesRequest { vnRequest, _ ->
            vnRequest?.results?.let { results ->
                for (i in 0 until results.count.toInt()) {
                    val observation = results.objectAtIndex(i.toULong()) as? VNBarcodeObservation
                    observation?.payloadStringValue?.let { data ->
                        val isQR = observation.symbology == VNBarcodeSymbologyQR
                        when (scanStep) {
                            1 -> {
                                if (!isQR) {
                                    val deviceId = data.replace(Regex("[^0-9]"), "")
                                    if (deviceId.isNotEmpty()) {
                                        viewModel.setDeviceId(deviceId)
                                    }
                                }
                            }
                            2 -> {
                                if (isQR) {
                                    val simNoRegex = Regex("""simNo="([^"]+)"""")
                                    val match = simNoRegex.find(data)
                                    val simNumber = match?.groups?.get(1)?.value ?: data
                                    val cleanSim = simNumber.removeSuffix("U")
                                    if (cleanSim.isNotEmpty()) {
                                        viewModel.setSimNumber(cleanSim + "U")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val handler = VNImageRequestHandler(cgImage, mapOf<Any?, Any>())
            handler.performRequests(listOf(request), errorPtr.ptr)
        }
    }
}

private fun extractBarcodeData(
    data: String,
    isQR: Boolean,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    when (scanStep) {
        1 -> {
            if (!isQR) {
                val deviceId = data.replace(Regex("[^0-9]"), "")
                if (deviceId.isNotEmpty()) {
                    onBarcodeDetected(deviceId)
                }
            }
        }
        2 -> {
            if (isQR) {
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

actual fun ScanBarcode(
    scanner: Any,
    imageProxy: Any,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
}

actual fun ProcessBarcodeData(
    data: String,
    format: Int,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
) {
    val isQR = (format == 256)
    extractBarcodeData(data, isQR, scanStep, onBarcodeDetected)
}

actual fun FillDeviceDetails(
    imageUri: Any,
    context: Any,
    scanStep: Int,
    onResult: (String) -> Unit
) {
}