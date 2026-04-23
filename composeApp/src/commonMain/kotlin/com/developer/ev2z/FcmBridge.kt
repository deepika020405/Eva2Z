package com.developer.ev2z

object FcmBridge {

    private var callback: ((String) -> Unit)? = null
    private var lastToken: String? = null
    var tokenFetcher: (() -> Unit)? = null

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

}


