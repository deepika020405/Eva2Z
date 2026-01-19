package com.developer.ev2z.util

import android.content.Context
import android.os.Bundle
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.analytics.FirebaseAnalytics

actual object Analytics {

    private lateinit var context: Context

    private lateinit var firebase: FirebaseAnalytics
    private lateinit var metaLogger: AppEventsLogger

    // ✅ Application.onCreate se call hoga
    fun init(appContext: Context) {
        context = appContext.applicationContext
        firebase = FirebaseAnalytics.getInstance(context)
        metaLogger = AppEventsLogger.newLogger(context)
    }

    actual fun logEvent(
        name: String,
        params: Map<String, Any?>
    ) {
        if (!::context.isInitialized) return

        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
            }
        }

        // 🔥 Firebase
        firebase.logEvent(name, bundle)

        // 🔵 Meta (Facebook)
        metaLogger.logEvent(name, bundle)
    }
}
