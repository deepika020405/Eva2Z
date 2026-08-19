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
        // Snapshot the session this registration belongs to. The call below is detached and
        // cannot be cancelled, so a logout while it is in flight can only be handled by
        // checking on the way back — otherwise the response re-arms a logged-out device.
        val generation = SessionState.generation

        println("📡 Registering FCM token to server (platform=$platform)...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                apiService.registerFcmToken(
                    authHeader = authTokenNullable,
                    fcmToken   = fcmToken,
                    deviceUuid = deviceUuid,
                    platform   = platform
                )

                if (generation != SessionState.generation) {
                    // The user logged out between the request going out and the response
                    // coming back. The server took the registration anyway (fcm/register does
                    // not require a live session), so undo it explicitly.
                    println("↩️ FCM registration landed after logout — deregistering")
                    try {
                        apiService.deregisterFcmToken(
                            fcmToken   = fcmToken,
                            deviceUuid = deviceUuid,
                            platform   = platform
                        )
                    } catch (e: Exception) {
                        println("❌ Failed to undo late FCM registration: " + e.message)
                    }
                    return@launch
                }

                println("✅ FCM token registered to server successfully")
                onSuccess?.invoke()
            } catch (e: Exception) {
                println("❌ Failed FCM token registration: " + e.message)
            }
        }
    }
}