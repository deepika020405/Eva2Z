package com.developer.ev2z

import android.app.Application
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable


lateinit var appCtx: Application

fun initAppContext(app: Application) {
    appCtx = app
}

actual fun getAppContext(): Any? = appCtx

actual fun cancelCallNotification() {
    // Stopping the service also dismisses the foreground notification automatically.
    CallRingtoneService.stop(appCtx)
}

actual fun stopCallSound() {
    // CallRingtoneService owns both the ringtone and the notification.
    CallRingtoneService.stop(appCtx)
}

@Composable
actual fun getPlatformContext(): Any? = LocalContext.current