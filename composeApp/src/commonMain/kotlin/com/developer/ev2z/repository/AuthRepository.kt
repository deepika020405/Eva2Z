package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.response.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

class AuthRepository(private val api: ApiService) {

    // ============================================
    // LOGIN
    // ============================================
    suspend fun login(request: LoginRequest): LoginResponse {
        val jsonResponse = api.login(request.phone, request.password)

        // ✅ Check if success response (has token & user)
        val token = jsonResponse["token"]?.jsonPrimitive?.content
        val userObj = jsonResponse["user"]?.jsonObject

        if (token != null && userObj != null) {
            val user = parseUserFromJson(userObj)
                ?: throw Exception("Invalid user data")
            return LoginResponse(user = user, token = token)
        }

        // ✅ Error response - throw API message as-is
        val message = jsonResponse["message"]?.jsonPrimitive?.content
            ?: "Login failed"

        throw Exception(message)
    }

    // ============================================
    // SIGN UP
    // ============================================
    suspend fun signUp(name: String, phone: String, password: String, email: String): SignUpResponse {
        val jsonResponse = api.registerUser(name, phone, email)

        // ✅ Check for success response
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        val success = when {
            message.contains("OTP sent successfully", ignoreCase = true) -> true
            message.contains("already registered", ignoreCase = true) -> false
            else -> false
        }

        val userObj = jsonResponse["user"]?.jsonObject
        val user = parseUserFromJson(userObj)

        return SignUpResponse(success = success, message = message, user = user)
    }

    // ============================================
    // VERIFY OTP - ✅ FIXED SUCCESS PARSING
    // ============================================
    suspend fun verifyOtp(phone: String, otp: String): OtpResponse {
        val jsonResponse = api.verifyOtp(phone, otp)

        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        // ✅ FIX: Check multiple success indicators
        val success = when {
            // Check for explicit success field
            jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() == true -> true
            // Check message for success keywords
            message.contains("successfully", ignoreCase = true) -> true
            message.contains("verified", ignoreCase = true) -> true
            message.contains("registered successfully", ignoreCase = true) -> true
            // Check for token presence (indicates successful verification)
            jsonResponse["token"]?.jsonPrimitive?.content != null -> true
            else -> false
        }

        val token = jsonResponse["token"]?.jsonPrimitive?.content
        val userObj = jsonResponse["user"]?.jsonObject
        val user = parseUserFromJson(userObj)

        return OtpResponse(success = success, message = message, token = token, user = user)
    }

    // ============================================
    // RESEND OTP
    // ============================================
    suspend fun resendOtp(phone: String): OtpResponse {
        val jsonResponse = api.resendOtp(phone)

        val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        return OtpResponse(success = success, message = message, token = null, user = null)
    }

    // ============================================
    // SAVE PASSWORD (for registration)
    // ============================================
    suspend fun savePassword(phone: String, password: String): SavePasswordResponse {
        val jsonResponse = api.savePassword(phone, password)

        val status = jsonResponse["status"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        return SavePasswordResponse(status = status, message = message)
    }

    // ============================================
    // FORGOT PASSWORD - ✅ FIXED
    // ============================================
    suspend fun forgotPassword(phone: String): ForgotPasswordResponse {
        val jsonResponse = api.forgotPassword(phone)

        // ✅ Check for both message and error fields
        val message = jsonResponse["message"]?.jsonPrimitive?.content
        val error = jsonResponse["error"]?.jsonPrimitive?.content

        val success = when {
            error != null -> false // Has error field = user not registered
            message?.contains("OTP sent successfully", ignoreCase = true) == true -> true
            message?.contains("sent successfully", ignoreCase = true) == true -> true
            else -> false
        }

        return ForgotPasswordResponse(
            success = success,
            message = error ?: message ?: "Unknown error"
        )
    }

    // ============================================
    // VERIFY FORGOT PASSWORD OTP - ✅ FIXED
    // ============================================
    suspend fun verifyForgotOtp(phone: String, otp: String): OtpResponse {
        val jsonResponse = api.verifyForgotOtp(phone, otp)

        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        // ✅ FIX: Multiple success checks
        val success = when {
            jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() == true -> true
            message.contains("successfully", ignoreCase = true) -> true
            message.contains("verified", ignoreCase = true) -> true
            else -> false
        }

        return OtpResponse(success = success, message = message, token = null, user = null)
    }

    // ============================================
    // RESET PASSWORD
    // ============================================
    suspend fun resetPassword(phone: String, password: String, confirmPassword: String): ResetPasswordResponse {
        val jsonResponse = api.resetPassword(phone, password, confirmPassword)

        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        // ✅ FIX: Multiple success checks
        val success = when {
            jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() == true -> true
            message.contains("successfully", ignoreCase = true) -> true
            message.contains("reset", ignoreCase = true) -> true
            else -> false
        }

        return ResetPasswordResponse(success = success, message = message)
    }

    // ============================================
    // HELPER FUNCTION - Parse User from JSON
    // ============================================
    private fun parseUserFromJson(userObj: kotlinx.serialization.json.JsonObject?): User? {
        return if (userObj != null) {
            User(
                id = userObj["id"]?.jsonPrimitive?.int ?: 0,
                name = userObj["name"]?.jsonPrimitive?.content ?: "",
                userName = userObj["userName"]?.jsonPrimitive?.content,
                phone = userObj["phone"]?.jsonPrimitive?.content ?: "",
                accountId = userObj["accountId"]?.jsonPrimitive?.content,
                device_id = userObj["device_id"]?.jsonPrimitive?.content,
                referral_code = userObj["referral_code"]?.jsonPrimitive?.content,
                referred_by = userObj["referred_by"]?.jsonPrimitive?.content,
                email = userObj["email"]?.jsonPrimitive?.content,
                photo = userObj["photo"]?.jsonPrimitive?.content,
                address = userObj["address"]?.jsonPrimitive?.content
            )
        } else null
    }
}