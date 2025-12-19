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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PhonePe", "🏗️ PaymentHandlerActivity created")

        // ✅ 1. Register launcher FIRST (mandatory before SDK call)
        launcher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handlePaymentResult(result.resultCode, result.data)
        }

        // ✅ 2. Initialize PhonePe SDK (EXACT SAME AS YOUR OLD CODE)
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

        // ✅ 4. Start PhonePe checkout (EXACT SAME AS YOUR OLD CODE)
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

        // ✅ Check for SDK error codes (EXACT SAME AS YOUR OLD CODE)
        val sdkErrorCode = data?.getStringExtra("key_error_code")
        val sdkErrorResult = data?.getStringExtra("key_error_result")

        if (!sdkErrorCode.isNullOrBlank()) {
            Log.e("PhonePe", "⚠️ SDK Error: code=$sdkErrorCode result=$sdkErrorResult")
            PaymentBus.notify(false, null, "Payment error: $sdkErrorCode")
            finish()
            return
        }

        // ✅ Handle result codes (EXACT SAME AS YOUR OLD CODE)
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.i("PhonePe", "✅ Payment SUCCESS")
                PaymentBus.notify(true, null, null)
            }
            Activity.RESULT_CANCELED -> {
                Log.w("PhonePe", "⚠️ Payment CANCELLED")
                PaymentBus.notify(false, null, "Payment cancelled by user")
            }
            else -> {
                Log.e("PhonePe", "❌ Payment FAILED: code=$resultCode")
                PaymentBus.notify(false, null, "Payment failed")
            }
        }

        finish()
    }
}