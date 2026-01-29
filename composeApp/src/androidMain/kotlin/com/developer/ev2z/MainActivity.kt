package com.developer.ev2z

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.phonepe.PaymentBus
import com.developer.ev2z.ui.screens.Profile.GlobalContext
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.util.MetaAnalytics
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.initGeocoder
import com.facebook.appevents.AppEventsConstants

//import com.developer.ev2z.util.AppSignatureHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "🚀 EVA2Z Connect v5.0.0 starting")

        try {
            NotificationPermissionHelper(this) {
                // permission granted
                FcmManager.fetchAndRegisterToken()
            }.checkAndRequest()

            NotificationUtil.createChannel(this)
            // ✅ Initialize storage
            initStorageManager(this)

            // ✅ Setup activity
            ActivityProvider.setActivity(this)
            initGeocoder(this)

            // ✅ Print signatures for SMS Retriever
//            AppSignatureHelper(this).printAppSignaturesToLog()

            // ✅ Setup global context
            GlobalContext.set(this)




            // ❌ REMOVED: Play Integrity check (was causing Play Protect warnings)
            // PlayIntegrityHelper.verifyIntegrity() - THIS WAS THE PROBLEM!

            // ✅ Set UI content
//            setContent {
//                App()
//            }
            setContent {
                CompositionLocalProvider(
                    LocalPlatformActivity provides this
                ) {
                    App()
                }
            }
            Log.d("MainActivity", "✅ App initialized successfully")
            MetaAnalytics.logEvent(
                AppEventsConstants.EVENT_NAME_ACTIVATED_APP,
                mapOf(
                    "login_method" to "password"
                )
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Initialization error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityProvider.clearActivity()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        intent.data?.let { uri ->
            Log.d("PhonePe", "🔗 Payment callback: $uri")

            val status = uri.getQueryParameter("status")
            val txnId = uri.getQueryParameter("transactionId")
                ?: uri.getQueryParameter("providerReferenceId")
            val code = uri.getQueryParameter("code")

            Log.d("PhonePe", "Status: $status, TxnId: $txnId, Code: $code")

            when (status?.uppercase()) {
                "SUCCESS" -> {
                    Log.d("PhonePe", "✅ Payment SUCCESS")
                    PaymentBus.notify(true, txnId, "SUCCESS")
                }
                "FAILED" -> {
                    Log.d("PhonePe", "❌ Payment FAILED")
                    PaymentBus.notify(false, txnId, "FAILED")
                }
                "PENDING" -> {
                    Log.d("PhonePe", "⏳ Payment PENDING")
                    PaymentBus.notify(false, txnId, "PENDING")
                }
                else -> {
                    Log.d("PhonePe", "❓ Unknown status: $status")
                    PaymentBus.notify(false, txnId, code ?: "UNKNOWN")
                }
            }
        }
    }
}

@Composable
fun AppAndroidPreview() {
    App()
}