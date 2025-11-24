package com.developer.ev2z.data.local

actual class StorageProvider {
    actual fun get(): StorageManager {
        return IOSStorageManager()
    }
}