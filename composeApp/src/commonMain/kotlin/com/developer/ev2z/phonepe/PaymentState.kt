package com.developer.ev2z.phonepe

sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()

    data class Success(
        val txnId: String?,
        val amount: Int
    ) : PaymentState()

    data class Failed(
        val reason: String?,
        val amount: Int
    ) : PaymentState()
}