package com.developer.ev2z.ui.screens.social_media

import android.content.Context
import android.content.Intent
import android.net.Uri

private lateinit var appContext: Context

fun initOpenUrl(context: Context) {
    appContext = context.applicationContext
}

actual fun openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    appContext.startActivity(intent)
}