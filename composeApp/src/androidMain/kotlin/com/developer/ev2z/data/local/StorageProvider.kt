package com.developer.ev2z.data.local

import android.content.Context

actual class StorageProvider(private val context: Context) {
    actual fun get(): StorageManager {
        return AndroidStorageManager(context)
    }
}