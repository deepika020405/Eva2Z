package com.developer.ev2z.calling

actual fun startIncomingCallWatcher(roomId: String) {
    if (roomId.isBlank()) return
    println("🔌 [iOS CallWatcher] startIncomingCallWatcher roomId=$roomId")
    IncomingCallWatcherBridgeHolder.delegate?.startWatching(roomId)
}

actual fun stopIncomingCallWatcher() {
    println("🔌 [iOS CallWatcher] stopIncomingCallWatcher")
    IncomingCallWatcherBridgeHolder.delegate?.stopWatching()
}