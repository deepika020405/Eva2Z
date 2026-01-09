package com.developer.ev2z.util

// 🔥 MUST be public
fun mapToUserError(e: Exception): String {
    val msg = e.message?.lowercase()?.trim() ?: ""

    return when {

        // ✅ PHONE ALREADY REGISTERED (🔥 VERY IMPORTANT)
        msg.contains("phone is already registered") ||
                msg.contains("already registered") -> {
            "This phone number is already registered."
        }

        // ✅ INVALID LOGIN
        msg.contains("incorrect password") ||
                msg.contains("password is incorrect") ||   // 👈 ADD THIS
                msg.contains("invalid password") ||
                msg.contains("wrong password") ||
                msg.contains("invalid credentials") -> {
            "Invalid phone number or password."
        }

        // ✅ USER NOT FOUND
        msg.contains("user not registered") ||
                msg.contains("not registered") -> {
            "User not registered."
        }

        // ✅ OTP ERRORS
        // ✅ OTP ERRORS (ROBUST)
        msg.contains("otp") && msg.contains("invalid") -> {
            "Invalid OTP. Please try again."
        }

        msg.contains("otp") && msg.contains("expired") -> {
            "OTP expired. Please request a new one."
        }


        // 🌐 NETWORK
        msg.contains("failed to connect") ||
                msg.contains("econnrefused") ||
                msg.contains("isconnected failed") -> {
            "Unable to connect. Please check your internet connection."
        }

        msg.contains("timeout") -> {
            "Request timed out. Please try again."
        }

        msg.contains("unknownhost") ||
                msg.contains("no route to host") -> {
            "No internet connection. Please try again."
        }

        // 🧯 FALLBACK
        else -> {
            "Something went wrong. Please try again."
        }
    }
}
