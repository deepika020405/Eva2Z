package com.developer.ev2z.calling

// Swift implements this interface (compiled to ObjC protocol) and registers itself.
// VoiceCallManager.ios.kt delegates every call operation to the registered delegate.
interface IOSWebRtcDelegate {
    fun startAsCallee(roomId: String)
    fun startAsCaller(roomId: String)
    fun endCall()
    // Joins the signaling room and immediately emits end-call so the caller's
    // screen dismisses without waiting for the ring timeout.
    fun connectAndDecline(roomId: String)
    fun setMuted(muted: Boolean)
    fun setSpeaker(speaker: Boolean)
    fun dispose()
}

object WebRtcBridgeHolder {
    var delegate: IOSWebRtcDelegate? = null
}