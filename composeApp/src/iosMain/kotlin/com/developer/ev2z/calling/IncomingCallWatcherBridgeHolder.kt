package com.developer.ev2z.calling

// Swift implements this interface (compiled to ObjC protocol) and registers itself.
// startIncomingCallWatcher / stopIncomingCallWatcher delegate to the registered instance.
interface IOSIncomingCallWatcherDelegate {
    fun startWatching(roomId: String)
    fun stopWatching()
}

object IncomingCallWatcherBridgeHolder {
    var delegate: IOSIncomingCallWatcherDelegate? = null
}