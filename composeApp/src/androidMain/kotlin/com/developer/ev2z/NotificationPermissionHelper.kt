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
        ) { granted: Boolean ->
            if (granted) {
                requestSmsPermission() // ✅ notification granted → now ask SMS
            }
        }

    // 📩 SMS permission launcher
    private val smsLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { _ ->
            onGranted() // ✅ SMS granted or denied — proceed either way
        }

    fun checkAndRequest() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requestSmsPermission() // skip notification check on old Android
            return
        }

        val notificationGranted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (notificationGranted) {
            requestSmsPermission()
        } else {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestSmsPermission() {
        val smsGranted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (smsGranted) {
            onGranted()
        } else {
            smsLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }
}