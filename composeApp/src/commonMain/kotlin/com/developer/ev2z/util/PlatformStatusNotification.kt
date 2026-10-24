package com.developer.ev2z.util

/**
 * Posts a vehicle status alert into the phone's notification tray.
 *
 * The Moving and Parked alerts arrive as server pushes (`type=vehicle_notification`),
 * which both platforms already render generically — see `MyFirebaseMessagingService`
 * and `AppDelegate`. Idle has no push behind it, so the app raises that one itself from
 * what the live-location poll reports; this is the one call that differs per platform.
 *
 * [key] identifies the alert rather than the occasion: posting the same key again
 * replaces the banner already on screen instead of stacking a second one, so a device
 * that idles twice never leaves two notifications behind. Use one key per device per
 * status — `"vehicle-idle-<deviceId>"`.
 */
expect fun showVehicleStatusNotification(key: String, title: String, body: String)
