package com.developer.ev2z.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val amount: Double,
    val final_amount: Double,
    val billing_id: Int
)