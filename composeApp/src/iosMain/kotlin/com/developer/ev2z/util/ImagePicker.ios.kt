// composeApp/src/iosMain/kotlin/com/developer/ev2z/util/ImagePicker.ios.kt
package com.developer.ev2z.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import platform.UIKit.*
import platform.Foundation.NSData
import platform.posix.memcpy
import androidx.compose.ui.interop.LocalUIViewController
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePicker(
    onImageSelected: (ByteArray?) -> Unit
): ImagePickerLauncher {
    var showDialog by remember { mutableStateOf(false) }
    val uiViewController = LocalUIViewController.current

    // Camera delegate
    val cameraDelegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
                    ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                image?.let {
                    // Convert UIImage to ByteArray
                    val imageData = UIImageJPEGRepresentation(it, 0.8) // 80% quality
                    imageData?.let { data ->
                        val bytes = ByteArray(data.length.toInt())
                        memcpy(bytes.refTo(0), data.bytes, data.length)
                        println("✅ iOS: Image captured, size: ${bytes.size} bytes")
                        onImageSelected(bytes)
                    } ?: run {
                        println("❌ iOS: Failed to convert image to data")
                        onImageSelected(null)
                    }
                } ?: run {
                    println("❌ iOS: No image found in picker result")
                    onImageSelected(null)
                }

                picker.dismissViewControllerAnimated(true, completion = null)
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                println("📸 iOS: Camera cancelled")
                picker.dismissViewControllerAnimated(true, completion = null)
                onImageSelected(null)
            }
        }
    }

    // Gallery delegate
    val galleryDelegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
                    ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                image?.let {
                    val imageData = UIImageJPEGRepresentation(it, 0.8)
                    imageData?.let { data ->
                        val bytes = ByteArray(data.length.toInt())
                        memcpy(bytes.refTo(0), data.bytes, data.length)
                        println("✅ iOS: Image selected from gallery, size: ${bytes.size} bytes")
                        onImageSelected(bytes)
                    } ?: run {
                        println("❌ iOS: Failed to convert image to data")
                        onImageSelected(null)
                    }
                } ?: run {
                    println("❌ iOS: No image found in picker result")
                    onImageSelected(null)
                }

                picker.dismissViewControllerAnimated(true, completion = null)
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                println("📸 iOS: Gallery cancelled")
                picker.dismissViewControllerAnimated(true, completion = null)
                onImageSelected(null)
            }
        }
    }

    // Show picker dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = { Text("Select Image") },
            text = { Text("Choose an image from camera or gallery") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        // Launch Gallery
                        val picker = UIImagePickerController()
                        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                        picker.allowsEditing = true
                        picker.delegate = galleryDelegate
                        println("📸 iOS: Opening photo library")
                        uiViewController.presentViewController(picker, animated = true, completion = null)
                    }
                ) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        // Check if camera is available
                        if (UIImagePickerController.isSourceTypeAvailable(
                                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                            )
                        ) {
                            // Launch Camera
                            val picker = UIImagePickerController()
                            picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                            picker.allowsEditing = true
                            picker.delegate = cameraDelegate
                            println("📸 iOS: Opening camera")
                            uiViewController.presentViewController(picker, animated = true, completion = null)
                        } else {
                            println("❌ iOS: Camera not available on this device")
                            onImageSelected(null)
                        }
                    }
                ) {
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



