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
        e.printStackTrace()
    }
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