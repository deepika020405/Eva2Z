package com.developer.ev2z.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderProduct(
    val product_id: Int,
    val quantity: Int,
    val price: String,
    val plan_type: String? = null,
    val device_type: String? = null
)

@Serializable
data class CreateOrderRequest(
    val amount: Double,
    val final_amount: Double,
    val billing_id: Int,
    val products: List<CreateOrderProduct>
)