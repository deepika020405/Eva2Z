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

/**
 * Cleans up a `message` the server sent in a failed response before it is shown.
 *
 * Most of them are written for the user and pass straight through. Some are not:
 * `save-password` answers a duplicate e-mail with the raw MySQL error — SQLSTATE,
 * the whole INSERT, and the bcrypt hash inside it (seen 2026-09-24,
 * `1062 Duplicate entry '…' for key 'users.users_email_unique'`). That must never
 * reach a user: it is unreadable, and it prints internals on screen.
 *
 * The duplicate-key cases are recognised and answered with what the user has to do
 * about it; anything else that looks like a server internal becomes [fallback].
 */
fun mapServerMessage(
    raw: String?,
    fallback: String = "Something went wrong. Please try again."
): String {
    val message = raw?.trim().orEmpty()
    if (message.isEmpty()) return fallback

    val lower = message.lowercase()

    // A unique-key violation means the account is already there — which field it is
    // decides what the user should do next.
    val duplicate = lower.contains("duplicate entry") ||
        lower.contains("1062") ||
        lower.contains("unique constraint")

    if (duplicate) {
        // MySQL names the constraint it tripped on ("for key 'users.users_email_unique'").
        // That is the only part that says which field clashed — the rest of the message
        // lists every column in the INSERT, phone and email alike.
        val key = Regex("for key '([^']+)'").find(lower)?.groupValues?.get(1)
        val field = key ?: lower

        return when {
            field.contains("email") ->
                "This email is already registered with another account. " +
                    "Go back and use a different email, or log in with that account."

            field.contains("phone") ->
                "This phone number is already registered. Please log in instead."

            else -> "This account already exists. Please log in instead."
        }
    }

    // Anything carrying the shape of a server-side failure rather than a sentence.
    val internal = listOf(
        "sqlstate", "sql:", "insert into", "update `", "select ", "connection: mysql",
        "exception", "stack trace", "/var/www", "vendor\\", "syntax error",
    ).any { lower.contains(it) }

    return if (internal || message.length > 200) fallback else message
}
