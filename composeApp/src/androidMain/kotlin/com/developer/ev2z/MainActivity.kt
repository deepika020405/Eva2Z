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
import com.developer.ev2z.calling.VoiceCallManager
import com.developer.ev2z.util.initGeocoder
import com.developer.ev2z.util.initOpenUrl
import com.facebook.appevents.AppEventsConstants

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "🚀 EVA2Z Connect v5.0.0 starting")

        try {
            // If MainActivity is launched from IncomingCallActivity after the user accepted
            // a call on the lock screen, keep it visible over the lock screen so the active
            // call screen is not blocked by a PIN prompt mid-call. This does NOT dismiss the
            // keyguard — the lock screen returns normally on the next screen timeout/power press.
            if (intent?.getBooleanExtra("is_accepted_call", false) == true) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }

            initOpenUrl(applicationContext)

            FcmBridge.tokenFetcher = { FcmManager.fetchAndRegisterToken() }
            NotificationPermissionHelper(this) {
                FcmManager.fetchAndRegisterToken()
            }.checkAndRequest()

            NotificationUtil.createChannel(this)
            NotificationUtil.createCallChannel(this)
            NotificationUtil.createCallServiceChannel(this)
            NotificationUtil.requestFullScreenIntentPermissionIfNeeded(this)

            // ✅ Step 1: Storage init karo
            initStorageManager(this)

            // ✅ Step 2: DeepLinkManager init — reuse same storage instance
            val storage = createStorageManager()
            DeepLinkManager.init(storage)

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

            // ✅ SOS / Incoming-call — setContent KE BAAD handle karo
            // SharedFlow replay=1 hai — HomeNavGraph ready hone par collect karega
            intent?.let { handleSosIntent(it) }

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Initialization error", e)
        }
    }

    override fun onStart() {
        super.onStart()
        AppState.isInForeground = true
    }

    override fun onStop() {
        super.onStop()
        AppState.isInForeground = false
        // Clear show-when-locked whenever the app goes to background and no call is active.
        // If left set, future notification taps would bypass the lock screen directly.
        // During an active call we keep it so the call screen remains reachable from
        // the lock screen without interrupting the call mid-conversation.
        if (!VoiceCallManager.isCallActive) {
            setShowWhenLocked(false)
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
        intent.data?.let { uri -> handleDeepLink(uri.toString()) }
        handleSosIntent(intent)
    }

    // Handles both our custom PendingIntent extras (sos_alert_id / sos_visitor_name) AND
    // Firebase auto-notification extras (chat_id / visitor_name) so tap-to-open works
    // regardless of whether the server sends a notification+data FCM or a data-only FCM.
    private fun handleSosIntent(intent: Intent) {
        // Firebase auto-notification tap passes FCM data extras as-is.
        // "type=incoming_call" comes from the FCM data payload when Firebase handles the
        // notification automatically (background/killed) and user taps the banner.
        val fcmType = intent.getStringExtra("type")
        val isCall = intent.getBooleanExtra("is_incoming_call", false)
                  || fcmType == "incoming_call"
        if (isCall) {
            val visitorName  = intent.getStringExtra("sos_visitor_name")
                ?: intent.getStringExtra("visitor_name")
            val visitorPhone = intent.getStringExtra("sos_visitor_phone")
                ?: intent.getStringExtra("visitor_phone")
            val roomId = intent.getStringExtra("call_room_id")
                ?: intent.getStringExtra("room_id") ?: ""
            val chatId = intent.getIntExtra("sos_alert_id", -1).takeIf { it != -1 }
                ?: intent.getStringExtra("chat_id")?.toIntOrNull()
            Log.d("SOS", "📞 Incoming call intent visitor=$visitorName roomId=$roomId")
            // Dismiss IncomingCallActivity if it is alive (shown over lock screen or as a
            // heads-up popup). Without this, its 30-second auto-decline timer would fire and
            // call declineCall() on a call that MainActivity's HomeNavGraph has already accepted.
            SosNotificationHandler.onMainActivityTookOver()
            // Start ringtone service — needed when user taps a Firebase auto-notification
            // (notification+data FCM handled by Firebase bypasses onMessageReceived entirely).
            CallRingtoneService.start(this, visitorName, visitorPhone, roomId, chatId)
            SosNotificationHandler.onIncomingCallReceived(
                visitorName  = visitorName,
                visitorPhone = visitorPhone,
                roomId       = roomId,
                chatId       = chatId
            )
            intent.removeExtra("is_incoming_call"); intent.removeExtra("type")
            intent.removeExtra("sos_visitor_name"); intent.removeExtra("visitor_name")
            intent.removeExtra("sos_visitor_phone"); intent.removeExtra("visitor_phone")
            intent.removeExtra("call_room_id"); intent.removeExtra("room_id")
            intent.removeExtra("sos_alert_id"); intent.removeExtra("chat_id")
        } else {
            // Our custom PendingIntent uses "sos_alert_id".
            // Firebase auto-notification (background/killed) passes the FCM data as-is → "chat_id".
            val alertId = intent.getIntExtra("sos_alert_id", -1).takeIf { it != -1 }
                ?: intent.getStringExtra("chat_id")?.toIntOrNull()
                ?: intent.getStringExtra("notification_id")?.toIntOrNull()
            if (alertId != null && alertId > 0) {
                val visitorName  = intent.getStringExtra("sos_visitor_name")
                    ?: intent.getStringExtra("visitor_name")
                val visitorPhone = intent.getStringExtra("sos_visitor_phone")
                    ?: intent.getStringExtra("visitor_phone")
                Log.d("SOS", "🚨 SOS intent alertId=$alertId visitor=$visitorName")
                SosNotificationHandler.onSosAlertReceived(
                    alertId = alertId,
                    visitorName = visitorName,
                    visitorPhone = visitorPhone
                )
                intent.removeExtra("sos_alert_id"); intent.removeExtra("chat_id")
                intent.removeExtra("notification_id")
                intent.removeExtra("sos_visitor_name"); intent.removeExtra("visitor_name")
                intent.removeExtra("sos_visitor_phone"); intent.removeExtra("visitor_phone")
            }
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

                // ✅ ADD — Product detail deeplinks
            uri.host == "go.eva2z.com" && uri.path?.startsWith("/product") == true -> {
                val value = uri.path?.removePrefix("/product/") ?: ""
                Log.d("DeepLink", "📦 Product detail deep link: $value")
                when (value) {
                    "wireless" -> DeepLinkBus.navigateTo("product_detail_10")
                    "wired"    -> DeepLinkBus.navigateTo("product_detail_11")
                    "sos-tag"  -> DeepLinkBus.navigateTo("product_detail_26")
                    else       -> DeepLinkBus.navigateTo("shop")
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