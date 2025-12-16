package com.developer.ev2z.phonepe

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.developer.ev2z.ActivityProvider

actual fun openPhonePeSdk(
    paymentUrl: String, // 👈 yahan hum orderToken pass karenge
    onResult: (PaymentResult) -> Unit
) {
    val activity = ActivityProvider.getActivity() ?: run {
        Log.e("PhonePe", "❌ Activity not found")
        onResult(PaymentResult(false, null, "Activity not available"))
        return
    }

    try {
        // 🔥 paymentUrl me actually ORDER TOKEN aayega
        val orderToken = paymentUrl

        val deepLink =
            "phonepe://pay?token=$orderToken"

        Log.d("PhonePe", "🚀 Opening PhonePe App")
        Log.d("PhonePe", "🔗 $deepLink")

        PaymentBus.register { success, txnId, reason ->
            onResult(PaymentResult(success, txnId, reason))
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(deepLink)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        activity.startActivity(intent)

    } catch (e: Exception) {
        Log.e("PhonePe", "❌ Error opening PhonePe", e)
        onResult(PaymentResult(false, null, e.message))
    }
}
