package com.developer.ev2z.phonepe

//expect fun openPhonePeSdk(
//    orderToken: String,
//    merchantId: String,
//    onResult: (PaymentResult) -> Unit
//)


expect fun openPhonePeSdk(
    paymentUrl: String,
    onResult: (PaymentResult) -> Unit
)
