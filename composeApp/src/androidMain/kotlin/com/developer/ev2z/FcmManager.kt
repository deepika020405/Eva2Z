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
}
