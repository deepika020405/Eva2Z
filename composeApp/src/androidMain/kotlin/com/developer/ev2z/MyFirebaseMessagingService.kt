package com.developer.ev2z

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.developer.ev2z.calling.VoiceCallManager
import com.developer.ev2z.util.AlertPrefs
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.SmsUtil
//import com.developer.ev2z.util.AlarmPlayer  // ── ALARM DISABLED
import com.developer.ev2z.util.SosNotificationHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private companion object {
        // Intent action Firebase uses for delivered messages. Token refresh arrives as
        // com.google.firebase.messaging.NEW_TOKEN, so gating on this alone is safe.
        const val INTENT_ACTION_FCM_RECEIVE = "com.google.android.c2dm.intent.RECEIVE"
    }

    // Fire-and-forget POST to mark message as delivered (arrived on device, not yet read).
    private fun callDeliveredApi(chatId: Int, token: String) {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://app.eva2z.in/api/chat/$chatId/delivered")
                    .post("".toRequestBody())
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Accept", "application/json")
                    .build()
                client.newCall(request).execute().close()
                println("✔️ markDelivered #$chatId (FCM)")
            } catch (e: Exception) {
                println("❌ markDelivered #$chatId (FCM): ${e.message}")
            }
        }
    }

    // Called for EVERY FCM message — even notification+data in background/killed state,
    // where Firebase skips onMessageReceived entirely.
    // We post the call notification here so the ringtone / full-screen UI fires
    // regardless of FCM payload type or app state.
    // NOTE: startForegroundService() is NOT called here — Android 12+ forbids it
    // from handleIntent when the app is killed/background (mAllowStartForeground false).
    // Instead we post a notification whose fullScreenIntent fires IncomingCallActivity,
    // which then starts CallRingtoneService from an Activity context (always allowed).
    @SuppressLint("MissingPermission")
    override fun handleIntent(intent: Intent) {
        val extras = intent.extras
        val prefs = applicationContext.getSharedPreferences("eva2z_prefs", MODE_PRIVATE)
        val authToken = prefs.getString("token", "") ?: ""
        val type = extras?.getString("type")

        // No session on this device → drop the message entirely instead of letting
        // Firebase display it. onMessageReceived() already guards data-only pushes, but
        // notification-payload pushes in background/killed state never reach it — they
        // are rendered by Firebase inside super.handleIntent(). Returning here is the
        // only app-side way to stop those. Token-refresh intents use a different action
        // and still pass through.
        if (authToken.isBlank() && intent.action == INTENT_ACTION_FCM_RECEIVE) {
            println("⚠️ FCM message dropped — user logged out on this device")
            return
        }

        when {
            // ── Visitor declined / caller cancelled — dismiss owner's CallScreen ──
            type == "call_ended" && authToken.isNotBlank() -> {
                val roomId      = extras?.getString("room_id") ?: ""
                val visitorName = extras?.getString("visitor_name")
                val visitorPhone= extras?.getString("visitor_phone")
                val chatId      = extras?.getString("chat_id")?.toIntOrNull()
                println("📵 handleIntent: call_ended intercepted — roomId=$roomId")
                CallRingtoneService.stop(applicationContext)
                SosNotificationHandler.onCallEnded()
                Handler(Looper.getMainLooper()).post {
                    NotificationUtil.showMissedCallNotification(
                        context      = applicationContext,
                        visitorName  = visitorName,
                        visitorPhone = visitorPhone,
                        chatId       = chatId
                    )
                }
            }

            // ── Incoming call ──────────────────────────────────────────────
            type == "incoming_call" && authToken.isNotBlank() && !VoiceCallManager.isCallActive -> {
                val visitorName  = extras.getString("visitor_name")
                val visitorPhone = extras.getString("visitor_phone")
                val roomId       = extras.getString("room_id") ?: ""
                val chatId       = extras.getString("chat_id")?.toIntOrNull()
                println("🔔 handleIntent: incoming_call intercepted — roomId=$roomId")
                SosNotificationHandler.onIncomingCallReceived(
                    visitorName  = visitorName,
                    visitorPhone = visitorPhone,
                    roomId       = roomId,
                    chatId       = chatId
                )
                NotificationUtil.showCallNotification(applicationContext, visitorName, visitorPhone, roomId, chatId)

                // Cancel Firebase's own duplicate notification (notification+data payload)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                                as NotificationManager
                        nm.activeNotifications
                            .filter { it.tag?.startsWith("FCM-Notification:") == true }
                            .forEach { nm.cancel(it.tag, it.id) }
                    }
                }, 400L)
            }

            // ── New visitor chat message ───────────────────────────────────
            // handleIntent runs even in background/killed state where a notification+data
            // FCM bypasses onMessageReceived entirely. Posting our own notification here
            // ensures the owner is always alerted regardless of app state.
            (type == "chat" || type == "vehicle_notification") && authToken.isNotBlank() -> {
                val chatId       = extras.getString("chat_id")?.toIntOrNull()
                    ?: extras.getString("notification_id")?.toIntOrNull()
                val visitorName  = extras.getString("visitor_name")
                val visitorPhone = extras.getString("visitor_phone")
                val title        = extras.getString("gcm.notification.title")
                    ?: extras.getString("title") ?: "New Message"
                val body         = extras.getString("gcm.notification.body")
                    ?: extras.getString("body") ?: "You have a new visitor message"
                println("🔔 handleIntent: chat message intercepted — chatId=$chatId visitor=$visitorName")

                NotificationUtil.show(
                    context      = applicationContext,
                    title        = title,
                    body         = body,
                    alertId      = chatId,
                    visitorName  = visitorName,
                    visitorPhone = visitorPhone
                )

                if (chatId != null) {
                    // Only emit in-app navigation when the app is actively in the foreground.
                    // If the screen is locked or the app is backgrounded, the user must
                    // authenticate via the notification tap first — handleSosIntent() in
                    // MainActivity will emit then, at the correct moment.
                    if (AppState.isInForeground) {
                        SosNotificationHandler.onSosAlertReceived(
                            alertId      = chatId,
                            visitorName  = visitorName,
                            visitorPhone = visitorPhone
                        )
                    }
                    callDeliveredApi(chatId, authToken)
                }

                // Cancel Firebase's duplicate auto-notification so only ours shows
                Handler(Looper.getMainLooper()).postDelayed({
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                                as NotificationManager
                        nm.activeNotifications
                            .filter { it.tag?.startsWith("FCM-Notification:") == true }
                            .forEach { nm.cancel(it.tag, it.id) }
                    }
                }, 400L)
            }
        }

        // Always let Firebase proceed (needed for kill/background state token refresh etc.)
        super.handleIntent(intent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("🔥 ANDROID FCM TOKEN: $token")

        FcmBridge.onToken(token)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // If user is logged out on this device, ignore all notifications
        val prefs = applicationContext.getSharedPreferences("eva2z_prefs", MODE_PRIVATE)
        val authToken = prefs.getString("token", "") ?: ""
        if (authToken.isBlank()) {
            println("⚠️ FCM ignored — user logged out on this device")
            return
        }

        val data = message.data
        val type = data["type"]

        val title = message.notification?.title ?: data["title"] ?: "Alert"
        val body  = message.notification?.body ?: data["body"] ?: ""

        // Server now sends `chat_id` for visitor chat messages; older payloads used `notification_id`.
        val alertId = data["chat_id"]?.toIntOrNull()
            ?: data["notification_id"]?.toIntOrNull()

        println("📩 ANDROID FCM MSG")
        println("Title: $title")
        println("Body: $body")
        println("Data: $data")
        // If message.notification != null the server is sending notification+data FCM.
        // Firebase will auto-handle it in background/killed WITHOUT calling onMessageReceived.
        // Server MUST send data-only FCM (no notification field) for incoming_call to work reliably.
        if (message.notification != null) {
            println("⚠️ WARNING: FCM has 'notification' field — background/killed delivery bypasses onMessageReceived!")
            println("⚠️ FIX REQUIRED: Server must send data-only FCM for type=incoming_call (remove notification field)")
        }

        val visitorName  = data["visitor_name"]
        val visitorPhone = data["visitor_phone"]

        when (type) {

            // ── Incoming call from a parking visitor ─────────────────
            "incoming_call" -> {
                val roomId = data["room_id"] ?: ""
                // Drop this FCM if a call is already active — duplicate events from
                // the server can arrive after the user has already accepted.
                if (VoiceCallManager.isCallActive) {
                    println("⚠️ INCOMING CALL FCM ignored — call already active (roomId=$roomId)")
                    return
                }
                println("📞 INCOMING CALL FCM — visitor=$visitorName phone=$visitorPhone roomId=$roomId foreground=${AppState.isInForeground}")

                // Always emit to callFlow — HomeNavGraph shows IncomingCallScreen when foreground.
                SosNotificationHandler.onIncomingCallReceived(
                    visitorName  = visitorName,
                    visitorPhone = visitorPhone,
                    roomId       = roomId,
                    chatId       = alertId
                )

                // Start foreground ringtone service — plays looping ringtone and shows
                // call notification with full-screen intent in ALL states:
                // foreground, background, killed, locked screen.
                // Wrapped in try-catch: on Android 12+ the high-priority FCM exemption
                // can expire between handleIntent and onMessageReceived if the process
                // was slow to start. Fall back to showCallNotification() in that case —
                // IncomingCallActivity will start CallRingtoneService from its context.
                try {
                    CallRingtoneService.start(
                        context      = applicationContext,
                        visitorName  = visitorName,
                        visitorPhone = visitorPhone,
                        roomId       = roomId,
                        chatId       = alertId
                    )
                } catch (e: Exception) {
                    println("⚠️ CallRingtoneService.start() blocked — falling back to showCallNotification: ${e.message}")
                    NotificationUtil.showCallNotification(applicationContext, visitorName, visitorPhone, roomId, alertId)
                }
                return   // skip generic notification below
            }

            // ── Another device (same account) accepted this call ─────
            "call_accepted" -> {
                val roomId = data["room_id"] ?: ""
                println("📱 CALL ACCEPTED ELSEWHERE FCM — room_id=$roomId")
                CallRingtoneService.stop(applicationContext)
                SosNotificationHandler.onCallAcceptedElsewhere()
                return
            }

            // ── Caller cancelled before owner picked up ──────────────
            "call_ended" -> {
                val roomId = data["room_id"] ?: ""
                println("📵 CALL ENDED FCM — room_id=$roomId visitor=$visitorName")
                CallRingtoneService.stop(applicationContext)
                SosNotificationHandler.onCallEnded()
                // 3. Show a silent missed-call notification
                NotificationUtil.showMissedCallNotification(
                    context      = applicationContext,
                    visitorName  = visitorName,
                    visitorPhone = visitorPhone,
                    chatId       = alertId
                )
                return   // skip generic notification
            }

            else -> { /* handled after generic notification */ }
        }

        // Always show system notification — foreground or background.
        // alertFlow handles in-app redirect; notification gives user a fallback tap target.
        NotificationUtil.show(
            context = applicationContext,
            title = title,
            body = body,
            alertId = alertId,
            visitorName = visitorName,
            visitorPhone = visitorPhone
        )

        when (type) {

            "chat", "vehicle_notification" -> {
                if (alertId != null) {
                    println("🚨 SOS ALERT — type=$type alertId=$alertId foreground=${AppState.isInForeground}")
                    if (AppState.isInForeground) {
                        SosNotificationHandler.onSosAlertReceived(
                            alertId = alertId,
                            visitorName = visitorName,
                            visitorPhone = visitorPhone
                        )
                    }
                    callDeliveredApi(alertId, authToken)
                }
            }


            // 🔔 GEOFENCE BREACH
            "GEOFENCE_BREACH" -> {

                println("🔔 GEOFENCE_BREACH received — deviceId: ${data["device_id"]}")

                val playAlarm = data["play_alarm"]?.equals("1") ?: true
                val sendSms   = data["send_sms"]?.equals("1") ?: false

//                println("playAlarm: $playAlarm, alarmEnabled: ${AlertPrefs.alarmEnabled}")
//                println("sendSms: $sendSms, smsEnabled: ${AlertPrefs.smsEnabled}")
//
//                if (playAlarm && AlertPrefs.alarmEnabled) {
//                    println("✅ Starting alarm")
////                    AlarmPlayer.play()
//                }

                if (sendSms && AlertPrefs.smsEnabled) {
                    val deviceId = data["device_id"] ?: "Unknown"
                    println("✅ Sending SMS for device: $deviceId")
                    SmsUtil.send(deviceId)
                }
            }

            // 🟢 GEOFENCE ENTER
            "GEOFENCE_ENTER" -> {

                println("✅ GEOFENCE_ENTER — stopping alarm")
//                AlarmPlayer.stop()
            }

            else -> {
                println("⚠️ Unknown FCM type: '$type'")
            }
        }
    }
}