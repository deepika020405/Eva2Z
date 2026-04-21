package com.developer.ev2z

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.ui.screens.SOSParkingTag.IncomingCallScreen
import com.developer.ev2z.util.IncomingCallData
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.SosNotificationHandler

/**
 * Lightweight Activity shown over the lock screen when a call FCM arrives while
 * the app is killed or in background.  Renders IncomingCallScreen directly — no
 * full App() boot needed so it appears in ~200 ms instead of 2-3 s.
 *
 * Flow on Accept:
 *   IncomingCallScreen calls apiService.acceptCall() → onAccepted() fires here
 *   → emit callAcceptedByOwnerFlow → start MainActivity → finish
 *   HomeNavGraph collects callAcceptedByOwnerFlow and goes straight to CallScreen.
 *
 * Flow on Decline / auto-timeout:
 *   IncomingCallScreen calls apiService.declineCall() → onDeclined() fires → finish
 */
class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── Turn screen on / show over lock screen ────────────────────
        // Window flags must be set immediately after super.onCreate so the
        // window is ready before setContent renders anything.
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON      or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON      or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED    or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Belt-and-suspenders: Activity-level API (API 27+)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // ── Init context + storage so ApiService can read auth token ──
        AppContextHolder.context = applicationContext
        initStorageManager(this)

        val visitorName  = intent.getStringExtra("sos_visitor_name")
            ?: intent.getStringExtra("visitor_name") ?: "Visitor"
        val visitorPhone = intent.getStringExtra("sos_visitor_phone")
            ?: intent.getStringExtra("visitor_phone") ?: ""
        val roomId = intent.getStringExtra("call_room_id")
            ?: intent.getStringExtra("room_id") ?: ""
        val chatId = intent.getIntExtra("sos_alert_id", -1).takeIf { it != -1 }
            ?: intent.getStringExtra("chat_id")?.toIntOrNull()

        // Safety net: start ringtone service here in case onMessageReceived was never called
        // (happens when server sends notification+data FCM — Firebase handles it without calling
        // our service, so CallRingtoneService.start() in FCM service was never reached).
        CallRingtoneService.start(applicationContext, visitorName, visitorPhone, roomId, chatId)

        val callData   = IncomingCallData(visitorName, visitorPhone, roomId, chatId)
        val apiService = ApiService()

        setContent {
            IncomingCallScreen(
                visitorName  = visitorName,
                visitorPhone = visitorPhone,
                roomId       = roomId,
                apiService   = apiService,
                onAccepted   = {
                    CallRingtoneService.stop(applicationContext)
                    SosNotificationHandler.onCallAcceptedByOwner(callData)
                    // Boot the main app — HomeNavGraph will read callAcceptedByOwnerFlow
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(mainIntent)
                    finish()
                },
                onDeclined   = {
                    CallRingtoneService.stop(applicationContext)
                    finish()
                }
            )
        }
    }
}