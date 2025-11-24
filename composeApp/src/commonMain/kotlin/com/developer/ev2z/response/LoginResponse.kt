package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val user: User,
    val token: String
)
@Serializable
data class User(
    val id: Int,
    val name: String,
    val userName: String? = null,
    val phone: String,
    val accountId: String?,
    val device_id: String?,
    val referral_code: String?,
    val referred_by: String?,
    val email: String? = null,
    val photo: String? = null,
    val address: String? = null
)