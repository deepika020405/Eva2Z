package com.developer.ev2z

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.VehicleStatusBackgroundCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.coroutines.cancellation.CancellationException

/**
 * Keeps the Moving / Idle / Parked alerts coming while the app is closed.
 *
 * `StatusCheckWorker` was supposed to be enough, and on stock Android it nearly is —
 * but its floor is fifteen minutes, and on the OEM skins this app actually runs on
 * (ColorOS, MIUI, Funtouch) a backgrounded app's work is frozen outright, which is the
 * "notification only arrives when I open the app" the user sees: WorkManager runs the
 * first period the moment the app launches, and then nothing until the next launch.
 *
 * A foreground service is the one thing those skins leave alone. It costs a permanent
 * low-priority notification in the tray — that is the deal Android offers, and what
 * every tracking app in this category shows.
 *
 * What it does not survive:
 * * **Force-stop from Settings.** Nothing an app can do survives that.
 * * **Android 15's six-hour daily cap on `dataSync`.** After that the system stops the
 *   service and will not let it start again until the user next opens the app;
 *   [StatusCheckWorker] carries the alerts at its own cadence in the meantime.
 * * **Doze with battery optimisation on.** Network is held back while the phone sleeps,
 *   so the alert lands when it next wakes. See `BatteryOptimization`.
 */
/*class VehicleStatusService : Service() {

    private var pollJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Started from a process with no Activity (boot, or the worker restarting us),
        // where MainActivity's initStorageManager has never run.
        initStorageManager(applicationContext)

        NotificationUtil.createTrackingChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        startPolling()

        // START_STICKY: when the system reclaims the process under memory pressure it
        // brings the service back, which is the whole point of running here instead of
        // in the app's own process.
        return START_STICKY
    }

    *//**
     * One check now, and the alarm chain for everything after it.
     *
     * The loop that used to live here — check, `delay(60s)`, repeat — managed one check
     * in four backgrounded minutes on a ColorOS phone, because a frozen process's timers
     * do not fire. [StatusAlarmScheduler] hands the cadence to the system instead; this
     * service's job is now to keep the process important enough to be allowed to run
     * when the alarm arrives.
     *//*
    private fun startPolling() {
        if (pollJob?.isActive == true) return

        pollJob = CoroutineScope(Dispatchers.Default).launch {
            val token = try {
                createStorageManager().getString("token", "")
            } catch (e: Exception) {
                ""
            }

            // Logged out — including the logout that happened while this service was
            // running. Nothing to watch, and a tray notification for a signed-out app
            // is worse than no service at all.
            if (token.isBlank()) {
                Log.d(TAG, "No session — stopping")
                StatusAlarmScheduler.cancel(applicationContext)
                isRunning = false
                stopSelf()
                return@launch
            }

            VehicleStatusBackgroundCheck.run()
            StatusAlarmScheduler.scheduleNext(applicationContext)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, NotificationUtil.TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle("Vehicle alerts on")
            .setContentText("Watching for moving, idle and parked")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(contentIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        pollJob?.cancel()
        pollJob = null
        Log.d(TAG, "Vehicle status service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "VehicleStatusService"
        private const val ACTION_STOP = "com.developer.ev2z.STOP_VEHICLE_STATUS"
        private const val NOTIFICATION_ID = 9004

        *//** Best-effort, for the worker's watchdog — the system is the real authority. *//*
        @Volatile
        var isRunning: Boolean = false
            private set

        *//**
         * Starts the service unless nobody is signed in.
         *
         * Safe to call repeatedly: a second start on a running service just re-enters
         * [onStartCommand], and [startPolling] keeps the existing loop.
         *//*
        fun start(context: Context) {
            val token = try {
                initStorageManager(context.applicationContext)
                createStorageManager().getString("token", "")
            } catch (e: Exception) {
                Log.w(TAG, "Cannot read session: ${e.message}")
                return
            }
            if (token.isBlank()) {
                Log.d(TAG, "Not starting — nobody is signed in")
                return
            }

            try {
                val intent = Intent(context, VehicleStatusService::class.java)
                // startForegroundService, not startService: from the background — boot,
                // or the worker — a plain start is illegal on O+ and throws.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
                Log.d(TAG, "Vehicle status service started")
            } catch (e: Exception) {
                // Android 12+ throws ForegroundServiceStartNotAllowedException when the
                // app has no window of its own to start from, and Android 15 refuses
                // once the dataSync budget for the day is spent. Neither is fatal: the
                // worker still runs, and the next app launch starts us again.
                Log.w(TAG, "Could not start service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            isRunning = false
            StatusAlarmScheduler.cancel(context)
            try {
                context.startService(
                    Intent(context, VehicleStatusService::class.java).setAction(ACTION_STOP)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop service: ${e.message}")
            }
        }
    }
}*/


