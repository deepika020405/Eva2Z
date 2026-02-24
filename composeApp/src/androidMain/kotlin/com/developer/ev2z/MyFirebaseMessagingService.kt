//package com.developer.ev2z
//
//
//import android.Manifest
//import androidx.annotation.RequiresPermission
//import com.developer.ev2z.util.NotificationUtil
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//        println("🔥 ANDROID FCM TOKEN: $token")
//
//        // CMP common bridge
//        FcmBridge.onToken(token)
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    override fun onMessageReceived(message: RemoteMessage) {
//        super.onMessageReceived(message)
//
//        val title =
//            message.notification?.title ?: message.data["title"]
//
//        val body =
//            message.notification?.body ?: message.data["body"]
//
//        println("📩 ANDROID FCM MSG")
//        println("Title: $title")
//        println("Body: $body")
//        println("Data: ${message.data}")
//
//        // ✅ SHOW NOTIFICATION EVEN WHEN APP IS OPEN
//        NotificationUtil.show(
//            context = applicationContext,
//            title = title,
//            body = body
//        )
//    }
//}


//package com.developer.ev2z
//
//import android.Manifest
//import androidx.annotation.RequiresPermission
//import com.developer.ev2z.util.NotificationUtil
//import com.developer.ev2z.util.AlertPrefs
//import com.developer.ev2z.util.SmsUtil
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//import com.developer.ev2z.util.AlarmPlayer
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//        println("🔥 ANDROID FCM TOKEN: $token")
//        FcmBridge.onToken(token)
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    override fun onMessageReceived(message: RemoteMessage) {
//        super.onMessageReceived(message)
//
//        val data = message.data
//        val type = data["type"]
//
//        val title = message.notification?.title ?: data["title"]
//        val body = message.notification?.body ?: data["body"]
//
//        println("📩 ANDROID FCM MSG")
//        println("Title: $title")
//        println("Body: $body")
//        println("Data: $data")
//
//        // ✅ Always show notification
//        NotificationUtil.show(
//            context = applicationContext,
//            title = title,
//            body = body
//        )
//
//        // 🔥 NEW: Handle Geofence Alerts
//        when (type) {
//            "GEOFENCE_BREACH" -> {
//                println("🔔 BREACH — forcing alarm")
//                AlarmPlayer.play() // test without any conditions
//                val deviceId = data["device_id"] ?: "Unknown"
//                val playAlarm = data["play_alarm"] == "1"
//                val sendSms   = data["send_sms"] == "1"
//
//                println("🔔 GEOFENCE_BREACH received")
//                println("   playAlarm flag from data: ${data["play_alarm"]}")
//                println("   AlertPrefs.alarmEnabled: ${AlertPrefs.alarmEnabled}")
//                println("   playAlarm computed: $playAlarm")
//
//                if (playAlarm && AlertPrefs.alarmEnabled) {
//                    println("✅ Calling AlarmPlayer.play()")
//                    AlarmPlayer.play()
//                } else {
//                    println("❌ Alarm skipped — playAlarm=$playAlarm, alarmEnabled=${AlertPrefs.alarmEnabled}")
//                }
//            }
//            else -> {
//                println("⚠️ Unknown FCM type: $type")
//            }
//        }
//    }
//}

package com.developer.ev2z

import android.Manifest
import androidx.annotation.RequiresPermission
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.AlertPrefs
import com.developer.ev2z.util.SmsUtil
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.developer.ev2z.util.AlarmPlayer

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

        val title = message.notification?.title ?: data["title"]
        val body  = message.notification?.body  ?: data["body"]

        println("📩 ANDROID FCM MSG")
        println("Title: $title")
        println("Body: $body")
        println("Data: $data")

        // ✅ Always show notification
        NotificationUtil.show(
            context = applicationContext,
            title = title,
            body = body
        )

        // 🔥 Handle Geofence Alerts
        when (type) {

            "GEOFENCE_BREACH" -> {
                println("🔔 GEOFENCE_BREACH received — deviceId: ${data["device_id"]}")

                // ✅ FIX: removed play_alarm check — BREACH itself is enough
                // play_alarm defaults to true if not sent by backend
                val playAlarm = data["play_alarm"]?.equals("1") ?: true
                val sendSms   = data["send_sms"]?.equals("1")   ?: false

                println("   playAlarm: $playAlarm, alarmEnabled: ${AlertPrefs.alarmEnabled}")
                println("   sendSms: $sendSms, smsEnabled: ${AlertPrefs.smsEnabled}")

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