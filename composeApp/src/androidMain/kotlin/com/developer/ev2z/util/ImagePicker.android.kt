package com.developer.ev2z.util

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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

    /* ---------------- GALLERY (NO PERMISSION) ---------------- */

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        scope.launch {
            if (uri == null) {
                onImageSelected(null)
                return@launch
            }

            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }

            onImageSelected(bytes)
        }
    }

    /* ---------------- CAMERA ---------------- */

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        scope.launch {
            if (!success || tempPhotoUri == null) {
                onImageSelected(null)
                return@launch
            }

            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(tempPhotoUri!!)?.use { it.readBytes() }
            }

            onImageSelected(bytes)
            tempPhotoUri = null
        }
    }

    // ✅ CAMERA PERMISSION HANDLER (CRASH FIX)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            tempPhotoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(tempPhotoUri!!)
        } else {
            onImageSelected(null)
        }
    }

    /* ---------------- UI ---------------- */

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Image") },
            text = { Text("Choose an image source") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) {
                    Text("Gallery")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false

                    // 🔐 CHECK CAMERA PERMISSION
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                        tempPhotoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        cameraLauncher.launch(tempPhotoUri!!)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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