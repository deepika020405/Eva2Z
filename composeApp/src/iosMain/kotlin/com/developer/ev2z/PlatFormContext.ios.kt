package com.developer.ev2z

import androidx.compose.runtime.Composable
import com.developer.ev2z.calling.CallKitBridgeHolder
import com.developer.ev2z.calling.RingtoneBridgeHolder
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
    // Stop foreground ringtone and dismiss CallKit native call UI
    RingtoneBridgeHolder.delegate?.stopRingtone()
    CallKitBridgeHolder.delegate?.dismissFromApp()
}

actual fun stopCallSound() {
    RingtoneBridgeHolder.delegate?.stopRingtone()
}

actual fun platformName() = "ios"