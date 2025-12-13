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
        // Call the updated API
        val jsonResponse = api.login(request.phone, request.password)

        // Parse the JSON response
        val token = jsonResponse["token"]?.jsonPrimitive?.content ?: ""
        val userObj = jsonResponse["user"]?.jsonObject

        val user = parseUserFromJson(userObj) ?: throw Exception("Invalid user data")

        return LoginResponse(user = user, token = token)
    }

    // ============================================
    // SIGN UP
    // ============================================
    suspend fun signUp(name: String, phone: String, password: String, email: String): SignUpResponse {
        val jsonResponse = api.signUp(name, phone, password, email)

        val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        // Parse user if returned
        val userObj = jsonResponse["user"]?.jsonObject
        val user = parseUserFromJson(userObj)

        return SignUpResponse(success = success, message = message, user = user)
    }

    // ============================================
    // VERIFY OTP
    // ============================================
    suspend fun verifyOtp(phone: String, otp: String): OtpResponse {
        val jsonResponse = api.verifyOtp(phone, otp)

        val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""
        val token = jsonResponse["token"]?.jsonPrimitive?.content

        // Parse user if returned
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
    // FORGOT PASSWORD
    // ============================================
    suspend fun forgotPassword(phone: String): ForgotPasswordResponse {
        val jsonResponse = api.forgotPassword(phone)

        val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        return ForgotPasswordResponse(success = success, message = message)
    }

    // ============================================
    // VERIFY FORGOT PASSWORD OTP
    // ============================================
    suspend fun verifyForgotOtp(phone: String, otp: String): OtpResponse {
        val jsonResponse = api.verifyForgotOtp(phone, otp)

        val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

        return OtpResponse(success = success, message = message, token = null, user = null)
    }

    // ============================================
    // RESET PASSWORD
    // ============================================
    suspend fun resetPassword(phone: String, password: String, confirmPassword: String): ResetPasswordResponse {
        val jsonResponse = api.resetPassword(phone, password, confirmPassword)

        val success = jsonResponse["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val message = jsonResponse["message"]?.jsonPrimitive?.content ?: ""

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