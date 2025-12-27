package com.developer.ev2z.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

actual object Analytics {

    private lateinit var context: Context

    // ✅ Application se call hoga
    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    private val firebase by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    actual fun logEvent(
        name: String,
        params: Map<String, Any?>
    ) {
        // 🛡️ Safety: analytics kabhi app crash na kare
        if (!::context.isInitialized) {
            return
        }

        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
            }
        }

        firebase.logEvent(name, bundle)
    }
}
