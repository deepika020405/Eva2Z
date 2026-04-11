package com.developer.ev2z.ui.screens.Profile

import android.content.Context
import android.content.Intent
import android.net.Uri

actual fun platformOpenUrl(url: String) {
    try {
        val context = GlobalContext.get()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: if market:// scheme fails (no Play Store app), open web URL
        if (url.startsWith("market://")) {
            try {
                val webUrl = url.replace("market://details?id=", "https://play.google.com/store/apps/details?id=")
                val context = GlobalContext.get()
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        } else {
            e.printStackTrace()
        }
    }
}

actual fun getPlatformStoreUrl(): String {
    return "market://details?id=com.developer.ev2z"
}

fun getPlatformStoreUrlFallback(): String {
    return "https://play.google.com/store/apps/details?id=com.developer.ev2z"
}

// Global context holder
object GlobalContext {
    private var context: Context? = null

    fun set(context: Context) {
        this.context = context.applicationContext
    }

    fun get(): Context {
        return context ?: throw IllegalStateException("Context not initialized")
    }
}