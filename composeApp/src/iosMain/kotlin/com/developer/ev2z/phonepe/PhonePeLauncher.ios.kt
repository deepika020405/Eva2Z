package com.developer.ev2z.phonepe

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openPhonePeSdk(
    orderToken: String,
    merchantId: String,
    onResult: (PaymentResult) -> Unit
) {
    // Store callback
    PaymentBus.register { success, txnId, reason ->
        onResult(PaymentResult(success, txnId, reason))
    }

    // PhonePe deep link format
    val urlString = "phonepe://pay?token=$orderToken&merchantId=$merchantId"

    val url = NSURL(string = urlString)

    if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
    } else {
        // PhonePe not installed, fallback to web
        val webUrl = NSURL(string = "https://mercury.phonepe.com/transact/$orderToken")
        if (webUrl != null) {
            UIApplication.sharedApplication.openURL(webUrl)
        } else {
            onResult(PaymentResult(false, null, "Failed to open payment"))
        }
    }
}