package com.developer.ev2z.phonepe

data class PaymentResult(
    val success: Boolean,
    val transactionId: String?,
    val message: String?
)