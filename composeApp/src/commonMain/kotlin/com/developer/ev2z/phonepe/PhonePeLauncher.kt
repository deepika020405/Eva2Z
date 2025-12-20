package com.developer.ev2z.phonepe

// ✅ Simple expect function - NO CLASS NEEDED
expect fun openPhonePeSdk(
    paymentUrl: String,
    onResult: (PaymentResult) -> Unit
)