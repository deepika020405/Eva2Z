package com.developer.ev2z.data.local

import android.content.Context
import android.content.SharedPreferences

class AndroidStorageManager(private val context: Context) : StorageManager {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("eva2z_prefs", Context.MODE_PRIVATE)

    override fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    override fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

// Global variable to hold context
private lateinit var appContext: Context

fun initStorageManager(context: Context) {
    appContext = context.applicationContext
}

actual fun createStorageManager(): StorageManager {
    return AndroidStorageManager(appContext)
}