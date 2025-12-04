package com.developer.ev2z.ui.screens.AddDevice

import androidx.compose.runtime.Composable


// Navigation callback type
typealias NavigationCallback = (String) -> Unit
@Composable
expect fun BottomSheet(onNavigate: NavigationCallback)

@Composable
expect fun AddDevice(onNavigate: (String) -> Unit, viewModel: AddDeviceViewModel)

@Composable
expect fun OpenCamera(
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
)

expect fun ScanBarcode(
    scanner: Any,
    imageProxy: Any,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
)

expect fun ProcessBarcodeData(
    data: String,
    format: Int,
    scanStep: Int,
    onBarcodeDetected: (String) -> Unit
)

expect fun FillDeviceDetails(
    imageUri: Any,
    context: Any,
    scanStep: Int,
    onResult: (String) -> Unit
)

@Composable
expect fun AddDeviceDeteails(
    onNavigate: NavigationCallback,
    viewModel: AddDeviceViewModel
)