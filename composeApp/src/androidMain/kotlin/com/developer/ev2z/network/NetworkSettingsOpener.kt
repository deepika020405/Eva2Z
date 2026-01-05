package com.developer.ev2z.network

import android.content.Context
import android.content.Intent
import android.provider.Settings

actual class NetworkSettingsOpener(
    private val context: Context
) {

    actual fun openInternetSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
