package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class ReferralCodeResponse(
    val success: Boolean,
    val message: String? = null,
    val referral_code: String? = null
)