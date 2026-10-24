package com.developer.ev2z

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.data.local.initStorageManager
import com.developer.ev2z.util.VehicleStatusBackgroundCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

/**
 * The heartbeat that actually ticks while the app is closed.
 *
 * This began as a `while (true) { check(); delay(60s) }` loop inside a foreground
 * service, and on the phone it was built against that loop produced **one** check in
 * four backgrounded minutes instead of four: ColorOS freezes the process between
 * windows, and a frozen process's `delay` simply does not come back. Neither the service
 * nor a battery-optimisation exemption changed that — and the service cost a permanent
 * "alerts active" notification besides, which is why it is gone and this carries the
 * alerts alone.
 *
 * An alarm is delivered by the system, which thaws the process to deliver it — so the
 * cadence belongs to the OS rather than to a timer inside a process the OS has paused.
 * Each delivery re-arms the next one; a chain of one-shot alarms, not a repeating alarm,
 * because only the one-shot `…AndAllowWhileIdle` variants are honoured in Doze.
 */
class StatusAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // The broadcast's own few milliseconds are not enough for a network round trip;
        // goAsync keeps the receiver alive for it (about ten seconds, which this uses a
        // fraction of).
        val pending = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            var hasSession = true
            try {
                initStorageManager(appContext)
                hasSession = createStorageManager().getString("token", "").isNotBlank()

                if (hasSession) {
                    VehicleStatusBackgroundCheck.run()
                } else {
                    Log.d(TAG, "Nobody signed in — ending the chain")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Alarm check failed: ${e.message}")
            } finally {
                // Re-armed whether the check worked or not: a chain that drops one
                // link is a chain that has ended, and nothing else would notice until
                // the 15-minute worker next ran. A logged-out phone is the one case
                // where ending it is right — the next launch arms it again.
                if (hasSession) StatusAlarmScheduler.scheduleNext(appContext)
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "StatusAlarmReceiver"
    }
}

/**
 * Arms the next status check.
 *
 * Exact where the system allows it and inexact where it does not — the difference is a
 * minute against roughly nine in Doze, and both are better than a loop that never wakes.
 * `canScheduleExactAlarms` is false on Android 13+ until the user grants it (the
 * battery-optimisation exemption usually carries it), so this is a runtime question, not
 * a manifest one.
 */
object StatusAlarmScheduler {

    private const val TAG = "StatusAlarmScheduler"
    private const val REQUEST_CODE = 9104

    /**
     * One check a minute while the system is willing — the floor on how quickly a
     * status change can be noticed with the app closed. The notification itself waits
     * for nothing once the change is seen (see `VehicleStatusNotifier`); this interval
     * is simply how often the app is allowed to look.
     */
    const val ALARM_INTERVAL_MS = 60_000L

    fun scheduleNext(context: Context, delayMs: Long = ALARM_INTERVAL_MS) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        val triggerAt = SystemClock.elapsedRealtime() + delayMs

        try {
            val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    alarmManager.canScheduleExactAlarms()

            if (canBeExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent(context)
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent(context)
                )
            }
        } catch (e: Exception) {
            // SecurityException when an exact alarm is refused between the check above
            // and the call. Inexact still gets the alerts out.
            Log.w(TAG, "Exact alarm refused, falling back: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent(context)
                )
            } catch (inner: Exception) {
                Log.w(TAG, "Could not schedule the next check: ${inner.message}")
            }
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, StatusAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
