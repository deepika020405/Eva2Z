package com.developer.ev2z.request

@kotlinx.serialization.Serializable
data class CartAddRequest(
    val action: String = "add",
    val product_id: Int,
    val quantity: Int,
    val plan_type: String?,
    val device_type: String?
)