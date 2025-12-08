// composeApp/src/androidMain/kotlin/com/developer/ev2z/util/ImagePicker.android.kt
package com.developer.ev2z.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberImagePicker(
    onImageSelected: (ByteArray?) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                onImageSelected(bytes)
            } catch (e: Exception) {
                println("❌ Error reading image: ${e.message}")
                onImageSelected(null)
            }
        } ?: onImageSelected(null)
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    onImageSelected(bytes)
                } catch (e: Exception) {
                    println("❌ Error reading camera image: ${e.message}")
                    onImageSelected(null)
                }
            }
        } else {
            onImageSelected(null)
        }
    }

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
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        // Create temp file for camera
                        val tempFile = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
                        tempFile.createNewFile()
                        tempPhotoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        cameraLauncher.launch(tempPhotoUri!!)
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