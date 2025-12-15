// PhonePe.android.kt
package com.developer.ev2z.phonepe

import android.app.Activity
import android.util.Log
import com.developer.ev2z.ActivityProvider
import com.phonepe.intent.sdk.api.PhonePe
import com.phonepe.intent.sdk.api.PhonePeInitException

actual fun openPhonePeSdk(
    orderToken: String,
    merchantId: String,
    onResult: (PaymentResult) -> Unit
) {
    // ✅ ActivityProvider se Activity lo
    val activity = ActivityProvider.getActivity() ?: run {
        Log.e("PhonePe", "Activity not found in ActivityProvider")
        onResult(PaymentResult(false, null, "Activity not available"))
        return
    }

    try {
        // Get list of UPI apps
        val upiApps = PhonePe.getUpiApps()

        if (upiApps.isEmpty()) {
            Log.e("PhonePe", "No UPI apps found")
            onResult(PaymentResult(false, null, "No UPI apps installed"))
            return
        }

        Log.d("PhonePe", "Available UPI apps: ${upiApps.map { it.packageName }}")

        // Store callback for later
        PaymentBus.register { success, txnId, reason ->
            onResult(PaymentResult(success, txnId, reason))
        }

        // Start PhonePe transaction
        val intent = PhonePe.getImplicitIntent(
            activity.applicationContext,
            orderToken,
            null
        )

        if (intent != null) {
            activity.startActivityForResult(intent, PHONEPE_REQUEST_CODE)
            Log.d("PhonePe", "✅ Payment intent launched successfully")
        } else {
            Log.e("PhonePe", "Failed to create PhonePe intent")
            PaymentBus.notify(false, null, "Failed to start payment")
        }

    } catch (e: PhonePeInitException) {
        Log.e("PhonePe", "Init failed: ${e.message}", e)
        onResult(PaymentResult(false, null, "PhonePe initialization failed"))
    } catch (e: Exception) {
        Log.e("PhonePe", "Payment failed: ${e.message}", e)
        onResult(PaymentResult(false, null, e.message))
    }
}