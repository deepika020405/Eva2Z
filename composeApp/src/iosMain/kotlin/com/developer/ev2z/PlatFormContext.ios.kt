package com.developer.ev2z

import androidx.compose.runtime.Composable

@Composable
actual fun getPlatformContext(): Any? {
    // iOS has no "context" like Android.
    // Compose allows returning null safely.
    return null
}

actual fun getAppContext(): Any? {
    // No global application context exists in iOS.
    return null
}