package com.developer.ev2z

import androidx.compose.runtime.Composable
import platform.UserNotifications.UNUserNotificationCenter

@Composable
actual fun getPlatformContext(): Any? {
    return null
}

actual fun getAppContext(): Any? {
    return null
}

actual fun cancelCallNotification() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.removePendingNotificationRequestsWithIdentifiers(listOf("incoming-call"))
    center.removeDeliveredNotificationsWithIdentifiers(listOf("incoming-call"))
}

actual fun stopCallSound() {
    // No-op on iOS — ringtone is handled natively via CallKit
}