package com.developer.ev2z

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.developer.ev2z.calling.VoiceCallManager
import com.developer.ev2z.util.AlertPrefs
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.SmsUtil
//import com.developer.ev2z.util.AlarmPlayer  // ── ALARM DISABLED
import com.developer.ev2z.util.SosNotificationHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

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
        if (extras != null && extras.getString("type") == "incoming_call") {
            val prefs = applicationContext.getSharedPreferences("eva2z_prefs", MODE_PRIVATE)
            val authToken = prefs.getString("token", "") ?: ""
            if (authToken.isNotBlank() && !VoiceCallManager.isCallActive) {
                val visitorName  = extras.getString("visitor_name")
                val visitorPhone = extras.getString("visitor_phone")
                val roomId       = extras.getString("room_id") ?: ""
                val chatId       = extras.getString("chat_id")?.toIntOrNull()
                println("🔔 handleIntent: incoming_call intercepted — roomId=$roomId")
                // Populate the shared flow so HomeNavGraph shows IncomingCallScreen
                // if/when the app comes to foreground before onMessageReceived fires.
                SosNotificationHandler.onIncomingCallReceived(
                    visitorName  = visitorName,
                    visitorPhone = visitorPhone,
                    roomId       = roomId,
                    chatId       = chatId
                )
                // Post call notification — no background restrictions on NotificationManager.notify().
                // Its fullScreenIntent fires IncomingCallActivity on lock screen / killed state.
                // IncomingCallActivity.onCreate() calls CallRingtoneService.start() from an
                // Activity context, which is always permitted.
                NotificationUtil.showCallNotification(applicationContext, visitorName, visitorPhone, roomId, chatId)
            }
        }
        // Always let Firebase proceed (shows notification banner, may call onMessageReceived)
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
                    SosNotificationHandler.onSosAlertReceived(
                        alertId = alertId,
                        visitorName = visitorName,
                        visitorPhone = visitorPhone
                    )
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