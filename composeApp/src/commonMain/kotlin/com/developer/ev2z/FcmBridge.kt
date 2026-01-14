package com.developer.ev2z

object FcmBridge {

    private var callback: ((String) -> Unit)? = null
    private var lastToken: String? = null
    fun register(cb: (String) -> Unit) {
        callback = cb
        lastToken?.let { cb(it) } // 👈 replay last token
    }

    fun onToken(token: String) {
        lastToken = token
        callback?.invoke(token)
    }


    fun clear() {
        callback = null
    }

}
