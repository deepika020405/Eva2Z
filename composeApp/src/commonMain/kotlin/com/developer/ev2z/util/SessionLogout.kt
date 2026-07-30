package com.developer.ev2z.util

import com.developer.ev2z.FcmBridge
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.platformName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

/**
 * Single entry point for ending a session.
 *
 * Every logout path in the app must go through here — the profile button, the account
 * activity screen, a 401 session expiry and account deletion. Any path that only wipes
 * local storage leaves this device's FCM token registered on the server, which is why
 * status notifications kept arriving after logout.
 */
object SessionLogout {

    /**
     * Deregisters this device server-side and wipes the session locally.
     *
     * Local state is cleared immediately so the UI can drop to the login screen without
     * waiting on the network; the deregister call runs in the background with the
     * credentials captured beforehand.
     */
    fun logoutAndClear(apiService: ApiService, storage: StorageManager) {
        val userPrefs = UserPrefs(storage)

        // Capture everything the server needs BEFORE clearing local storage.
        val token = storage.getString("token", "")
        // In-memory token is the freshest; fall back to the persisted copy when the app
        // was restarted and FCM hasn't handed us a token again this session.
        val fcmToken = FcmBridge.currentToken()?.takeIf { it.isNotBlank() }
            ?: storage.getString("last_fcm_token", "")
        val deviceUuid = userPrefs.getOrCreateDeviceUuid()
        val platform = platformName()

        clearLocal(storage)

        // Fire even when the FCM token is unknown — device_uuid alone is enough for the
        // server to find and delete this device's row.
        if (token.isNotBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    apiService.logout(
                        authHeader = "Bearer $token",
                        fcmToken   = fcmToken,
                        deviceUuid = deviceUuid,
                        platform   = platform
                    )
                    println("✅ Logout API called successfully (device deregistered)")
                } catch (e: Exception) {
                    println("⚠️ Logout API failed (local logout already done): ${e.message}")
                }
            }
        } else {
            println("⚠️ Logout: no auth token — server-side deregistration skipped")
        }
    }

    /**
     * Wipes the session locally while keeping `device_uuid`.
     *
     * The UUID identifies the *phone*, not the session. Rotating it on every logout made
     * each login insert a fresh device row carrying the same FCM token, so the old rows
     * survived and kept receiving pushes. Keeping it stable means the next login updates
     * the same row instead of orphaning one.
     */
    fun clearLocal(storage: StorageManager) {
        val deviceUuid = storage.getString("device_uuid", "")
        storage.clear()
        if (deviceUuid.isNotBlank()) {
            storage.saveString("device_uuid", deviceUuid)
        }
        FcmBridge.clear()
    }
}