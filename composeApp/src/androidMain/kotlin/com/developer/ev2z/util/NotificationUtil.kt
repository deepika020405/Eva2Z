package com.developer.ev2z.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.developer.ev2z.MainActivity
import com.developer.ev2z.R

object NotificationUtil {

    const val CHANNEL_ID      = "default_channel"
    const val CALL_CHANNEL_ID = "incoming_call_channel"

    const val GEOFENCE_NOTIFICATION_ID = 9001
    const val CALL_NOTIFICATION_ID     = 9002

    // ── General notification channel (chat, geofence, etc.) ──────────
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "EV2Z Notifications"
                setSound(notifSound, audioAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    // ── Incoming-call channel (ringtone sound, repeating) ─────────────
    fun createCallChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming calls from parking visitors"
                setSound(ringtoneUri, audioAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            }

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    // ── Regular notification (chat / geofence) ────────────────────────
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(
        context: Context,
        title: String?,
        body: String?,
        alertId: Int? = null,
        visitorName: String? = null,
        visitorPhone: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            alertId?.let { putExtra("sos_alert_id", it) }
            if (!visitorName.isNullOrBlank())  putExtra("sos_visitor_name", visitorName)
            if (!visitorPhone.isNullOrBlank()) putExtra("sos_visitor_phone", visitorPhone)
        }

        val requestCode = alertId ?: System.currentTimeMillis().toInt()
        val clickPendingIntent = PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isGeofenceAlert =
            title?.contains("Geofence", true) == true ||
            body?.contains("geofence",  true) == true ||
            body?.contains("crossed",   true) == true

        val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle(title ?: "EV2Z")
            .setContentText(body ?: "New notification")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(notifSound)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setAutoCancel(true)
            .setContentIntent(clickPendingIntent)

        val notificationId =
            if (isGeofenceAlert) GEOFENCE_NOTIFICATION_ID
            else alertId ?: System.currentTimeMillis().toInt()

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    // ── Incoming-call notification (ringtone + full-screen intent) ────
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCallNotification(
        context: Context,
        visitorName: String?,
        visitorPhone: String?,
        roomId: String?,
        chatId: Int?
    ) {
        val callerDisplay = visitorName?.takeIf { it.isNotBlank() } ?: "Visitor"
        val callerPhone   = visitorPhone?.takeIf { it.isNotBlank() } ?: ""

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("is_incoming_call", true)
            chatId?.let { putExtra("sos_alert_id", it) }
            if (callerDisplay.isNotBlank()) putExtra("sos_visitor_name", callerDisplay)
            if (callerPhone.isNotBlank())   putExtra("sos_visitor_phone", callerPhone)
            if (!roomId.isNullOrBlank())    putExtra("call_room_id", roomId)
        }

        val requestCode      = chatId ?: System.currentTimeMillis().toInt()
        val contentPendingIntent = PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, requestCode + 50_000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(context, CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle("Incoming Call")
            .setContentText(
                if (callerPhone.isNotBlank()) "$callerDisplay • $callerPhone"
                else callerDisplay
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setAutoCancel(true)
            .setTimeoutAfter(30_000L)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)

        // FLAG_INSISTENT makes the ringtone repeat until user interacts
        val notification = builder.build()
        @Suppress("DEPRECATION")
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        NotificationManagerCompat.from(context).notify(CALL_NOTIFICATION_ID, notification)
    }

    // ── Cancel ringing notification (call_ended received) ─────────────
    fun cancelCallNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(CALL_NOTIFICATION_ID)
    }

    // ── Missed call notification ──────────────────────────────────────
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMissedCallNotification(
        context:      Context,
        visitorName:  String?,
        visitorPhone: String?,
        chatId:       Int?
    ) {
        val callerDisplay = visitorName?.takeIf { it.isNotBlank() } ?: "Visitor"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            chatId?.let { putExtra("sos_alert_id", it) }
            if (!visitorName.isNullOrBlank())  putExtra("sos_visitor_name", visitorName)
            if (!visitorPhone.isNullOrBlank()) putExtra("sos_visitor_phone", visitorPhone)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            chatId ?: System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle("Missed Call")
            .setContentText("You missed a call from $callerDisplay")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context)
            .notify(chatId ?: System.currentTimeMillis().toInt(), builder.build())
    }
}