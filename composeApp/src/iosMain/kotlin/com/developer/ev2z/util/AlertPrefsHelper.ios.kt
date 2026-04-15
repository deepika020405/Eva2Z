package com.developer.ev2z.util

import platform.Foundation.NSUserDefaults

actual fun saveAlertPrefs(alarmEnabled: Boolean, smsEnabled: Boolean) {
    // NSUserDefaults.standardUserDefaults.setBool(alarmEnabled, "alarm_enabled")  // ── ALARM DISABLED
    NSUserDefaults.standardUserDefaults.setBool(smsEnabled, "sms_enabled")
    println("✅ AlertPrefs saved (iOS) — sms=$smsEnabled")
}

actual fun loadAlertPrefs() {
    val defaults = NSUserDefaults.standardUserDefaults
    // ── ALARM DISABLED ──────────────────────────────────────────
    // AlertPrefs.alarmEnabled = if (defaults.objectForKey("alarm_enabled") != null)
    //     defaults.boolForKey("alarm_enabled") else true
    AlertPrefs.smsEnabled = if (defaults.objectForKey("sms_enabled") != null)
        defaults.boolForKey("sms_enabled") else true
    println("✅ AlertPrefs loaded (iOS) — sms=${AlertPrefs.smsEnabled}")
}