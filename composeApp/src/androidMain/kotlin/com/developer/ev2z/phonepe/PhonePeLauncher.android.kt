// androidMain/com/developer/ev2z/phonepe/PhonePeLauncher.android.kt
package com.developer.ev2z.phonepe

import android.content.Intent
import android.util.Log
import com.developer.ev2z.ActivityProvider

actual fun openPhonePeSdk(
    paymentUrl: String, // orderToken
    onResult: (PaymentResult) -> Unit
) {
    val activity = ActivityProvider.getActivity() ?: run {
        Log.e("PhonePe", "❌ Activity not found")
        onResult(PaymentResult(false, null, "Activity not available"))
        return
    }

    try {
        val orderToken = paymentUrl
        val orderId = "OMO${System.currentTimeMillis()}"

        Log.d("PhonePe", "🚀 Launching Payment Handler")
        Log.d("PhonePe", "🔑 Order Token: ${orderToken.take(50)}...")
        Log.d("PhonePe", "🆔 Order ID: $orderId")

        // ✅ Register callback
        PaymentBus.register { success, txnId, reason ->
            Log.d("PhonePe", "📞 Callback: success=$success")
            onResult(PaymentResult(success, txnId, reason))
        }

        // ✅ Launch PaymentHandlerActivity
        val intent = Intent(activity, PaymentHandlerActivity::class.java).apply {
            putExtra("ORDER_TOKEN", orderToken)
            putExtra("ORDER_ID", orderId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        activity.startActivity(intent)
        Log.d("PhonePe", "✅ Payment handler launched")

    } catch (e: Exception) {
        Log.e("PhonePe", "❌ Error: ${e.message}", e)
        onResult(PaymentResult(false, null, e.message))
    }
}