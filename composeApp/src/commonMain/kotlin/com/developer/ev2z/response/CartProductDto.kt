package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class CartProductDto(
    val id: Int,
    val name: String,
    val sale_price: String,
    val recharge_price: String? = null,
    val image: String? = null
)

@Serializable
data class CartItemDto(
    val id: Int,
    val user_id: Int,
    val product_id: Int,
    val quantity: Int,
    val price: String,
    val created_at: String,
    val updated_at: String,
    val plan_type: String? = null,
    val device_type: String? = null,
    val recharge_amount: String? = null,
    val product: CartProductDto? = null
)

@Serializable
data class CartResponse(
    val success: Boolean,
    val cart: List<CartItemDto> = emptyList(),
    val totalPrice: String? = null
)