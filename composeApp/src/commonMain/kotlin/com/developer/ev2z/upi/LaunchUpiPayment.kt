// commonMain
package com.developer.ev2z.upi

import com.developer.ev2z.data.local.model.UpiApp

expect fun launchUpiPayment(
    app: UpiApp,
    amount: String,
    txnId: String,
    onResult: (Boolean) -> Unit
)