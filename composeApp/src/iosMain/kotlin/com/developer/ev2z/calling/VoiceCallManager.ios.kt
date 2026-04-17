package com.developer.ev2z.calling

// iOS stub — native WebRTC calling would be implemented in the Swift layer (PaymentBridge pattern).
actual class VoiceCallManager actual constructor() {
    actual fun startAsCallee(roomId: String) {}
    actual fun startAsCaller(roomId: String) {}
    actual fun endCall() {}
    actual fun setMuted(muted: Boolean) {}
    actual fun setSpeaker(speaker: Boolean) {}
    actual fun dispose() {}
}