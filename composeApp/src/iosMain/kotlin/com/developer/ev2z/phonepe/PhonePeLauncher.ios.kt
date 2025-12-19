package com.developer.ev2z.phonepe

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSString
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual fun openPhonePeSdk(
    paymentUrl: String,
    onResult: (PaymentResult) -> Unit
) {
    // ✅ Register callback
    PaymentBus.register { success, txnId, reason ->
        println("📞 iOS Callback: success=$success txnId=$txnId")
        onResult(PaymentResult(success, txnId, reason))
    }

    try {
        // ✅ Build PhonePe payment URL
        val phonePeWebUrl = "https://mercury.phonepe.com/transact/pg?token=$paymentUrl"

        println("🚀 Opening In-App PhonePe WebView")
        println("🔗 URL: $phonePeWebUrl")

        // ✅ Post notification to Swift layer (CORRECTED)
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "OpenPaymentWebView",
            anObject = phonePeWebUrl as NSObject  // ✅ Direct cast to NSObject
        )

        println("✅ Payment WebView notification posted")

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        onResult(PaymentResult(false, null, "Failed to open payment: ${e.message}"))
    }
}