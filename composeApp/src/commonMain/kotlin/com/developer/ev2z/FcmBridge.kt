package com.developer.ev2z

object FcmBridge {

    private var callback: ((String) -> Unit)? = null
    private var lastToken: String? = null
    var tokenFetcher: (() -> Unit)? = null

    // Platform hook that deletes this device's FCM token outright. Wired from MainActivity
    // (Android) and AppDelegate (iOS).
    var tokenDeleter: (() -> Unit)? = null

    // Returns the current in-memory FCM token (preferred over reading from storage).
    // Use this for logout API calls so the server always gets the correct token.
    fun currentToken(): String? = lastToken

    fun register(cb: (String) -> Unit) {
        callback = cb
        val token = lastToken
        if (token != null) {
            cb(token)
        } else {
            tokenFetcher?.invoke() // fetch fresh token; onToken() will invoke cb when ready
        }
    }

    fun onToken(token: String) {
        if (token == lastToken) return
        lastToken = token
        callback?.invoke(token)
    }

    // Called on logout — clears in-memory state so next login triggers fresh registration.
    fun clear() {
        callback  = null
        lastToken = null
    }

    /**
     * Deletes the FCM token from this device.
     *
     * This is the only part of logout that does not depend on the server behaving correctly:
     * once the token is gone, FCM rejects every push aimed at it (UNREGISTERED), so a device
     * row the server failed to delete can no longer deliver anything. Firebase issues a fresh
     * token on the next request, which the next login registers.
     */
    fun deleteToken() {
        val deleter = tokenDeleter
        if (deleter == null) {
            println("⚠️ No FCM tokenDeleter wired — token stays live on this device")
            return
        }
        deleter.invoke()
    }

}


