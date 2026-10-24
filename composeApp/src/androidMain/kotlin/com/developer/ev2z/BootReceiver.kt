package com.developer.ev2z

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.developer.ev2z.util.StatusCheckScheduler

/**
 * Puts the vehicle alerts back after a reboot or an app update.
 *
 * An alarm chain does not survive either — the pending alarm is gone with the boot, and
 * until something arms a new one the phone is as quiet as if the feature did not exist.
 * WorkManager restores its own schedule across a reboot; the alarm has to be asked.
 *
 * The check reads the session itself and does nothing without one, so a phone that
 * rebooted while logged out stays silent.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // OEM early-boot broadcast — some skins send this and never the standard one.
            "android.intent.action.QUICKBOOT_POWERON",
            -> {
                Log.d("BootReceiver", "Restoring vehicle alerts after ${intent.action}")
                StatusCheckScheduler.schedule(context)
                StatusAlarmScheduler.scheduleNext(context)
            }
        }
    }
}
