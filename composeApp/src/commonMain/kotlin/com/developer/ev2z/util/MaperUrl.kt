package com.developer.ev2z.util

// 🔥 MUST be public
fun mapToUserError(e: Exception): String {
    val msg = e.message?.lowercase()?.trim() ?: ""

    return when {

        // ✅ API BUSINESS ERRORS (HIGH PRIORITY)
        msg.contains("user not registered") ->
            "User not registered."

        msg.contains("invalid credentials") ||
                msg.contains("incorrect password") ->
            "Invalid phone number or password."

        msg.contains("otp") && msg.contains("invalid") ->
            "Invalid OTP. Please try again."

        msg.contains("otp") && msg.contains("expired") ->
            "OTP expired. Please request a new one."

        msg.contains("already registered") ->
            "This phone number is already registered."

        // 🌐 NETWORK ERRORS
        msg.contains("failed to connect") ||
                msg.contains("econnrefused") ||
                msg.contains("isconnected failed") ->
            "Unable to connect. Please check your internet connection."

        msg.contains("timeout") ->
            "Request timed out. Please try again."

        msg.contains("unknownhost") ||
                msg.contains("no route to host") ||
                msg.contains("host") ->
            "No internet connection. Please try again."

        // 🧯 FALLBACK
        else ->
            "Something went wrong. Please try again."
    }
}