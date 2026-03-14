package com.developer.ev2z

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.developer.ev2z.data.local.DeepLinkManager
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.phonepe.PaymentBus
import com.developer.ev2z.ui.screens.Profile.GlobalContext
import com.developer.ev2z.util.DeepLinkBus
import com.developer.ev2z.util.MetaAnalytics
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.SosNotificationHandler
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

            // ✅ Step 1: Storage init karo
            initStorageManager(this)

            // ✅ Step 2: DeepLinkManager init karo
            DeepLinkManager.init(createStorageManager())

            // ✅ Step 3: Context set karo
            AppContextHolder.context = applicationContext
            ActivityProvider.setActivity(this)
            initGeocoder(this)
            GlobalContext.set(this)

            // ✅ setContent PEHLE — App() compose start ho
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

            // ✅ Deep link — cold start (same as before)
            intent?.data?.let { uri ->
                handleDeepLink(uri.toString())
            }

            // ✅ SOS — setContent KE BAAD handle karo
            // SharedFlow replay=1 hai — HomeNavGraph ready hone par collect karega
            intent?.let {
                val alertId = it.getIntExtra("sos_alert_id", -1)
                if (alertId != -1) {
                    Log.d("SOS", "🚨 Cold Start alertId=$alertId")
                    SosNotificationHandler.onSosAlertReceived(alertId)
                    // ✅ prevent duplicate trigger
                    it.removeExtra("sos_alert_id")
                }
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Initialization error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityProvider.clearActivity()
    }

    // ✅ App open ho — notification tap
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Deep link (same as before)
        intent.data?.let { uri ->
            handleDeepLink(uri.toString())
        }

        // ✅ SOS — app already open
        val alertId = intent.getIntExtra("sos_alert_id", -1)
        if (alertId != -1) {
            Log.d("SOS", "🚨 App Open alertId=$alertId")
            SosNotificationHandler.onSosAlertReceived(alertId)
        }
    }

    private fun handleDeepLink(uriString: String) {
        val uri = android.net.Uri.parse(uriString)
        Log.d("DeepLink", "🔗 Received URI: $uri")
        Log.d("DeepLink", "   scheme: ${uri.scheme}")
        Log.d("DeepLink", "   host:   ${uri.host}")
        Log.d("DeepLink", "   path:   ${uri.path}")

        when {
            uri.host == "go.eva2z.com" && uri.path?.startsWith("/shop") == true -> {
                val path = uri.path ?: ""
                if (path.startsWith("/shop/product/")) {

                    val value = path.removePrefix("/shop/product/")

                    when (value) {
                        "wired" -> {
                            Log.d("DeepLink", "🔌 Wired products deep link")
                            DeepLinkBus.navigateTo("shop_wired")
                        }
                        "wireless" -> {
                            Log.d("DeepLink", "📡 Wireless products deep link")
                            DeepLinkBus.navigateTo("shop_wireless")
                        }
                        else -> {
                            Log.d("DeepLink", "🛍️ Product deep link: $value")
                            DeepLinkBus.navigateTo("product/$value")
                        }
                    }

                } else {
                    Log.d("DeepLink", "🛒 Shop deep link triggered")
                    DeepLinkBus.navigateTo("shop")
                }
            }

            uri.scheme == "ev2z" && uri.host == "payment" -> {
                Log.d("PhonePe", "🔗 Payment callback: $uri")
                val status = uri.getQueryParameter("status")
                val txnId = uri.getQueryParameter("transactionId")
                    ?: uri.getQueryParameter("providerReferenceId")
                val code = uri.getQueryParameter("code")
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