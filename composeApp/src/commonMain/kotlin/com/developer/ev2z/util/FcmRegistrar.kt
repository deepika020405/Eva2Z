package com.developer.ev2z.util


import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

object FcmRegistrar {

    private val apiService by lazy { ApiService() }

    fun registerFcmToServer(
        authTokenNullable: String,
        fcmToken: String,
        deviceUuid: String
    ) {
        println("📡 Registering FCM token to server...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.registerFcmToken(
                    authHeader = authTokenNullable,
                    fcmToken = fcmToken,
                    deviceUuid = deviceUuid
                )
                println("✅ FCM token registered to server successfully")
            } catch (e: Exception) {
                println("❌ Failed FCM token registration: " + e.message)
            }
        }
    }
}
