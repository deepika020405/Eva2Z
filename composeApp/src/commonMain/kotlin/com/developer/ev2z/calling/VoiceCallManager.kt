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
    fun setMuted(muted: Boolean)
    fun setSpeaker(speaker: Boolean)
    fun dispose()
}