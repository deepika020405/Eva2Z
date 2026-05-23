package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val user: User,
    val token: String,
    val device: LoginDeviceInfo? = null
)

@Serializable
data class LoginDeviceInfo(
    val id: Int = 0,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("is_active") val isActive: Int = 0,
    @SerialName("device_uuid") val deviceUuid: String? = null,
    val platform: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null
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
data class SavePasswordResponse(
    val status: Boolean,
    val message: String
)

@Serializable
data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String,
    val rateLimited: Boolean = false
)

@Serializable
data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)