//// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceScreen.kt
//package com.developer.ev2z.ui.screens.AddDevice
//
//import androidx.compose.runtime.Composable
//
//typealias NavigationCallback = (String) -> Unit
//
//// ✅ Added viewModel parameter
//@Composable
//expect fun BottomSheet(
//    onNavigate: NavigationCallback,
//    viewModel: AddDeviceViewModel
//)
//
//@Composable
//expect fun AddDevice(
//    onNavigate: NavigationCallback,
//    viewModel: AddDeviceViewModel
//)
//
//@Composable
//expect fun OpenCamera(
//    scanStep: Int,
//    onBarcodeDetected: (String) -> Unit
//)
//
//expect fun ScanBarcode(
//    scanner: Any,
//    imageProxy: Any,
//    scanStep: Int,
//    onBarcodeDetected: (String) -> Unit
//)
//
//expect fun ProcessBarcodeData(
//    data: String,
//    format: Int,
//    scanStep: Int,
//    onBarcodeDetected: (String) -> Unit
//)
//
//expect fun FillDeviceDetails(
//    imageUri: Any,
//    context: Any,
//    scanStep: Int,
//    onResult: (String) -> Unit
//)
//
//// ✅ Fixed name to match Android
//@Composable
//expect fun AddDeviceDeteails(
//    onNavigate: NavigationCallback,
//    viewModel: AddDeviceViewModel
//)