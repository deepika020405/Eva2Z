package com.developer.ev2z.data.local

expect class StorageProvider {
    fun get(): StorageManager  // ✅ Storage ki jagah StorageManager
}