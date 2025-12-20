// androidMain/com/developer/ev2z/phonepe/PaymentHandlerActivity.kt
package com.developer.ev2z.phonepe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.phonepe.intent.sdk.api.PhonePeKt
import com.phonepe.intent.sdk.api.models.PhonePeEnvironment

class PaymentHandlerActivity : ComponentActivity() {

    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var hasProcessedResult = false // ⭐ Prevent duplicate callbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PhonePe", "🏗️ PaymentHandlerActivity created")

        // ✅ 1. Register launcher FIRST
        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (!hasProcessedResult) {
                hasProcessedResult = true
                handlePaymentResult(result.resultCode, result.data)
            }
        }

        // ✅ 2. Initialize PhonePe SDK
        val initResult = PhonePeKt.init(
            this,
            "M22VRDGEOVBYAUAT",
            "ABCDEFGHIJK12",
            PhonePeEnvironment.RELEASE,
            false,
            null
        )

        if (!initResult) {
            Log.e("PhonePe", "❌ SDK init failed")
            PaymentBus.notify(false, null, "SDK initialization failed")
            finish()
            return
        }

        Log.d("PhonePe", "✅ PhonePe SDK initialized")

        // ✅ 3. Get order details
        val orderToken = intent.getStringExtra("ORDER_TOKEN")
        val orderId = intent.getStringExtra("ORDER_ID")

        if (orderToken.isNullOrBlank() || orderId.isNullOrBlank()) {
            Log.e("PhonePe", "❌ Missing order details")
            PaymentBus.notify(false, null, "Missing payment details")
            finish()
            return
        }

        Log.d("PhonePe", "🔑 Token: ${orderToken.take(50)}...")
        Log.d("PhonePe", "🆔 Order ID: $orderId")

        // ✅ 4. Start PhonePe checkout
        try {
            PhonePeKt.startCheckoutPage(
                this,           // context
                orderToken,     // token
                orderId,        // orderId
                launcher        // activityResultLauncher
            )

            Log.d("PhonePe", "🚀 PhonePe checkout started")

        } catch (e: Exception) {
            Log.e("PhonePe", "❌ Error starting checkout: ${e.message}", e)
            PaymentBus.notify(false, null, "Failed to start payment: ${e.message}")
            finish()
        }
    }

    private fun handlePaymentResult(resultCode: Int, data: Intent?) {
        Log.d("PhonePe", "📱 Result Code: $resultCode")

        // ✅ Check for SDK error codes
        val sdkErrorCode = data?.getStringExtra("key_error_code")
        val sdkErrorResult = data?.getStringExtra("key_error_result")

        if (!sdkErrorCode.isNullOrBlank()) {
            Log.e("PhonePe", "⚠️ SDK Error: code=$sdkErrorCode result=$sdkErrorResult")
            PaymentBus.notify(false, null, "Payment error: $sdkErrorCode")
            finish()
            return
        }

        // ✅ Handle result codes
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.i("PhonePe", "✅ Payment SUCCESS (User completed flow)")
                // ⭐ IMPORTANT: This means user completed payment FLOW
                // NOT that payment is verified by backend
                PaymentBus.notify(true, null, null)
            }
            Activity.RESULT_CANCELED -> {
                Log.w("PhonePe", "⚠️ Payment CANCELLED by user")
                PaymentBus.notify(false, null, "Payment cancelled by user")
            }
            else -> {
                Log.e("PhonePe", "❌ Payment FAILED: code=$resultCode")
                PaymentBus.notify(false, null, "Payment failed")
            }
        }

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("PhonePe", "🗑️ PaymentHandlerActivity destroyed")
    }
}