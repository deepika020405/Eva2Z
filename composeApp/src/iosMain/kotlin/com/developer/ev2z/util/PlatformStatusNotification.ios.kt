package com.developer.ev2z.util

import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * The local banner for a status the app worked out itself.
 *
 * `AppDelegate` already posts local notifications for data-only pushes the same way, so
 * an alert raised here looks like every other vehicle notification. Authorisation
 * was requested at launch (`AppDelegate.didFinishLaunching`); if the user declined,
 * iOS drops the request and the completion handler reports it.
 *
 * No trigger — `null` means deliver now rather than on a schedule. Reusing [key] as the
 * request identifier is what makes a repeat alert for the same device replace its own
 * banner instead of stacking.
 */
actual fun showVehicleStatusNotification(key: String, title: String, body: String) {
    val content = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(body)
        setSound(UNNotificationSound.defaultSound)
    }

    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = key,
        content = content,
        trigger = null,
    )

    UNUserNotificationCenter.currentNotificationCenter()
        .addNotificationRequest(request) { error ->
            if (error != null) {
                println("❌ Status notification failed — ${error.localizedDescription}")
            } else {
                println("🔔 Status notification posted — $key")
            }
        }
}
