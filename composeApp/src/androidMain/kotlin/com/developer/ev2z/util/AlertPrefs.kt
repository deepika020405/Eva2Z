//package com.developer.ev2z.util
//
//import android.content.Context
//import com.developer.ev2z.AppContextHolder
//
//fun AlertPrefs.save() {
//    val context = AppContextHolder.context
//    val prefs = context.getSharedPreferences("alert_prefs", Context.MODE_PRIVATE)
//    prefs.edit()
//        .putBoolean("alarm_enabled", alarmEnabled)
//        .putBoolean("sms_enabled", smsEnabled)
//        .apply()
//    println("✅ AlertPrefs saved — alarm=$alarmEnabled, sms=$smsEnabled")
//}
//
//fun AlertPrefs.load() {
//    val context = AppContextHolder.context
//    val prefs = context.getSharedPreferences("alert_prefs", Context.MODE_PRIVATE)
//    alarmEnabled = prefs.getBoolean("alarm_enabled", true)
//    smsEnabled   = prefs.getBoolean("sms_enabled", true)
//    println("✅ AlertPrefs loaded — alarm=$alarmEnabled, sms=$smsEnabled")
//}