package com.developer.ev2z

object FcmBridge {

    private var callback: ((String) -> Unit)? = null

    fun register(cb: (String) -> Unit) {
        callback = cb
    }

    fun onToken(token: String) {
        callback?.invoke(token)
    }
}
