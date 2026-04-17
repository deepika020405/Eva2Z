package com.developer.ev2z.calling

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.developer.ev2z.AppContextHolder
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.SosNotificationHandler
import com.developer.ev2z.webrtc.SocketIOSignalingClient
import com.developer.ev2z.webrtc.WebRtcManager
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

// ⚠️ IMPORTANT — SET YOUR SOCKET.IO SIGNALING SERVER URL HERE.
//
// The current URL https://app.eva2z.in does NOT have a Socket.IO server running on it.
// You are getting "xhr poll error" because this endpoint doesn't serve Socket.IO.
//
// You need to either:
//   1. Deploy a Socket.IO signaling server (see node.js example below) and put its URL here.
//   2. Ask your backend team for the correct WebSocket / Socket.IO server URL.
//
// Minimal Node.js server to run alongside your API:
//   const io = require('socket.io')(httpServer, { cors: { origin: '*' } });
//   io.on('connection', socket => {
//     socket.on('join',      d => socket.join(d.room_id));
//     socket.on('offer',     d => socket.to(d.room_id).emit('offer',     d));
//     socket.on('answer',    d => socket.to(d.room_id).emit('answer',    d));
//     socket.on('candidate', d => socket.to(d.room_id).emit('candidate', d));
//     socket.on('end',       d => socket.to(d.room_id).emit('end',       d));
//   });
//
// Replace the URL below once your signaling server is live:
private const val SIGNALING_SERVER_URL = "https://app.eva2z.in:3001"

actual class VoiceCallManager actual constructor() {

    companion object {
        // Checked by FCM handler and HomeNavGraph to suppress new incoming-call
        // events while a call is already in progress.
        @Volatile var isCallActive: Boolean = false
            private set
    }

    private val webRtc = WebRtcManager(AppContextHolder.context)
    private val signalingClient = SocketIOSignalingClient(SIGNALING_SERVER_URL)
    private var isCaller = false

    actual fun startAsCallee(roomId: String) {
        isCaller = false
        isCallActive = true
        println("📞 [VoiceCall] ── START AS CALLEE ── roomId=$roomId  isCallActive=true")
        // Stop the ringing notification immediately when user accepts
        NotificationUtil.cancelCallNotification(AppContextHolder.context)
        if (!checkMicPermission()) return
        initWebRtc()
        signalingClient.connect(roomId, signalingListener)
    }

    actual fun startAsCaller(roomId: String) {
        isCaller = true
        isCallActive = true
        println("📞 [VoiceCall] ── START AS CALLER ── roomId=$roomId  isCallActive=true")
        if (!checkMicPermission()) return
        initWebRtc()
        signalingClient.connect(roomId, signalingListener)
    }

    private fun checkMicPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            AppContextHolder.context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            println("❌ [VoiceCall] RECORD_AUDIO permission NOT granted — cannot start call")
            isCallActive = false
        } else {
            println("✅ [VoiceCall] RECORD_AUDIO permission granted")
        }
        return granted
    }

    private fun initWebRtc() {
        println("🔧 [VoiceCall] Initializing WebRTC engine (isCaller=$isCaller)")
        webRtc.init(object : WebRtcManager.Listener {
            override fun onLocalDescriptionCreated(sdp: SessionDescription) {
                println("📤 [VoiceCall] Local SDP ready (${sdp.type}) — forwarding via Socket.IO")
                if (isCaller) signalingClient.sendOffer(sdp) else signalingClient.sendAnswer(sdp)
            }
            override fun onIceCandidateGenerated(candidate: IceCandidate) {
                signalingClient.sendIceCandidate(candidate)
            }
            override fun onConnectionEstablished() {
                println("🟢 [VoiceCall] ══ CALL CONNECTED — audio is live ══")
            }
            override fun onConnectionFailed() {
                println("🔴 [VoiceCall] ══ CALL FAILED / DISCONNECTED — notifying UI ══")
                // ICE disconnected (remote hung up or network drop) → dismiss call screen
                SosNotificationHandler.onCallEnded()
                dispose()
            }
        })
    }

    private val signalingListener = object : SocketIOSignalingClient.Listener {
        override fun onRemoteOfferReceived(sdp: SessionDescription) {
            println("📥 [VoiceCall] Remote offer received — setting remote desc + creating answer")
            webRtc.setRemoteDescription(sdp)
            webRtc.createAnswer()
        }
        override fun onRemoteAnswerReceived(sdp: SessionDescription) {
            println("📥 [VoiceCall] Remote answer received — call fully negotiated")
            webRtc.setRemoteDescription(sdp)
        }
        override fun onRemoteIceCandidateReceived(candidate: IceCandidate) {
            webRtc.addIceCandidate(candidate)
        }
        override fun onUserJoined() {
            println("👤 [VoiceCall] Remote user joined room — isCaller=$isCaller")
            if (isCaller) {
                println("📋 [VoiceCall] We are caller → creating offer")
                webRtc.createOffer()
            } else {
                println("📋 [VoiceCall] We are callee → waiting for offer from caller")
            }
        }
        override fun onCallEnded() {
            println("📵 [VoiceCall] Remote peer ended call — notifying UI + disposing")
            SosNotificationHandler.onCallEnded()
            dispose()
        }
    }

    actual fun endCall() {
        println("📵 [VoiceCall] endCall() — sending end signal")
        signalingClient.sendEnd()
    }

    actual fun setMuted(muted: Boolean) {
        webRtc.setMuted(muted)
    }

    actual fun setSpeaker(speaker: Boolean) {
        webRtc.setSpeaker(speaker)
    }

    actual fun dispose() {
        isCallActive = false
        println("🗑️ [VoiceCall] dispose() — cleaning up Socket.IO + WebRTC  isCallActive=false")
        signalingClient.disconnect()
        webRtc.dispose()
        println("✅ [VoiceCall] Call session fully disposed")
    }
}