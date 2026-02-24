//package com.developer.ev2z.util
//
//
//import android.Manifest
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.Context
//import android.os.Build
//import androidx.annotation.RequiresPermission
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.developer.ev2z.R
//
//object NotificationUtil {
//
//    const val CHANNEL_ID = "default_channel"
//
//    fun createChannel(context: Context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "General Notifications",
//                NotificationManager.IMPORTANCE_HIGH
//            ).apply {
//                description = "EV2Z Notifications"
//            }
//
//            val manager =
//                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            manager.createNotificationChannel(channel)
//        }
//    }
//
//    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
//    fun show(
//        context: Context,
//        title: String?,
//        body: String?
//    ) {
//        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(R.drawable.app_logos) // ⚠️ must exist
//            .setContentTitle(title ?: "EV2Z")
//            .setContentText(body ?: "New notification")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setAutoCancel(true)
//            .build()
//
//        NotificationManagerCompat.from(context)
//            .notify(System.currentTimeMillis().toInt(), notification)
//    }
//}


package com.developer.ev2z.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.developer.ev2z.AlarmStopReceiver
import com.developer.ev2z.R

object NotificationUtil {

    const val CHANNEL_ID = "default_channel"
    const val GEOFENCE_NOTIFICATION_ID = 9001  // ✅ add karo


    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "EV2Z Notifications"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(
        context: Context,
        title: String?,
        body: String?
    ) {
        // 🛑 Stop Alarm PendingIntent
        val stopIntent = Intent(context, AlarmStopReceiver::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isGeofenceAlert = title?.contains("Geofence", ignoreCase = true) == true
                || body?.contains("geofence", ignoreCase = true) == true
                || body?.contains("crossed", ignoreCase = true) == true

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle(title ?: "EV2Z")
            .setContentText(body ?: "New notification")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // ✅ Only add Stop button for geofence alerts
        if (isGeofenceAlert) {
            builder.addAction(
                R.drawable.app_logos,
                "🛑 Stop Alarm",
                stopPendingIntent
            )
        }

        val notificationId = if (isGeofenceAlert) {
            GEOFENCE_NOTIFICATION_ID  // fixed ID so button stays visible
        } else {
            System.currentTimeMillis().toInt()  // unique ID for other notifications
        }

        NotificationManagerCompat.from(context)
            .notify(notificationId, builder.build())
    }
}