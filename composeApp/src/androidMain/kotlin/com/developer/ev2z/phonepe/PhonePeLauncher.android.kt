package com.developer.ev2z.phonepe

import android.content.Intent
import android.net.Uri
import com.developer.ev2z.getAppContext

actual fun openPhonePeCheckout(redirectUrl: String) {
    val ctx = getAppContext() as android.content.Context

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    ctx.startActivity(intent)
}