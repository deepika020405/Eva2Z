package com.developer.ev2z.response


import kotlinx.serialization.Serializable

@Serializable
data class CartActionResponse(
    val success: Boolean? = null,
    val action: String? = null,
    val message: String? = null,
    val product_id: Int? = null,
    val quantity: Int? = null,
    val plan_type: String? = null,
    val device_type: String? = null
)