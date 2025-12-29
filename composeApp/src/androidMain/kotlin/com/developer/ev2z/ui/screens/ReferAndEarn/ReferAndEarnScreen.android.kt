package com.developer.ev2z.ui.screens.ReferAndEarn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.ComponentActivity

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
            putExtra(Intent.EXTRA_SUBJECT, "Join EVA2Z Connect - Get Rewards!")
            putExtra(Intent.EXTRA_TEXT, message)
        }

        val chooserIntent = Intent.createChooser(intent, "Share Referral Code")
        androidContext.startActivity(chooserIntent)
        Log.d("ReferEarn", "✅ Opening share sheet")
    } catch (e: Exception) {
        Log.e("ReferEarn", "❌ Share failed: ${e.message}")
    }
}

// ✅ NEW: Contact Picker for Android
actual fun openContactPicker(context: Any?, message: String) {
    val androidContext = context as? ComponentActivity ?: run {
        Log.e("ReferEarn", "Context is not ComponentActivity, cannot open contacts")
        return
    }

    try {
        // Create SMS intent with the referral message
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", message)
        }

        // Check if SMS app is available
        if (smsIntent.resolveActivity(androidContext.packageManager) != null) {
            androidContext.startActivity(smsIntent)
            Log.d("ReferEarn", "✅ Opening SMS with message")
        } else {
            Log.e("ReferEarn", "❌ No SMS app found")
            // Fallback to generic share
            shareViaOther(context, message)
        }
    } catch (e: Exception) {
        Log.e("ReferEarn", "❌ Contact picker failed: ${e.message}")
        // Fallback to generic share
        shareViaOther(context, message)
    }
}