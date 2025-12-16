package com.developer.ev2z.phonepe

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openPhonePeSdk(
    paymentUrl: String,
    onResult: (PaymentResult) -> Unit
) {
    PaymentBus.register { success, txnId, reason ->
        onResult(PaymentResult(success, txnId, reason))
    }

    val url = NSURL(string = paymentUrl)
    if (url != null) {
        UIApplication.sharedApplication.openURL(url)
    } else {
        onResult(PaymentResult(false, null, "Invalid payment URL"))
    }
}
