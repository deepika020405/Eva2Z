// iosMain
package com.developer.ev2z.upi

import com.developer.ev2z.data.local.model.UpiApp
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIApplication

actual fun launchUpiPayment(
    activity: Any,   // 👈 added to match expect
    app: UpiApp,
    amount: String,
    txnId: String,
    onResult: (Boolean) -> Unit
) {



    val urlString =
        "upi://pay" +
                "?pa=9990758825@ptyes" +
                "&pn=EV2Z" +
                "&tr=$txnId" +
                "&am=$amount" +
                "&cu=INR"

    val url = NSURL.URLWithString(urlString)

    if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
        onResult(true)
    } else {
        onResult(false)
    }
}
