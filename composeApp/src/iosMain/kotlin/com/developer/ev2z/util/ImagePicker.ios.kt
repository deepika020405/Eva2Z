package com.developer.ev2z.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePicker(
    onImageSelected: (ByteArray?) -> Unit
): ImagePickerLauncher {

    var showDialog by remember { mutableStateOf(false) }
    val uiViewController = LocalUIViewController.current

    /* ---------------- SAFE DELEGATE ---------------- */

    class PickerDelegate(
        private val onResult: (ByteArray?) -> Unit
    ) : NSObject(),
        UIImagePickerControllerDelegateProtocol,
        UINavigationControllerDelegateProtocol {

        override fun imagePickerController(
            picker: UIImagePickerController,
            didFinishPickingMediaWithInfo: Map<Any?, *>
        ) {
            var selectedImage: UIImage? = null

            // ✅ SAFEST way in KMP
            for (value in didFinishPickingMediaWithInfo.values) {
                if (value is UIImage) {
                    selectedImage = value
                    break
                }
            }

            val bytes = selectedImage?.let { image ->
                val data = UIImageJPEGRepresentation(image, 0.8)
                data?.let { d ->
                    ByteArray(d.length.toInt()).also { arr ->
                        memcpy(arr.refTo(0), d.bytes, d.length)
                    }
                }
            }

            picker.dismissViewControllerAnimated(true) {
                // 🔥 MAIN THREAD UI UPDATE (THIS FIXES THE CRASH)
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(bytes)
                }
            }
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true) {
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(null)
                }
            }
        }
    }

    val galleryDelegate = remember { PickerDelegate(onImageSelected) }
    val cameraDelegate = remember { PickerDelegate(onImageSelected) }

    /* ---------------- UI ---------------- */

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Image") },
            text = { Text("Choose an image from camera or gallery") },

            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    dispatch_async(dispatch_get_main_queue()) {
                        val picker = UIImagePickerController().apply {
                            sourceType =
                                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                            allowsEditing = false
                            delegate = galleryDelegate
                        }
                        uiViewController.presentViewController(
                            picker,
                            animated = true,
                            completion = null
                        )
                    }
                }) {
                    Text("Gallery")
                }
            },

            dismissButton = {
                TextButton(onClick = {
                    showDialog = false

                    if (!UIImagePickerController.isSourceTypeAvailable(
                            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                        )
                    ) {
                        onImageSelected(null)
                        return@TextButton
                    }

                    dispatch_async(dispatch_get_main_queue()) {
                        val picker = UIImagePickerController().apply {
                            sourceType =
                                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                            allowsEditing = false
                            delegate = cameraDelegate
                        }
                        uiViewController.presentViewController(
                            picker,
                            animated = true,
                            completion = null
                        )
                    }
                }) {
                    Text("Camera")
                }
            }
        )
    }

    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                showDialog = true
            }
        }
    }
}