package com.developer.ev2z.data.local

import platform.Foundation.NSUserDefaults

class IOSStorageManager : StorageManager {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun saveString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
        userDefaults.synchronize()
    }

    override fun getString(key: String, defaultValue: String): String {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    override fun saveBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, forKey = key)
        userDefaults.synchronize()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return userDefaults.boolForKey(key)
    }

    override fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
        userDefaults.synchronize()
    }

    override fun clear() {
        val dictionary = userDefaults.dictionaryRepresentation()
        dictionary.keys.forEach { key ->
            userDefaults.removeObjectForKey(key as String)
        }
        userDefaults.synchronize()
    }
}

actual fun createStorageManager(): StorageManager {
    return IOSStorageManager()
}