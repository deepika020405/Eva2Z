// iosMain/com/developer/ev2z/phonepe/PhonePeLauncher.ios.kt
package com.developer.ev2z.phonepe

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSString
import platform.darwin.NSObject

actual fun openPhonePeSdk(
    paymentUrl: String,
    onResult: (PaymentResult) -> Unit
) {
    println("🚀 iOS PhonePe launcher called")
    println("🔗 Payment URL/Token: ${paymentUrl.take(50)}...")

    // ✅ Register callback FIRST
    PaymentBus.register { success, txnId, reason ->
        println("📞 iOS Payment Callback: success=$success, txnId=$txnId, reason=$reason")
        onResult(PaymentResult(success, txnId, reason))
    }

    try {
        // ✅ Build PhonePe payment URL with token parameter
        val phonePeWebUrl = "https://mercury.phonepe.com/transact/pg?token=$paymentUrl"

        println("📲 Posting notification with URL")

        // ✅ Cast to NSString then NSObject
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "OpenPaymentWebView",
            `object` = phonePeWebUrl as NSString as NSObject
        )

        println("✅ Payment notification posted successfully")

    } catch (e: Exception) {
        println("❌ iOS Payment Error: ${e.message}")
        e.printStackTrace()
        onResult(PaymentResult(false, null, "Failed to open payment: ${e.message}"))
    }
}