// androidMain
package com.developer.ev2z.upi

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.developer.ev2z.data.local.model.UpiApp

lateinit var appContext: Context   // init once from Application

actual fun launchUpiPayment(
    app: UpiApp,
    amount: String,
    txnId: String,
    onResult: (Boolean) -> Unit
) {
    val cleanTxnId = "EV2Z${System.currentTimeMillis()}"

    val uri = Uri.parse(
        "upi://pay" +
                "?pa=9990758825@ptyes" +
                "&pn=EV2Z" +
                "&tr=$cleanTxnId" +
                "&am=$amount" +
                "&cu=INR"
    )

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        appContext.startActivity(intent)

        // ⚠️ DO NOT call success/failure here
        onResult(true) // means "UPI APP OPENED"
    } catch (e: Exception) {
        onResult(false)
    }
}