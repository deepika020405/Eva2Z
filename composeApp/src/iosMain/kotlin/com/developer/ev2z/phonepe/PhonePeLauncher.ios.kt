// iosMain/com/developer/ev2z/phonepe/PhonePeLauncher.ios.kt
package com.developer.ev2z.phonepe

import platform.Foundation.NSNotificationCenter

actual fun openPhonePeSdk(
    orderToken: String,
    orderId: String,
    onResult: (PaymentResult) -> Unit
) {
    println("🚀 iOS PhonePe launcher called")
    println("🔑 Token: ${orderToken.take(50)}...")
    println("🆔 Order ID: $orderId")

    // ✅ Register callback FIRST
    PaymentBus.register { success, txnId, reason ->
        println("📞 iOS Payment Callback: success=$success, txnId=$txnId, reason=$reason")
        onResult(PaymentResult(success, txnId, reason))
    }

    try {
        // userInfo, not a URL. The token is base64 and used to be smuggled through a
        // "…/pg?token=$token" query string that PaymentBridge parsed back out — a
        // round-trip that silently truncates on any '#' or '&' the token happens to
        // carry, and that had nowhere to put the order id.
        println("📲 Posting payment request to PaymentBridge")
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "OpenPaymentWebView",
            `object` = null,
            userInfo = mapOf<Any?, Any?>(
                "token" to orderToken,
                "orderId" to orderId
            )
        )
        println("✅ Payment notification posted successfully")

    } catch (e: Exception) {
        println("❌ iOS Payment Error: ${e.message}")
        e.printStackTrace()
        onResult(PaymentResult(false, null, "Failed to open payment: ${e.message}"))
    }
}