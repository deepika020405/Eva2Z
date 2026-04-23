package com.developer.ev2z.calling

// Delegate set by Swift AppDelegate → allows Kotlin cancelCallNotification()
// to dismiss the CallKit native call UI when user accepts/declines via Compose.
interface IOSCallKitDelegate {
    fun dismissFromApp()
}

object CallKitBridgeHolder {
    var delegate: IOSCallKitDelegate? = null
}