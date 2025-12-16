package com.developer.ev2z

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // ✅ App context initialize
        initAppContext(this)

        // ✅ PhonePe SDK initialize
//        initPhonePe()
    }

//    private fun initPhonePe() {
//        try {
//            // ✅ Use the 4-parameter init method
//            PhonePe.init(
//                this,                           // Context
//                PhonePeEnvironment.RELEASE,     // Environment (RELEASE for production)
//                "M22VRDGEOVBYAUAT",             // Merchant ID
//                ""                              // App ID (empty string if not required)
//            )
//            Log.d("PhonePe", "✅ SDK initialized successfully in Application")
//        } catch (e: PhonePeInitException) {
//            Log.e("PhonePe", "❌ SDK init failed: ${e.message}", e)
//        } catch (e: Exception) {
//            Log.e("PhonePe", "❌ SDK init error: ${e.message}", e)
//        }
//    }
}