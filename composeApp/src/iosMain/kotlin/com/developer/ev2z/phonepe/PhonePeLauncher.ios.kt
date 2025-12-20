// iosMain/com/developer/ev2z/phonepe/PhonePeLauncher.ios.kt
package com.developer.ev2z.phonepe

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification

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

        // ✅ Post notification to Swift layer
        NSNotificationCenter.defaultCenter.postNotificationName(
            "OpenPaymentWebView",
            `object` = platform.Foundation.NSString.create(string = phonePeWebUrl)
        )

        println("✅ Payment WebView notification posted")

    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        onResult(PaymentResult(false, null, "Failed to open payment: ${e.message}"))
    }
}