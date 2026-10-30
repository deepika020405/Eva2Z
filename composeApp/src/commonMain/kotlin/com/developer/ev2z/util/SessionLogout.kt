package com.developer.ev2z.util

import com.developer.ev2z.FcmBridge
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.network.RateLimitGate
import com.developer.ev2z.network.SharedFleetLocations
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

        // A blank token here means this session was already torn down. The profile screen
        // logs out and then reports logoutSuccess, which fires App's onLogout — so every
        // manual logout reaches this function twice. Repeating the work just doubles the
        // network calls and logs a misleading "no auth token" for the second pass.
        if (token.isBlank()) {
            println("⚠️ Logout: session already cleared — skipping duplicate")
            clearLocal(storage)
            return
        }

        // In-memory token is the freshest; fall back to the persisted copy when the app
        // was restarted and FCM hasn't handed us a token again this session.
        val fcmToken = FcmBridge.currentToken()?.takeIf { it.isNotBlank() }
            ?: storage.getString("last_fcm_token", "")
        val deviceUuid = userPrefs.getOrCreateDeviceUuid()
        val platform = platformName()

        // Bump the session generation FIRST. An FCM registration already in flight checks this
        // when it lands and undoes itself instead of putting this phone back on the push list —
        // that race is what re-registered the device milliseconds after a 401 logout.
        SessionState.invalidate()

        clearLocal(storage)

        // Kill the token on the device itself. Everything below depends on the server doing the
        // right thing; this step does not. Without it, any device row the server keeps — from a
        // failed logout, an older install, or a session that expired offline — keeps delivering.
        FcmBridge.deleteToken()

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

            // Always deregister as well, whatever /logout did. On the session-expiry path the
            // token above is already rejected, so /logout deletes nothing and this is the only
            // call that can remove the row.
            if (fcmToken.isNotBlank() || deviceUuid.isNotBlank()) {
                try {
                    apiService.deregisterFcmToken(
                        fcmToken   = fcmToken,
                        deviceUuid = deviceUuid,
                        platform   = platform,
                        authHeader = "Bearer $token"
                    )
                } catch (e: Exception) {
                    println("⚠️ FCM deregister failed: ${e.message}")
                }
            }
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
        // The fleet answer is shared between screens and keyed on the token, so the next
        // account could not be served this one's vehicles — but it is held for a few
        // seconds after a logout and there is no reason to keep it.
        SharedFleetLocations.clear()
        // A throttle hold belongs to the session that earned it; whoever signs in next
        // should start polling straight away.
        RateLimitGate.open()
        // Both notifiers' memories go with the session, so the next account is baselined
        // rather than told what the previous one's vehicles were doing.
        VehicleStatusAlerts.reset()
        IgnitionAlerts.reset()
    }
}