// commonMain
package com.developer.ev2z.upi

import com.developer.ev2z.data.local.model.UpiApp

expect fun launchUpiPayment(
    activity: Any,
    app: UpiApp,
    amount: String,
    txnId: String,
    onResult: (Boolean) -> Unit
)
