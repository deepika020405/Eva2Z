package com.developer.ev2z

import android.Manifest
import androidx.annotation.RequiresPermission
import com.developer.ev2z.calling.VoiceCallManager
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.AlertPrefs
import com.developer.ev2z.util.SmsUtil
//import com.developer.ev2z.util.AlarmPlayer  // ── ALARM DISABLED
import com.developer.ev2z.util.SosNotificationHandler
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

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
                println("📞 INCOMING CALL FCM — visitor=$visitorName phone=$visitorPhone roomId=$roomId")
                SosNotificationHandler.onIncomingCallReceived(
                    visitorName  = visitorName,
                    visitorPhone = visitorPhone,
                    roomId       = roomId,
                    chatId       = alertId
                )
                NotificationUtil.showCallNotification(
                    context      = applicationContext,
                    visitorName  = visitorName,
                    visitorPhone = visitorPhone,
                    roomId       = roomId,
                    chatId       = alertId
                )
                return   // skip generic notification below
            }

            // ── Another device (same account) accepted this call ─────
            "call_accepted" -> {
                val roomId = data["room_id"] ?: ""
                println("📱 CALL ACCEPTED ELSEWHERE FCM — room_id=$roomId")
                // Stop ringing on this device
                NotificationUtil.cancelCallNotification(applicationContext)
                // Dismiss the IncomingCallScreen in the UI
                SosNotificationHandler.onCallAcceptedElsewhere()
                return
            }

            // ── Caller cancelled before owner picked up ──────────────
            "call_ended" -> {
                val roomId = data["room_id"] ?: ""
                println("📵 CALL ENDED FCM — room_id=$roomId visitor=$visitorName")
                // 1. Kill the ringing notification immediately (stops sound)
                NotificationUtil.cancelCallNotification(applicationContext)
                // 2. Signal the UI to dismiss IncomingCallScreen / active call
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

        // ✅ Show standard notification for all non-call types
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
                    println("🚨 SOS ALERT — App Open: $alertId (type=$type)")
                    // App open ho tab bhi emit karo — HomeNavGraph sun raha hai
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