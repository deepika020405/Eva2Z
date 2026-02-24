package com.developer.ev2z

import android.app.Application
import android.util.Log
import com.developer.ev2z.ui.screens.social_media.initOpenUrl
import com.developer.ev2z.util.AlertPrefs
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.AppContextProvider
import com.developer.ev2z.util.MetaAnalytics
import com.developer.ev2z.util.initContexts
import com.developer.ev2z.util.loadAlertPrefs
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = this
        loadAlertPrefs() // ✅ add karo — baaki sab unchanged



        try {
            // Meta SDK
//            FacebookSdk.sdkInitialize(this)
//            AppEventsLogger.activateApp(this)

//            FacebookSdk.sdkInitialize(this)
            FacebookSdk.setApplicationId(getString(R.string.facebook_app_id))
            FacebookSdk.sdkInitialize(this)
            FacebookSdk.setAutoInitEnabled(true)
            FacebookSdk.fullyInitialize()

// activateApp REMOVE
            Analytics.init(this)
            MetaAnalytics.init(this)
            initOpenUrl(this)
            initAppContext(this)
            initContexts(this)

            // ✅ Set Activity Provider context
            Log.d("MainApplication", "✅ Application initialized")
            Log.d("MainApplication", "✅ Meta SDK initialized")

            // ✅ You can add more initialization here if needed

        } catch (e: Exception) {
            Log.e("MainApplication", "❌ Application init error: ${e.message}", e)
        }
    }
}

//package com.developer.ev2z
//
//import android.app.Application
//import com.developer.ev2z.upi.appContext
//
//class MainApplication : Application() {
//    override fun onCreate() {
//        super.onCreate()
//
//        // ✅ App context initialize
//        initAppContext(this)
//        appContext = this
//
//        // ✅ PhonePe SDK initialize
////        initPhonePe()
//    }
//
////    private fun initPhonePe() {
////        try {
////            // ✅ Use the 4-parameter init method
////            PhonePe.init(
////                this,                           // Context
////                PhonePeEnvironment.RELEASE,     // Environment (RELEASE for production)
////                "M22VRDGEOVBYAUAT",             // Merchant ID
////                ""                              // App ID (empty string if not required)
////            )
////            Log.d("PhonePe", "✅ SDK initialized successfully in Application")
////        } catch (e: PhonePeInitException) {
////            Log.e("PhonePe", "❌ SDK init failed: ${e.message}", e)
////        } catch (e: Exception) {
////            Log.e("PhonePe", "❌ SDK init error: ${e.message}", e)
////        }
////    }
//}