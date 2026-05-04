package com.developer.ev2z.calling

actual class VoiceCallManager actual constructor() {

    companion object {
        var isCallActive: Boolean = false
    }

    actual fun startAsCallee(roomId: String) {
        isCallActive = true
        println("📞 [iOS VoiceCall] startAsCallee roomId=$roomId — delegating to WebRtcBridge")
        WebRtcBridgeHolder.delegate?.startAsCallee(roomId)
    }

    actual fun startAsCaller(roomId: String) {
        isCallActive = true
        println("📞 [iOS VoiceCall] startAsCaller roomId=$roomId — delegating to WebRtcBridge")
        WebRtcBridgeHolder.delegate?.startAsCaller(roomId)
    }

    actual fun endCall() {
        println("📵 [iOS VoiceCall] endCall")
        WebRtcBridgeHolder.delegate?.endCall()
    }

    actual fun sendDeclineSignal(roomId: String) {
        println("📵 [iOS VoiceCall] sendDeclineSignal room=$roomId — delegating to bridge")
        WebRtcBridgeHolder.delegate?.connectAndDecline(roomId)
    }

    actual fun setMuted(muted: Boolean) {
        WebRtcBridgeHolder.delegate?.setMuted(muted)
    }

    actual fun setSpeaker(speaker: Boolean) {
        WebRtcBridgeHolder.delegate?.setSpeaker(speaker)
    }

    actual fun dispose() {
        isCallActive = false
        println("🗑️ [iOS VoiceCall] dispose — isCallActive=false")
        WebRtcBridgeHolder.delegate?.dispose()
    }
}