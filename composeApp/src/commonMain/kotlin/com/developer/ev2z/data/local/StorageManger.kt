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


object StorageKeys {
    const val VAULT_TOKEN = "vault_token"
    const val VAULT_EXPIRES_AT = "vault_expires_at"
    const val USER_NAME = "user_name"


    // Deep Link keys
    const val PENDING_DEEP_LINK = "pending_deep_link"
    const val IS_FIRST_LAUNCH = "is_first_launch"

}