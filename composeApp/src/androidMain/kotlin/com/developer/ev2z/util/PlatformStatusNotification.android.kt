package com.developer.ev2z.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.developer.ev2z.appCtx

/**
 * The tray notification for a status the app worked out itself.
 *
 * Goes through [NotificationUtil] so it lands on the same channel, with the same icon
 * and sound, as the Moving and Parked banners the server pushes — a locally raised
 * alert that looked different would read as a different app.
 */
@SuppressLint("MissingPermission") // checked below; the call cannot be reached without it
actual fun showVehicleStatusNotification(key: String, title: String, body: String) {
    // appCtx is set by MainApplication. A notification raised before that — from a
    // unit test, or from a process the Application class has not run in — is dropped
    // rather than crashing the poll that asked for it.
    val context: Context = try {
        appCtx
    } catch (_: UninitializedPropertyAccessException) {
        println("⚠️ Idle notification skipped — app context not initialised yet")
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        println("⚠️ Idle notification skipped — POST_NOTIFICATIONS not granted")
        return
    }

    // Idempotent, and cheap: the channel exists already in a normal launch, but a
    // notification is also the one thing that fails silently when it does not.
    NotificationUtil.createChannel(context)
    NotificationUtil.showVehicleStatus(context, title, body, key)
}
