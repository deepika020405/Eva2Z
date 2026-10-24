package com.developer.ev2z.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.developer.ev2z.data.local.createStorageManager

/**
 * Asks, once, to be left alone by the battery optimiser.
 *
 * Without the exemption the vehicle alerts behave exactly as reported — fine while the
 * app is open, silent once it is closed. Doze holds back the poll's network while the
 * phone sleeps, and the OEM skins go further and freeze the process outright, so the
 * foreground service is alive but unable to ask the server anything until the user next
 * picks the phone up.
 *
 * Asked once and remembered: a dialog that reappears on every launch is a dialog that
 * gets dismissed on every launch. The user can still grant it later from Settings, and
 * the alerts work without it — just at the mercy of whenever the phone wakes.
 */
object BatteryOptimization {

    private const val ASKED_KEY = "battery_optimization_asked"

    fun isExempt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read battery optimisation state: ${e.message}")
            false
        }
    }

    /**
     * Shows the system exemption prompt if this device has not been asked before and is
     * not exempt already. Call it once the user is signed in — asking a login screen to
     * stay awake for alerts it cannot receive yet is a dialog with no story behind it.
     */
    // REQUEST_IGNORE_BATTERY_OPTIMIZATIONS is flagged by lint as a policy-sensitive
    // permission; it is declared for exactly this prompt, and the feature degrades
    // rather than breaks when the user says no.
    @SuppressLint("BatteryLife")
    fun requestExemptionOnce(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val storage = try {
            createStorageManager()
        } catch (e: Exception) {
            return
        }

        if (storage.getString("token", "").isBlank()) return
        if (isExempt(activity)) return
        if (storage.getBoolean(ASKED_KEY, false)) return

        storage.saveBoolean(ASKED_KEY, true)

        try {
            activity.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${activity.packageName}"),
                )
            )
            Log.d(TAG, "Battery optimisation exemption requested")
        } catch (e: Exception) {
            // Some OEM builds ship no activity for this intent. Nothing to fall back on,
            // and nothing worth crashing over.
            Log.w(TAG, "No battery optimisation screen on this device: ${e.message}")
        }
    }

    private const val TAG = "BatteryOptimization"
}
