package com.developer.ev2z

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class NotificationPermissionHelper(
    private val activity: ComponentActivity,
    private val onGranted: () -> Unit
) {

    private val launcher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->   // 👈 explicit Boolean
            if (granted) {
                onGranted()
            }
        }

    fun checkAndRequest() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted()
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            onGranted()
        } else {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
