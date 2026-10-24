package com.developer.ev2z.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.developer.ev2z.StatusAlarmScheduler
import com.developer.ev2z.data.local.initStorageManager
import java.util.concurrent.TimeUnit

/**
 * The status check as WorkManager sees it — what keeps the Moving / Idle / Parked notifications
 * arriving with the app backgrounded or swiped away.
 *
 * This runs in a process the user never opened, so nothing can be assumed to be set up:
 * [initStorageManager] normally runs in `MainActivity`, which has not launched here, and
 * without it reading the token throws. It is idempotent, so calling it again when the
 * app *is* open costs nothing.
 */
class StatusCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        initStorageManager(applicationContext)

        // Watchdog as much as fallback: the OEM skins this app runs on kill a
        // backgrounded service without telling anyone, and Android 15 stops it outright
        // once the day's dataSync budget is spent. A periodic worker is the one thing
        // still ticking at that point, so it puts the service back before doing its own
        // check — `start` is a no-op when the service is already up or the user is
        // signed out.
        // Watchdog first: an alarm chain that lost a link — a process kill between two
        // alarms, a reboot before BootReceiver ran — has simply ended, and this periodic
        // job is the only thing still ticking that can put it back.
        StatusAlarmScheduler.scheduleNext(applicationContext)

        // `retry` only for a check that reached the network and failed — WorkManager
        // backs off and tries again inside the same period. A logged-out device returns
        // false too, and retrying that would just burn the backoff slot, but the next
        // period is 15 minutes away either way and nothing is lost by the extra attempt.
        return if (VehicleStatusBackgroundCheck.run()) Result.success() else Result.retry()
    }
}

/**
 * Arms the periodic check. Called from `MainApplication.onCreate`, so it is re-armed
 * every time the process starts; WorkManager itself persists the schedule across reboots.
 *
 * Fifteen minutes is the floor Android allows for periodic work, and the OS may stretch
 * it further under Doze — so the alert is "within the next quarter of an hour or so",
 * not the few seconds the alarm chain and the foreground poll manage.
 * A force-stop from Settings, or an aggressive OEM battery manager, stops it entirely;
 * only a server push survives that, which is how Moving and Parked arrive.
 */
object StatusCheckScheduler {

    private const val UNIQUE_NAME = "eva2z-vehicle-status-check"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<StatusCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()

        // KEEP, not REPLACE: REPLACE restarts the period on every app launch, so a user
        // who opens the app often would reset the timer before it ever fired.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        println("⏰ Status check scheduled — every 15 min, network required")
    }

    /** Stops the check. The worker also no-ops without a token, so this is belt and braces. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        println("⏹️ Status check cancelled")
    }
}
