package com.developer.ev2z.util

import android.content.Intent
import android.util.Log

actual fun shareText(text: String, subject: String) {
    try {
        // Reuses the application context installed by initOpenUrl() in MainActivity.
        // Throws UninitializedPropertyAccessException if that never ran — caught below.
        val context = OpenUrlHolder.context

        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // Started from the application context, so the chooser needs its own task.
        val chooser = Intent.createChooser(send, subject).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Log.e("ShareSheet", "Share failed: ${e.message}")
    }
}