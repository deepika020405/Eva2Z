package com.developer.ev2z.data.local

// commonMain/com/developer/ev2z/data/local/DeepLinkManager.kt

object DeepLinkManager {

    private lateinit var storage: StorageManager

    fun init(storageManager: StorageManager) {
        storage = storageManager
    }

    fun savePendingDeepLink(path: String) {
        storage.saveString(StorageKeys.PENDING_DEEP_LINK, path)
    }

    fun consumePendingDeepLink(): String? {
        val link = storage.getString(StorageKeys.PENDING_DEEP_LINK)
        if (link.isNotEmpty()) {
            storage.remove(StorageKeys.PENDING_DEEP_LINK)
            return link
        }
        return null
    }

    fun isFirstLaunch(): Boolean {
        return storage.getBoolean(StorageKeys.IS_FIRST_LAUNCH, true)
    }

    fun markFirstLaunchDone() {
        storage.saveBoolean(StorageKeys.IS_FIRST_LAUNCH, false)
    }
}