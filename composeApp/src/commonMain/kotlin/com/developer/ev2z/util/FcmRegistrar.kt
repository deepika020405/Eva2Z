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
        fcmToken:          String,
        deviceUuid:        String,
        platform:          String,
        onSuccess:         (() -> Unit)? = null
    ) {
        println("📡 Registering FCM token to server (platform=$platform)...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.registerFcmToken(
                    authHeader = authTokenNullable,
                    fcmToken   = fcmToken,
                    deviceUuid = deviceUuid,
                    platform   = platform
                )
                println("✅ FCM token registered to server successfully")
                onSuccess?.invoke()
            } catch (e: Exception) {
                println("❌ Failed FCM token registration: " + e.message)
            }
        }
    }
}