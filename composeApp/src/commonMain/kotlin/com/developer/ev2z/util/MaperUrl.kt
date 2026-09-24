package com.developer.ev2z.util

// 🔥 MUST be public
fun mapToUserError(e: Exception): String {
    val msg = e.message?.lowercase()?.trim() ?: ""

    return when {

        // ✅ EMAIL ALREADY REGISTERED — must be checked before the phone case below,
        // which would otherwise swallow it and name the wrong field.
        msg.contains("email is already registered") ||
                (msg.contains("email") && msg.contains("already registered")) -> {
            "This email is already registered."
        }

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

        // ✅ OTP NOT VERIFIED (🔥 FIXED)
        msg.contains("otp not verified") -> {
            "OTP not verified"
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
