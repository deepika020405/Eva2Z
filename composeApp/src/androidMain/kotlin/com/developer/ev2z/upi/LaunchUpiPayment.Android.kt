// androidMain
package com.developer.ev2z.upi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.developer.ev2z.data.local.model.UpiApp

lateinit var appContext: Context   // init once from Application

actual fun launchUpiPayment(
    activity: Any,
    app: UpiApp,
    amount: String,
    txnId: String,
    onResult: (Boolean) -> Unit
) {
    val act = activity as Activity

    val formattedAmount = "%.2f".format(amount.toDouble())

    val uri = Uri.parse(
        "upi://pay" +
                "?pa=9990758825@ptyes" +
                "&pn=EV2Z" +
                "&tr=$txnId" +
                "&am=$formattedAmount" +
                "&cu=INR"
    )

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage(app.packageName)
    }

    try {
        act.startActivity(intent)
        onResult(true)
    } catch (e: Exception) {
        onResult(false)
    }
}
