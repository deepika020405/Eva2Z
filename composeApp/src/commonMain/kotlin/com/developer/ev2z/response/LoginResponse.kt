package com.developer.ev2z.response

data class LoginResponse(
    val user: User,
    val token: String
)

data class User(
    val id: Int,
    val name: String,
    val userName: String,
    val phone: String,
    val address: String?,
    val accountId: String?,
    val device_id: String?,
    val email: String?,
    val referral_code: String?,
    val referred_by: String?,
    val photo: String?
)