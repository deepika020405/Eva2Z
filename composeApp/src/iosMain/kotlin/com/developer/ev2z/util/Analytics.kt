package com.developer.ev2z.util

//import cocoapods.FirebaseAnalytics.FIRAnalytics
//import cocoapods.FBSDKCoreKit.AppEvents

actual object Analytics {

    actual fun logEvent(
        name: String,
        params: Map<String, Any?>
    ) {
//        FIRAnalytics.logEventWithName(
//            name = name,
//            parameters = params
//        )

        // 🔵 Meta (Facebook)
//        AppEvents.shared.logEvent(
//            name = name,
//            parameters = params
//        )
    }
}
