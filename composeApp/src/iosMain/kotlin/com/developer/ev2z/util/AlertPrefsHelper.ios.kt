package com.developer.ev2z.util

import platform.Foundation.NSUserDefaults

actual fun saveAlertPrefs(alarmEnabled: Boolean, smsEnabled: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(alarmEnabled, "alarm_enabled")
    NSUserDefaults.standardUserDefaults.setBool(smsEnabled, "sms_enabled")
    println("✅ AlertPrefs saved (iOS) — alarm=$alarmEnabled, sms=$smsEnabled")
}

actual fun loadAlertPrefs() {
    val defaults = NSUserDefaults.standardUserDefaults
    AlertPrefs.alarmEnabled = if (defaults.objectForKey("alarm_enabled") != null)
        defaults.boolForKey("alarm_enabled") else true
    AlertPrefs.smsEnabled = if (defaults.objectForKey("sms_enabled") != null)
        defaults.boolForKey("sms_enabled") else true
    println("✅ AlertPrefs loaded (iOS) — alarm=${AlertPrefs.alarmEnabled}, sms=${AlertPrefs.smsEnabled}")
}