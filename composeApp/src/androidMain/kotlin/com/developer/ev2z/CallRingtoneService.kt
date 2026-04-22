package com.developer.ev2z

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.developer.ev2z.util.NotificationUtil

/**
 * Foreground service that plays the system ringtone for incoming calls.
 *
 * Why a foreground service?
 * - Android will not kill a foreground service, unlike a plain Ringtone
 *   started from onMessageReceived or an Activity.
 * - Works reliably in all states: foreground, background, killed, locked screen.
 * - Required for OPPO/Xiaomi/Realme devices that aggressively kill background audio.
 *
 * Lifecycle:
 *   FCM arrives → CallRingtoneService.start() → ringtone loops + call notification shown
 *   Accept / Decline / call_ended FCM → CallRingtoneService.stop() → everything cleaned up
 */
class CallRingtoneService : Service() {

    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Auto-stop after 60 s if the call is never answered / declined.
    // Prevents the foreground notification from staying forever when the
    // caller gives up and the call_ended FCM never arrives (e.g. new device
    // whose FCM token hasn't been registered with the server yet).
    private val timeoutHandler  = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        Log.d(TAG, "Auto-stop after 60 s — call was not answered")
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val visitorName  = intent.getStringExtra(EXTRA_VISITOR_NAME)  ?: "Visitor"
                val visitorPhone = intent.getStringExtra(EXTRA_VISITOR_PHONE) ?: ""
                val roomId       = intent.getStringExtra(EXTRA_ROOM_ID)       ?: ""
                val chatId       = intent.getIntExtra(EXTRA_CHAT_ID, -1).takeIf { it != -1 }
                handleStart(visitorName, visitorPhone, roomId, chatId)
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(
        visitorName: String,
        visitorPhone: String,
        roomId: String,
        chatId: Int?
    ) {
        wakeScreen()
        val notification = buildCallNotification(visitorName, visitorPhone, roomId, chatId)
        // startForeground posts the notification whose fullScreenIntent fires IncomingCallActivity
        // automatically on a locked/off screen. Do NOT also call startActivity() directly —
        // that would launch a second IncomingCallActivity instance on top of the one the
        // fullScreenIntent already started, producing duplicate call UI and a race condition.
        startForeground(NotificationUtil.CALL_NOTIFICATION_ID, notification)
        playRingtone()
        timeoutHandler.removeCallbacks(autoStopRunnable)
        timeoutHandler.postDelayed(autoStopRunnable, 60_000L)
    }

    private fun wakeScreen() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "ev2z:incoming_call"
            )
            // Hold for up to 60 s — released earlier in onDestroy when call is answered/declined.
            wl.acquire(60_000L)
            wakeLock = wl
            Log.d(TAG, "Screen wake lock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "wakeScreen error: ${e.message}")
        }
    }

    private fun playRingtone() {
        if (ringtone?.isPlaying == true) return

        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val rt = RingtoneManager.getRingtone(applicationContext, uri) ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                rt.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                rt.isLooping = true
            } else {
                @Suppress("DEPRECATION")
                rt.streamType = AudioManager.STREAM_ALARM
            }

            // Ensure alarm stream is audible; restore nothing — volume is user's setting
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            if (am.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                am.setStreamVolume(AudioManager.STREAM_ALARM, max / 2, 0)
            }

            rt.play()
            ringtone = rt
            Log.d(TAG, "Ringtone started (alarm stream, looping)")
        } catch (e: Exception) {
            Log.e(TAG, "playRingtone error: ${e.message}")
        }
    }

    private fun buildCallNotification(
        visitorName: String,
        visitorPhone: String,
        roomId: String,
        chatId: Int?
    ): Notification {
        val requestCode = chatId ?: System.currentTimeMillis().toInt()

        fun Intent.addCallExtras() {
            putExtra("is_incoming_call", true)
            chatId?.let { putExtra("sos_alert_id", it) }
            if (visitorName.isNotBlank())  putExtra("sos_visitor_name", visitorName)
            if (visitorPhone.isNotBlank()) putExtra("sos_visitor_phone", visitorPhone)
            if (roomId.isNotBlank())       putExtra("call_room_id", roomId)
        }

        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            addCallExtras()
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, requestCode, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCallExtras()
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, requestCode + 50_000, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
        } else true

        val callerDisplay = if (visitorPhone.isNotBlank()) "$visitorName • $visitorPhone"
                            else visitorName

        return NotificationCompat.Builder(this, NotificationUtil.CALL_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle("Incoming Call")
            .setContentText(callerDisplay)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPendingIntent)
            .apply { if (canUseFullScreen) setFullScreenIntent(fullScreenPendingIntent, true) }
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacks(autoStopRunnable)
        try {
            ringtone?.stop()
            ringtone = null
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy ringtone error: ${e.message}")
        }
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy wakelock error: ${e.message}")
        }
        Log.d(TAG, "Service destroyed — ringtone stopped, wake lock released")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CallRingtoneService"

        const val ACTION_START = "com.developer.ev2z.ACTION_START_RINGTONE"
        const val ACTION_STOP  = "com.developer.ev2z.ACTION_STOP_RINGTONE"

        private const val EXTRA_VISITOR_NAME  = "visitor_name"
        private const val EXTRA_VISITOR_PHONE = "visitor_phone"
        private const val EXTRA_ROOM_ID       = "room_id"
        private const val EXTRA_CHAT_ID       = "chat_id"

        // Track the roomId currently being rung so duplicate start() calls are ignored.
        @Volatile private var activeRoomId: String = ""

        fun start(
            context: Context,
            visitorName:  String?,
            visitorPhone: String?,
            roomId:       String?,
            chatId:       Int?
        ) {
            val room = roomId ?: ""
            if (room.isNotBlank() && room == activeRoomId) {
                Log.d(TAG, "Already ringing for roomId=$room — skipping duplicate start")
                return
            }
            activeRoomId = room
            val intent = Intent(context, CallRingtoneService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_VISITOR_NAME,  visitorName  ?: "Visitor")
                putExtra(EXTRA_VISITOR_PHONE, visitorPhone ?: "")
                putExtra(EXTRA_ROOM_ID,       room)
                chatId?.let { putExtra(EXTRA_CHAT_ID, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "startForegroundService sent — roomId=$room")
        }

        fun stop(context: Context) {
            activeRoomId = ""
            context.stopService(Intent(context, CallRingtoneService::class.java))
            Log.d(TAG, "stopService sent")
        }
    }
}