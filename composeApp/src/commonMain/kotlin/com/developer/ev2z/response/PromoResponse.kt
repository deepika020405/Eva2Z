package com.developer.ev2z.response

@kotlinx.serialization.Serializable
data class PromoResponse(
    val success: Boolean,
    val originalAmount: String,
    val discountApplied: Double,
    val finalAmount: Double,
    val appliedCode: String,
    val codeType: String
)