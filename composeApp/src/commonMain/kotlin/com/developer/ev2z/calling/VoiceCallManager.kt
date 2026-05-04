package com.developer.ev2z.calling

// Platform-specific WebRTC + Socket.IO voice call manager.
// Android: real peer connection via stream-webrtc-android + socket.io-client.
// iOS: no-op stub (native WebRTC integration would go in Swift layer).
expect class VoiceCallManager() {
    // Call startAsCallee after the REST /call/accept succeeds (owner answers).
    fun startAsCallee(roomId: String)
    // Call startAsCaller when the owner initiates an outgoing call.
    fun startAsCaller(roomId: String)
    fun endCall()
    // Joins the signaling room and immediately emits end-call so the caller's
    // screen dismisses without waiting for an FCM or a 60s timeout.
    fun sendDeclineSignal(roomId: String)
    fun setMuted(muted: Boolean)
    fun setSpeaker(speaker: Boolean)
    fun dispose()
}