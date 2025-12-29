// composeApp/src/androidMain/kotlin/com/developer/ev2z/util/ImagePicker.android.kt
package com.developer.ev2z.util

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberImagePicker(
    onImageSelected: (ByteArray?) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            println("✅ Android: Permission granted for ${pendingAction}")
            when (pendingAction) {
                "camera" -> {
                    // Create temp file and trigger camera
                    scope.launch {
                        try {
                            val tempFile = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
                            tempFile.createNewFile()
                            tempPhotoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                            println("✅ Android: Temp file created: ${tempFile.absolutePath}")
                        } catch (e: Exception) {
                            println("❌ Android: Error creating temp file: ${e.message}")
                            onImageSelected(null)
                        }
                    }
                }
            }
        } else {
            println("❌ Android: Permission denied for ${pendingAction}")
            onImageSelected(null)
        }
        pendingAction = null
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        scope.launch {
            if (uri != null) {
                println("✅ Android: Gallery image selected: $uri")
                try {
                    val bytes = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.readBytes()
                        }
                    }

                    if (bytes != null) {
                        println("✅ Android: Image read successfully, size: ${bytes.size} bytes")
                        onImageSelected(bytes)
                    } else {
                        println("❌ Android: Failed to read image bytes")
                        onImageSelected(null)
                    }
                } catch (e: Exception) {
                    println("❌ Android: Error reading gallery image: ${e.message}")
                    e.printStackTrace()
                    onImageSelected(null)
                }
            } else {
                println("❌ Android: No image selected from gallery")
                onImageSelected(null)
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        scope.launch {
            if (success) {
                println("✅ Android: Camera capture successful")
                tempPhotoUri?.let { uri ->
                    try {
                        val bytes = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                stream.readBytes()
                            }
                        }

                        if (bytes != null) {
                            println("✅ Android: Camera image read successfully, size: ${bytes.size} bytes")
                            onImageSelected(bytes)
                        } else {
                            println("❌ Android: Failed to read camera image bytes")
                            onImageSelected(null)
                        }

                        // Clean up temp file
                        withContext(Dispatchers.IO) {
                            try {
                                val file = File(uri.path ?: "")
                                if (file.exists()) {
                                    file.delete()
                                    println("✅ Android: Temp file cleaned up")
                                }
                            } catch (e: Exception) {
                                println("⚠️ Android: Failed to delete temp file: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Android: Error reading camera image: ${e.message}")
                        e.printStackTrace()
                        onImageSelected(null)
                    }
                } ?: run {
                    println("❌ Android: Temp photo URI is null")
                    onImageSelected(null)
                }
            } else {
                println("❌ Android: Camera capture cancelled or failed")
                onImageSelected(null)
            }
            tempPhotoUri = null
        }
    }

    // Launch camera when URI is ready
    LaunchedEffect(tempPhotoUri) {
        if (tempPhotoUri != null && pendingAction == null) {
            println("📸 Android: Launching camera with URI: $tempPhotoUri")
            cameraLauncher.launch(tempPhotoUri!!)
        }
    }

    // Show picker dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            title = { Text("Select Image") },
            text = { Text("Choose an option to select your profile picture") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        println("📸 Android: Gallery selected")

                        // Check gallery permission
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }

                        when {
                            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                                println("✅ Android: Gallery permission already granted")
                                galleryLauncher.launch("image/*")
                            }
                            else -> {
                                println("⚠️ Android: Requesting gallery permission")
                                pendingAction = "gallery"
                                permissionLauncher.launch(permission)
                                // Launch gallery after permission is handled
                                scope.launch {
                                    kotlinx.coroutines.delay(500)
                                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                        galleryLauncher.launch("image/*")
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        println("📸 Android: Camera selected")

                        // Check camera permission
                        when {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                                println("✅ Android: Camera permission already granted")
                                // Create temp file and set URI
                                scope.launch {
                                    try {
                                        val tempFile = File(context.cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
                                        tempFile.createNewFile()
                                        tempPhotoUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            tempFile
                                        )
                                        println("✅ Android: Temp file created for camera")
                                    } catch (e: Exception) {
                                        println("❌ Android: Error creating temp file: ${e.message}")
                                        onImageSelected(null)
                                    }
                                }
                            }
                            else -> {
                                println("⚠️ Android: Requesting camera permission")
                                pendingAction = "camera"
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
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
                println("📸 Android: Opening image picker dialog")
                showDialog = true
            }
        }
    }
}



