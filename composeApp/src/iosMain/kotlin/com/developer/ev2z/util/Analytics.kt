package com.developer.ev2z.util

import cocoapods.FirebaseAnalytics.FIRAnalytics

actual object Analytics {

    actual fun logEvent(
        name: String,
        params: Map<String, Any?>
    ) {
        FIRAnalytics.logEventWithName(
            name = name,
            parameters = params
        )
    }
}
