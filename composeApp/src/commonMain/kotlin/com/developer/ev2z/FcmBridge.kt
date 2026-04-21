package com.developer.ev2z

object FcmBridge {

    private var callback: ((String) -> Unit)? = null
    private var lastToken: String? = null
    var tokenFetcher: (() -> Unit)? = null

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

    fun clear() {
        callback = null
        lastToken = null
    }

}


