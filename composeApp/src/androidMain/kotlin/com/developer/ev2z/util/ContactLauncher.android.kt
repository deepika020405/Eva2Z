package com.developer.ev2z.util

import android.content.Intent
import android.net.Uri
import android.content.Context

lateinit var appContexts: Context

fun initContexts(context: Context) {
    appContexts = context
}

actual fun openCall(phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    appContexts.startActivity(intent)
}

actual fun openWhatsApp(phoneNumber: String, message: String) {
    val uri = Uri.parse(
        "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
    )
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    appContexts.startActivity(intent)
}