class VehicleStatusService : Service() {

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var pollJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        initStorageManager(applicationContext)
        NotificationUtil.createTrackingChannel(this)

        Log.d(TAG, "VehicleStatusService created")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_STOP) {
            StatusAlarmScheduler.cancel(applicationContext)
            isRunning = false
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // A sticky restart may provide a null intent.
        val token = getSessionToken()

        if (token.isNullOrBlank()) {
            Log.d(TAG, "No active session; stopping service")

            StatusAlarmScheduler.cancel(applicationContext)
            isRunning = false
            stopSelf(startId)

            return START_NOT_STICKY
        }

        // A foreground service must promote itself promptly.
        try {
            val notification = buildNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification
                )
            }

            isRunning = true
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Unable to enter foreground",
                e
            )

            isRunning = false
            stopSelf(startId)

            // The scheduler/worker should provide the fallback.
            return START_NOT_STICKY
        }

        startPolling()

        // Android may recreate the service with a null intent.
        // The session is checked again before doing any work.
        return START_STICKY
    }

    /**
     * Executes one check per service start.
     * Future checks are scheduled by StatusAlarmScheduler.
     */
    private fun startPolling() {
        if (pollJob?.isActive == true) return

        pollJob = serviceScope.launch {
            try {
                val token = getSessionToken()

                if (token.isNullOrBlank()) {
                    Log.d(TAG, "Session expired or logged out")

                    StatusAlarmScheduler.cancel(
                        applicationContext
                    )

                    isRunning = false
                    stopSelf()
                    return@launch
                }

                Log.d(TAG, "Running vehicle status check")

                VehicleStatusBackgroundCheck.run()

                // Schedule the next check after this check completes.
                StatusAlarmScheduler.scheduleNext(
                    applicationContext
                )

                Log.d(TAG, "Next status check scheduled")

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Vehicle status check failed",
                    e
                )

                // Preserve the future check even if this check fails.
                try {
                    StatusAlarmScheduler.scheduleNext(
                        applicationContext
                    )
                } catch (scheduleError: Exception) {
                    Log.e(
                        TAG,
                        "Unable to schedule next check",
                        scheduleError
                    )
                }
            }
        }
    }

    private fun getSessionToken(): String? {
        return try {
            initStorageManager(applicationContext)

            createStorageManager()
                .getString("token", "")
        } catch (e: Exception) {
            Log.e(TAG, "Unable to read session token", e)
            null
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            this,
            NotificationUtil.TRACKING_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.app_logos)
            .setContentTitle("EV2Z vehicle alerts active")
            .setContentText(
                "Checking moving, idle and parked status"
            )
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Do not cancel scheduled alarms when the user removes
        // the app from Recents.
        Log.d(TAG, "App removed from Recents")
    }

    override fun onDestroy() {
        isRunning = false

        pollJob?.cancel()
        pollJob = null

        serviceScope.cancel()

        super.onDestroy()

        Log.d(TAG, "VehicleStatusService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "VehicleStatusService"

        private const val ACTION_STOP =
            "com.developer.ev2z.STOP_VEHICLE_STATUS"

        private const val NOTIFICATION_ID = 9004

        @Volatile
        var isRunning: Boolean = false
            private set

        /**
         * Starts only when a valid session exists.
         */
        fun start(context: Context) {
            val appContext = context.applicationContext

            val token = try {
                initStorageManager(appContext)

                createStorageManager()
                    .getString("token", "")
            } catch (e: Exception) {
                Log.e(TAG, "Unable to check session", e)
                return
            }

            if (token.isBlank()) {
                Log.d(TAG, "Not starting: no active session")
                return
            }

            try {
                val serviceIntent = Intent(
                    appContext,
                    VehicleStatusService::class.java
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(serviceIntent)
                } else {
                    appContext.startService(serviceIntent)
                }

                Log.d(TAG, "Service start requested")

            } catch (e: Exception) {
                // Android may reject a background FGS start.
                Log.e(TAG, "Service start rejected", e)
            }
        }

        /**
         * Stops the service and cancels future status alarms.
         */
        fun stop(context: Context) {
            val appContext = context.applicationContext

            isRunning = false

            StatusAlarmScheduler.cancel(appContext)

            try {
                appContext.stopService(
                    Intent(
                        appContext,
                        VehicleStatusService::class.java
                    )
                )

                Log.d(TAG, "Service stop requested")

            } catch (e: Exception) {
                Log.e(TAG, "Unable to stop service", e)
            }
        }
    }
}
