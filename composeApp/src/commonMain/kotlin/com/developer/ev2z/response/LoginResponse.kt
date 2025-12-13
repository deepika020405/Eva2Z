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
    val accountId: String? = null,
    val device_id: String? = null,
    val referral_code: String? = null,
    val referred_by: String? = null,
    val email: String? = null,
    val photo: String? = null,
    val address: String? = null
)

// ===== All other Auth Responses =====

@Serializable
data class SignUpResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null
)

@Serializable
data class OtpResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: User? = null
)

@Serializable
data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)