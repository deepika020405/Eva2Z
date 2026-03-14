package com.developer.ev2z

import android.Manifest
import androidx.annotation.RequiresPermission
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.AlertPrefs
import com.developer.ev2z.util.SmsUtil
import com.developer.ev2z.util.AlarmPlayer
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

        val data = message.data
        val type = data["type"]

        val title = message.notification?.title ?: data["title"] ?: "Alert"
        val body  = message.notification?.body ?: data["body"] ?: ""

        val alertId = data["notification_id"]?.toIntOrNull()

        println("📩 ANDROID FCM MSG")
        println("Title: $title")
        println("Body: $body")
        println("Data: $data")

        // ✅ Show notification WITH alertId
        NotificationUtil.show(
            context = applicationContext,
            title = title,
            body = body,
            alertId = alertId
        )

        when (type) {

            "vehicle_notification" -> {
                if (alertId != null) {
                    println("🚨 SOS ALERT — App Open: $alertId")
                    // ✅ App open ho tab bhi emit karo — HomeNavGraph sun raha hai
                    SosNotificationHandler.onSosAlertReceived(alertId)
                }
            }


            // 🔔 GEOFENCE BREACH
            "GEOFENCE_BREACH" -> {

                println("🔔 GEOFENCE_BREACH received — deviceId: ${data["device_id"]}")

                val playAlarm = data["play_alarm"]?.equals("1") ?: true
                val sendSms   = data["send_sms"]?.equals("1") ?: false

                println("playAlarm: $playAlarm, alarmEnabled: ${AlertPrefs.alarmEnabled}")
                println("sendSms: $sendSms, smsEnabled: ${AlertPrefs.smsEnabled}")

                if (playAlarm && AlertPrefs.alarmEnabled) {
                    println("✅ Starting alarm")
                    AlarmPlayer.play()
                }

                if (sendSms && AlertPrefs.smsEnabled) {
                    val deviceId = data["device_id"] ?: "Unknown"
                    println("✅ Sending SMS for device: $deviceId")
                    SmsUtil.send(deviceId)
                }
            }

            // 🟢 GEOFENCE ENTER
            "GEOFENCE_ENTER" -> {

                println("✅ GEOFENCE_ENTER — stopping alarm")
                AlarmPlayer.stop()
            }

            else -> {
                println("⚠️ Unknown FCM type: '$type'")
            }
        }
    }
}