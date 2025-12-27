package com.developer.ev2z.ui.screens.ReferAndEarn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

actual fun shareViaWhatsApp(context: Any?, message: String) {
    val androidContext = context as? Context ?: run {
        Log.e("ReferEarn", "Context is null, cannot share")
        return
    }

    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, message)
        }

        // Check if WhatsApp is installed
        if (intent.resolveActivity(androidContext.packageManager) != null) {
            androidContext.startActivity(intent)
            Log.d("ReferEarn", "✅ Opening WhatsApp")
        } else {
            // WhatsApp not installed, fallback to browser
            val whatsappUrl = "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
            androidContext.startActivity(browserIntent)
            Log.d("ReferEarn", "✅ Opening WhatsApp Web")
        }
    } catch (e: Exception) {
        Log.e("ReferEarn", "❌ WhatsApp share failed: ${e.message}")
        // Fallback to generic share
        shareViaOther(context, message)
    }
}

actual fun shareViaOther(context: Any?, message: String) {
    val androidContext = context as? Context ?: run {
        Log.e("ReferEarn", "Context is null, cannot share")
        return
    }

    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Join EVA2Z Connect - Get ₹150 OFF!")
            putExtra(Intent.EXTRA_TEXT, message)
        }

        val chooserIntent = Intent.createChooser(intent, "Share Referral Code")
        androidContext.startActivity(chooserIntent)
        Log.d("ReferEarn", "✅ Opening share sheet")
    } catch (e: Exception) {
        Log.e("ReferEarn", "❌ Share failed: ${e.message}")
    }
}