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
        fcmToken: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.registerFcmToken(
                    authHeader = authTokenNullable,
                    fcmToken = fcmToken
                )
            } catch (e: Exception) {
                println("Failed FCM token registration: " + e.message)
            }
        }
    }
}
