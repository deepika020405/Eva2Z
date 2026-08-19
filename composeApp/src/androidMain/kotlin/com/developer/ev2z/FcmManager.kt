package com.developer.ev2z

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

object FcmManager {

    fun fetchAndRegisterToken() {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d("FCM", "🔥 Token: $token")

                // 🔁 Send to KMP common
                FcmBridge.onToken(token)
            }
            .addOnFailureListener {
                Log.e("FCM", "❌ Token fetch failed", it)
            }
    }

    // Invalidates this device's token so pushes aimed at it are rejected by FCM itself.
    // Firebase mints a new one on the next token request, which the next login registers.
    fun deleteToken() {
        FirebaseMessaging.getInstance().deleteToken()
            .addOnSuccessListener {
                Log.d("FCM", "🗑️ Token deleted — device no longer reachable by push")
            }
            .addOnFailureListener {
                Log.e("FCM", "❌ Token delete failed", it)
            }
    }
}
