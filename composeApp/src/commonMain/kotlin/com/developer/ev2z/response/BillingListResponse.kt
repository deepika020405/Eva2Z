package com.developer.ev2z.response

@kotlinx.serialization.Serializable
data class BillingListResponse(
    val status: String,
    val billings: List<BillingDto>
)

@kotlinx.serialization.Serializable
data class BillingDto(
    val id: Int,
    val fname: String?,
    val lname: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val pincode: String?
)