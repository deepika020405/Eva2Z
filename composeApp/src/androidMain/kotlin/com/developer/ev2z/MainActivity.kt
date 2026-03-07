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
import com.developer.ev2z.util.DeepLinkBus
import com.developer.ev2z.util.MetaAnalytics
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.initGeocoder
import com.developer.ev2z.util.initOpenUrl
import com.facebook.appevents.AppEventsConstants

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "🚀 EVA2Z Connect v5.0.0 starting")

        try {
            initOpenUrl(applicationContext)
            NotificationPermissionHelper(this) {
                FcmManager.fetchAndRegisterToken()
            }.checkAndRequest()

            NotificationUtil.createChannel(this)
            initStorageManager(this)
            ActivityProvider.setActivity(this)
            initGeocoder(this)
            GlobalContext.set(this)

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
                mapOf("login_method" to "password")
            )

            // ✅ Handle deep link from cold start (app band tha)
            intent?.data?.let { uri ->
                handleDeepLink(uri.toString())
            }

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
            handleDeepLink(uri.toString())
        }
    }

    // ✅ Single function — sab deep links yahan handle honge
    private fun handleDeepLink(uriString: String) {
        val uri = android.net.Uri.parse(uriString)
        Log.d("DeepLink", "🔗 Received URI: $uri")
        Log.d("DeepLink", "   scheme: ${uri.scheme}")
        Log.d("DeepLink", "   host:   ${uri.host}")
        Log.d("DeepLink", "   path:   ${uri.path}")

        when {

            // ✅ SHOP DEEP LINK — https://ev2z.com/shop
            uri.host == "go.eva2z.com" && uri.path?.startsWith("/shop") == true -> {
                Log.d("DeepLink", "🛒 Shop deep link triggered")
                DeepLinkBus.navigateTo("shop")
            }

            // ✅ PHONEPE CALLBACK — ev2z://payment/callback
            uri.scheme == "ev2z" && uri.host == "payment" -> {
                Log.d("PhonePe", "🔗 Payment callback: $uri")

                val status = uri.getQueryParameter("status")
                val txnId  = uri.getQueryParameter("transactionId")
                    ?: uri.getQueryParameter("providerReferenceId")
                val code   = uri.getQueryParameter("code")

                Log.d("PhonePe", "Status: $status, TxnId: $txnId, Code: $code")

                when (status?.uppercase()) {
                    "SUCCESS" -> PaymentBus.notify(true, txnId, "SUCCESS")
                    "FAILED"  -> PaymentBus.notify(false, txnId, "FAILED")
                    "PENDING" -> PaymentBus.notify(false, txnId, "PENDING")
                    else      -> PaymentBus.notify(false, txnId, code ?: "UNKNOWN")
                }
            }

            else -> {
                Log.d("DeepLink", "⚠️ Unknown deep link: $uri")
            }
        }
    }

    @Composable
    fun AppAndroidPreview() {
        App()
    }
}



