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
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.developer.ev2z.IncomingCallActivity
import com.developer.ev2z.MainActivity
import com.developer.ev2z.R

object NotificationUtil {

    // v4: force-recreate channels — v3 may exist on devices without sound settings locked in.
    const val CHANNEL_ID              = "eva2z_general_v4"
    const val CALL_CHANNEL_ID         = "eva2z_call_v4"
    // Silent channel used by CallRingtoneService's foreground notification.
    // The service plays the ringtone itself via playRingtone(); the notification
    // must not also trigger channel sound or the user hears double ringing.
    const val CALL_SERVICE_CHANNEL_ID = "eva2z_call_service_v4"
    // Silent channel for VehicleStatusService's ongoing notification.
    const val TRACKING_CHANNEL_ID     = "eva2z_tracking_v1"

    const val GEOFENCE_NOTIFICATION_ID = 9001
    const val CALL_NOTIFICATION_ID     = 9002

    // Vault expiry reminders get their own id/request-code range, offset far enough
    // above the fixed ids above that a document id can never collide with them.
    private const val VAULT_NOTIFICATION_ID_BASE = 20_000
    private const val VAULT_REQUEST_CODE_BASE    = 20_000

    // Locally raised vehicle status alerts (Engine Idle), one id per device, kept above
    // the vault block so a device id and a document id can never land on each other.
    private const val VEHICLE_STATUS_ID_BASE = 30_000

    // ── Android 14+: request USE_FULL_SCREEN_INTENT if not granted ───
    fun requestFullScreenIntentPermissionIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.canUseFullScreenIntent()) {
                Log.w("NotificationUtil", "⚠️ USE_FULL_SCREEN_INTENT not granted")
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    android.net.Uri.parse("package:${context.packageName}")
                ).apply { flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK }
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        }
    }

    // ── Delete stale channels from old app versions ───────────────────
    private fun deleteOldChannels(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            listOf(
                "default_channel", "incoming_call_channel",
                "eva2z_general_v2", "eva2z_call_v2",
                "eva2z_general_v3", "eva2z_call_v3"
            ).forEach { old ->
                nm.deleteNotificationChannel(old)
            }
        }
    }

    // ── General notification channel (chat / geofence / SOS) ─────────
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            deleteOldChannels(nm)

            val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Eva2z Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chat, geofence and SOS alerts"
                setSound(notifSound, audioAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    // ── Silent channel for VehicleStatusService's own notification ────
    // MIN importance and no sound: this is the permanent "Vehicle alerts on" row the
    // foreground service is required to show, not an alert in its own right. The alerts
    // it raises go out on CHANNEL_ID like every other one.
    fun createTrackingChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                "Eva2z Vehicle Alerts",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps moving / idle / parked alerts running in the background"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            nm.createNotificationChannel(channel)
        }
    }

    // ── Incoming-call channel (ringtone, repeating) ───────────────────
    fun createCallChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            deleteOldChannels(nm)

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Eva2z Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming voice calls from parking visitors"
                setSound(ringtoneUri, audioAttr)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    // ── Silent channel for CallRingtoneService foreground notification ─
    // No sound/vibration — the service's playRingtone() handles audio.
    fun createCallServiceChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CALL_SERVICE_CHANNEL_ID,
                "Eva2z Call Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Silent channel for ongoing call ringtone service"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
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
        visitorPhone: String? = null,
        // Set for type=VAULT_EXPIRY pushes. Routes the tap to the document vault
        // instead of the SOS chat screen that sos_alert_id would open.
        vaultDocumentId: String? = null,
        vaultDocumentType: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            alertId?.let { putExtra("sos_alert_id", it) }
            if (!visitorName.isNullOrBlank())  putExtra("sos_visitor_name", visitorName)
            if (!visitorPhone.isNullOrBlank()) putExtra("sos_visitor_phone", visitorPhone)
            if (!vaultDocumentId.isNullOrBlank()) {
                putExtra("vault_document_id", vaultDocumentId)
                if (!vaultDocumentType.isNullOrBlank()) {
                    putExtra("vault_document_type", vaultDocumentType)
                }
            }
        }

        val requestCode = alertId
            ?: vaultDocumentId?.toIntOrNull()?.let { VAULT_REQUEST_CODE_BASE + it }
            ?: System.currentTimeMillis().toInt()
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

        // Reuse one id per vault document so a repeated expiry reminder for the same
        // document replaces the previous banner instead of stacking a new one.
        val notificationId = when {
            isGeofenceAlert -> GEOFENCE_NOTIFICATION_ID
            vaultDocumentId?.toIntOrNull() != null ->
                VAULT_NOTIFICATION_ID_BASE + vaultDocumentId.toInt()
            else -> alertId ?: System.currentTimeMillis().toInt()
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    // ── Vehicle status alert the app raised itself (Engine Idle) ──────
    /**
     * Same channel, icon and sound as the pushed Moving/Parked banners — see
     * `showVehicleStatusNotification`, the only caller.
     *
     * [key] is the identity of the alert, not of the occasion: it is hashed into a
     * stable notification id so the same device idling again replaces its own banner
     * instead of stacking a second one. The id is offset into its own range, clear of
     * the fixed geofence/call ids and of the vault block above.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showVehicleStatus(context: Context, title: String, body: String, key: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val notificationId = VEHICLE_STATUS_ID_BASE + (key.hashCode() and 0xFFFF)
        val clickPendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(clickPendingIntent)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        Log.d("NotificationUtil", "🔔 vehicle status notification posted — $key: $title")
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

        val requestCode = chatId ?: System.currentTimeMillis().toInt()

        // Call extras shared by both intents
        fun Intent.addCallExtras() {
            putExtra("is_incoming_call", true)
            chatId?.let { putExtra("sos_alert_id", it) }
            if (callerDisplay.isNotBlank()) putExtra("sos_visitor_name", callerDisplay)
            if (callerPhone.isNotBlank())   putExtra("sos_visitor_phone", callerPhone)
            if (!roomId.isNullOrBlank())    putExtra("call_room_id", roomId)
        }

        // Tap-on-notification → open MainActivity with is_incoming_call=true so
        // handleSosIntent() emits to callFlow and HomeNavGraph shows IncomingCallScreen.
        // This avoids opening IncomingCallActivity on top of an already-running app.
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("is_incoming_call", true)
            addCallExtras()
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, requestCode, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full-screen intent — fires immediately when screen is off / device locked.
        // Uses IncomingCallActivity so it appears in ~200ms without full App() boot.
        // On Android 14+, USE_FULL_SCREEN_INTENT may need to be granted by the user
        // in Settings → Special app access. If not granted, the notification falls
        // back to a heads-up banner that the user can tap (contentIntent above).
        val fullScreenIntent = Intent(context, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCallExtras()
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, requestCode + 50_000, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // On Android 14+ check if we're allowed to use full-screen intents.
        // If not, the notification still rings/vibrates as heads-up.
        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .canUseFullScreenIntent()
        } else {
            true
        }

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
            .apply {
                if (canUseFullScreen) setFullScreenIntent(fullScreenPendingIntent, true)
            }
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