// iosMain
package com.developer.ev2z.upi

import com.developer.ev2z.data.local.model.UpiApp
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual fun launchUpiPayment(
    app: UpiApp,
    amount: String,
    txnId: String,
    onResult: (Boolean) -> Unit
) {
    val cleanTxnId = "EV2Z${Clock.System.now().toEpochMilliseconds()}"

    val url =
        "${app.scheme}?" +
                "pa=9990758825@ptyes" +
                "&pn=EV2Z" +
                "&tr=$cleanTxnId" +
                "&am=$amount" +
                "&cu=INR"

    UIApplication.sharedApplication.openURL(
        NSURL.URLWithString(url)!!
    )

    // iOS → no callback possible
    onResult(true)
}