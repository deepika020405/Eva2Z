package com.developer.ev2z.calling

import com.developer.ev2z.AppContextHolder
import com.developer.ev2z.CallRingtoneService
import com.developer.ev2z.IncomingCallSocketWatcher
import com.developer.ev2z.util.SosNotificationHandler

private const val SIGNAL_SERVER = "https://gps.eva2z.in"

private var activeWatcher: IncomingCallSocketWatcher? = null

actual fun startIncomingCallWatcher(roomId: String) {
    if (roomId.isBlank()) return
    stopIncomingCallWatcher()
    val watcher = IncomingCallSocketWatcher(
        serverUrl = SIGNAL_SERVER,
        roomId    = roomId,
        onAcceptedElsewhere = {
            println("📱 [CallWatcher] Accepted elsewhere — dismissing IncomingCallScreen")
            AppContextHolder.context?.let { CallRingtoneService.stop(it) }
            SosNotificationHandler.onCallAcceptedElsewhere()
        },
        onEnded = {
            println("📵 [CallWatcher] Call ended — dismissing IncomingCallScreen")
            AppContextHolder.context?.let { CallRingtoneService.stop(it) }
            SosNotificationHandler.onCallEnded()
        }
    )
    watcher.connect()
    activeWatcher = watcher
}

actual fun stopIncomingCallWatcher() {
    activeWatcher?.disconnect()
    activeWatcher = null
}