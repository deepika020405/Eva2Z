package com.developer.ev2z

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.SosNotificationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps WebRTC audio alive while a voice call is active.
 *
 * Without a foreground service, aggressive OEMs (OPPO, Xiaomi, Realme) suspend the
 * app process when it is backgrounded, killing WebRTC audio threads.
 * foregroundServiceType="microphone" prevents Android 14+ from silencing mic input.
 *
 * Lifecycle:
 *   Call accepted → ActiveCallService.start() → persistent "Call in progress" notification
 *   Call ended / user taps "End Call" → ActiveCallService.stop() → service destroyed
 */
class ActiveCallService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var timerJob: Job? = null
    private var elapsedSeconds = 0
    private var visitorName = "Visitor"

    private val endCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_END_CALL_BROADCAST) {
                Log.d(TAG, "End call tapped from notification")
                SosNotificationHandler.onCallEnded()
                isRunning = false
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_END_CALL_BROADCAST)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(endCallReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(endCallReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                visitorName = intent.getStringExtra(EXTRA_VISITOR_NAME) ?: "Visitor"
                handleStart()
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun handleStart() {
        acquireWakeLock()
        elapsedSeconds = 0

        // Android 14+ requires passing the foreground service type to startForeground.
        // ServiceCompat handles the API-level branching automatically.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        startTimer()
        Log.d(TAG, "Active call service started — WebRTC audio protected in background")
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ev2z:active_call")
            wl.acquire(3_600_000L) // max 1 hour
            wakeLock = wl
        } catch (e: Exception) {
            Log.e(TAG, "acquireWakeLock error: ${e.message}")
        }
    }

    private fun startTimer() {
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, buildNotification())
            }
        }
    }

    private fun buildNotification(): Notification {
        val mins  = elapsedSeconds / 60
        val secs  = elapsedSeconds % 60
        val timer = String.format("%02d:%02d", mins, secs)

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endCallPendingIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(ACTION_END_CALL_BROADCAST).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationUtil.CALL_SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle("Call in progress · $timer")
            .setContentText(visitorName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.app_logos, "End Call", endCallPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        timerJob?.cancel()
        timerJob = null
        try { unregisterReceiver(endCallReceiver) } catch (_: Exception) {}
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy wakelock error: ${e.message}")
        }
        Log.d(TAG, "Active call service destroyed — wake lock released")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG             = "ActiveCallService"
        const val NOTIFICATION_ID         = 9003

        private const val ACTION_START             = "com.developer.ev2z.ACTION_START_ACTIVE_CALL"
        private const val ACTION_STOP              = "com.developer.ev2z.ACTION_STOP_ACTIVE_CALL"
        private const val ACTION_END_CALL_BROADCAST = "com.developer.ev2z.ACTION_END_CALL"
        private const val EXTRA_VISITOR_NAME       = "visitor_name"

        @Volatile var isRunning = false
            private set

        fun start(context: Context, visitorName: String? = null) {
            if (isRunning) return
            isRunning = true
            val intent = Intent(context, ActiveCallService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_VISITOR_NAME, visitorName ?: "Visitor")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "startForegroundService sent")
        }

        fun stop(context: Context) {
            isRunning = false
            context.stopService(Intent(context, ActiveCallService::class.java))
            Log.d(TAG, "stopService sent")
        }
    }
}