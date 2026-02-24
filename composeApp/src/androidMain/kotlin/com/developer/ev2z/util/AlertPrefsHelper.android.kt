package com.developer.ev2z.util

import com.developer.ev2z.AppContextHolder

actual fun saveAlertPrefs(alarmEnabled: Boolean, smsEnabled: Boolean) {
    val context = AppContextHolder.context
    val prefs = context.getSharedPreferences(
        "alert_prefs",
        android.content.Context.MODE_PRIVATE
    )
    prefs.edit()
        .putBoolean("alarm_enabled", alarmEnabled)
        .putBoolean("sms_enabled", smsEnabled)
        .apply()
    println("✅ AlertPrefs saved — alarm=$alarmEnabled, sms=$smsEnabled")
}

actual fun loadAlertPrefs() {
    val context = AppContextHolder.context
    val prefs = context.getSharedPreferences(
        "alert_prefs",
        android.content.Context.MODE_PRIVATE
    )
    AlertPrefs.alarmEnabled = prefs.getBoolean("alarm_enabled", true)
    AlertPrefs.smsEnabled   = prefs.getBoolean("sms_enabled", true)
    println("✅ AlertPrefs loaded — alarm=${AlertPrefs.alarmEnabled}, sms=${AlertPrefs.smsEnabled}")
}