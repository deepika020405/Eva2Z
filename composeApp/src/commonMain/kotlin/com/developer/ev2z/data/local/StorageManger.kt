package com.developer.ev2z.data.local


interface StorageManager {
    fun saveString(key: String, value: String)
    fun getString(key: String, defaultValue: String = ""): String
    fun saveBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun remove(key: String)
    fun clear()
}

// Factory function
expect fun createStorageManager(): StorageManager