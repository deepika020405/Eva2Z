//package com.developer.ev2z
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.activity.ComponentActivity
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//
//class NotificationPermissionHelper(
//    private val activity: ComponentActivity,
//    private val onGranted: () -> Unit
//) {
//
//    private val launcher =
//        activity.registerForActivityResult(
//            ActivityResultContracts.RequestPermission()
//        ) { granted: Boolean ->   // 👈 explicit Boolean
//            if (granted) {
//                onGranted()
//            }
//        }
//
//    fun checkAndRequest() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
//            onGranted()
//            return
//        }
//
//        val granted = ContextCompat.checkSelfPermission(
//            activity,
//            Manifest.permission.POST_NOTIFICATIONS
//        ) == PackageManager.PERMISSION_GRANTED
//
//        if (granted) {
//            onGranted()
//        } else {
//            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
//        }
//    }
//}


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

    // 🔔 Notification permission launcher
    private val notificationLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            requestMicPermission() // proceed regardless — token must be registered even if denied
        }

    // 🎤 Mic permission launcher — needed for WebRTC AudioRecord
    private val micLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            android.util.Log.d("Permissions", "🎤 RECORD_AUDIO granted=$granted")
            onGranted() // ✅ proceed regardless
        }

    fun checkAndRequest() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requestMicPermission() // skip notification check on old Android
            return
        }

        val notificationGranted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (notificationGranted) {
            requestMicPermission()
        } else {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestMicPermission() {
        val micGranted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (micGranted) {
            onGranted()
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}