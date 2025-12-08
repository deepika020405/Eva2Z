// composeApp/src/iosMain/kotlin/com/developer/ev2z/util/ImagePicker.ios.kt
package com.developer.ev2z.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*

@Composable
actual fun rememberImagePicker(
    onImageSelected: (ByteArray?) -> Unit
): ImagePickerLauncher {
    var showDialog by remember { mutableStateOf(false) }

    // Show picker dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onImageSelected(null)
            },
            title = { Text("Select Image") },
            text = { Text("Choose an option to select your profile picture") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        // TODO: Launch iOS Photo Library
                        println("📸 iOS: Gallery selected")
                        onImageSelected(null)
                    }
                ) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        // TODO: Launch iOS Camera
                        println("📸 iOS: Camera selected")
                        onImageSelected(null)
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