package com.developer.ev2z.response

data class CartActionResponse(
    val success: Boolean,
    val action: String,
    val product_id: Int? = null,
    val quantity: Int? = null,
    val plan_type: String? = null,
    val device_type: String? = null
)