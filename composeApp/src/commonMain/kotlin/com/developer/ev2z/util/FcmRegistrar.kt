package com.developer.ev2z.util


import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

object FcmRegistrar {

    fun registerFcmToServer(
        authTokenNullable: String,
        fcmToken: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
//                Log.d("FCM", "📡 Sending FCM token to backend")

                println("➡️ Auth Header: $authTokenNullable")
                println("➡️ FCM Token: $fcmToken")

                ApiService().registerFcmToken(
                    authHeader = authTokenNullable,
                    fcmToken = fcmToken
                )
                println("Success FCM token registered successfully")

//                Log.d("FCM", "✅ FCM token registered successfully")

            } catch (e: Exception) {
//                Log.e("FCM", "❌ Failed to register FCM token", e)
                println("Failed FCM token registered successfully" + e.message)
            }
        }
    }
}
