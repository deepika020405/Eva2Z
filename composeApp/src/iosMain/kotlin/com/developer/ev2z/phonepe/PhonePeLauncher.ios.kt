// iosMain/com/developer/ev2z/phonepe/PhonePeLauncher.ios.kt
package com.developer.ev2z.phonepe

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
actual fun openPhonePeSdk(
    paymentUrl: String,
    onResult: (PaymentResult) -> Unit
) {
    // ✅ Register callback
    PaymentBus.register { success, txnId, reason ->
        println("📞 iOS Callback: success=$success")
        onResult(PaymentResult(success, txnId, reason))
    }

    try {
        // ✅ PhonePe Web Checkout URL
        val phonePeWebUrl = "https://mercury.phonepe.com/transact/pg?token=$paymentUrl"

        println("🚀 Opening PhonePe WebView: $phonePeWebUrl")

        val url = NSURL.URLWithString(phonePeWebUrl)

        if (url != null) {
            UIApplication.sharedApplication.openURL(url)
            println("✅ PhonePe payment opened")
        } else {
            println("❌ Invalid payment URL")
            onResult(PaymentResult(false, null, "Invalid payment URL"))
        }
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        onResult(PaymentResult(false, null, "Failed to open payment: ${e.message}"))
    }
}