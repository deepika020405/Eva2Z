// PhonePe.android.kt
package com.developer.ev2z.phonepe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.developer.ev2z.ActivityProvider
import java.net.URLEncoder

actual fun openPhonePeSdk(
    orderToken: String,
    merchantId: String,
    onResult: (PaymentResult) -> Unit
) {
    val activity = ActivityProvider.getActivity() ?: run {
        Log.e("PhonePe", "❌ Activity not found")
        onResult(PaymentResult(false, null, "Activity not available"))
        return
    }

    try {
        Log.d("PhonePe", "🚀 Starting payment")

        // Register callback FIRST
        PaymentBus.register { success, txnId, reason ->
            Log.d("PhonePe", "💰 Callback: success=$success, txn=$txnId, reason=$reason")
            onResult(PaymentResult(success, txnId, reason))
        }

        // ✅ URL ENCODE the token
        val encodedToken = URLEncoder.encode(orderToken, "UTF-8")
        val paymentUrl = "https://mercury.phonepe.com/transact/$encodedToken"

        Log.d("PhonePe", "🌐 Opening payment URL")
        Log.d("PhonePe", "Original token: ${orderToken.take(50)}...")
        Log.d("PhonePe", "Encoded URL: ${paymentUrl.take(100)}...")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        activity.startActivity(intent)

        Log.d("PhonePe", "✅ Browser opened")

    } catch (e: Exception) {
        Log.e("PhonePe", "❌ Error: ${e.message}", e)
        onResult(PaymentResult(false, null, e.message))
    }
}