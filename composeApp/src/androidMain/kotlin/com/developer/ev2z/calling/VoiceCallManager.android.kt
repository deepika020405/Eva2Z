package com.developer.ev2z.calling

import android.Manifest
import android.content.pm.PackageManager
import android.media.ToneGenerator
import android.media.AudioManager
import androidx.core.content.ContextCompat
import com.developer.ev2z.AppContextHolder
import com.developer.ev2z.network.KtorClient
import com.developer.ev2z.util.NotificationUtil
import com.developer.ev2z.util.SosNotificationHandler
import com.developer.ev2z.webrtc.SocketIOSignalingClient
import com.developer.ev2z.webrtc.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.atomic.AtomicBoolean

actual class VoiceCallManager actual constructor() {

    companion object {
        // Checked by FCM handler and HomeNavGraph to suppress new incoming-call
        // events while a call is already in progress.
        @Volatile var isCallActive: Boolean = false
            private set
    }

    private val webRtc = WebRtcManager(AppContextHolder.context)
    private val signalingClient = SocketIOSignalingClient(KtorClient.SOCKET_SERVER_URL)
    private val disposed = AtomicBoolean(false)
    private var isCaller = false

    private var ringbackJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    // Cancels the outgoing call if the callee never joins within this window
    private var ringTimeoutJob: Job? = null
    private val RING_TIMEOUT_MS = 30_000L

    private fun startRingback() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            ringbackJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1500)
                    delay(3000)
                }
            }
            println("🔔 [VoiceCall] Ringback tone started")
        } catch (e: Exception) {
            println("⚠️ [VoiceCall] Could not start ringback: ${e.message}")
        }
    }

    private fun stopRingback() {
        ringbackJob?.cancel()
        ringbackJob = null
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
        println("🔕 [VoiceCall] Ringback tone stopped")
    }

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
        startRingback()
        initWebRtc()
        signalingClient.connect(roomId, signalingListener)
        // Auto-cancel: if visitor never joins within 60 s, treat as declined
        ringTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(RING_TIMEOUT_MS)
            if (!disposed.get()) {
                println("⏰ [VoiceCall] Ring timeout — no answer after ${RING_TIMEOUT_MS / 1000}s, ending call")
                SosNotificationHandler.onCallEnded()
                endCall()
                dispose()
            }
        }
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
                SosNotificationHandler.onCallConnected()
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
            ringTimeoutJob?.cancel()  // callee answered — no longer need the timeout
            ringTimeoutJob = null
            if (isCaller) {
                stopRingback()
                SosNotificationHandler.onCalleeJoined()
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
        override fun onSignalingFailed() {
            println("❌ [VoiceCall] Signaling server unreachable — ending call")
            SosNotificationHandler.onCallEnded()
            dispose()
        }
    }

    actual fun endCall() {
        println("📵 [VoiceCall] endCall() — sending end signal")
        signalingClient.sendEnd()
    }

    actual fun sendDeclineSignal(roomId: String) {
        if (roomId.isBlank()) return
        println("📵 [VoiceCall] sendDeclineSignal — notifying caller via socket room=$roomId")
        SocketIOSignalingClient(KtorClient.SOCKET_SERVER_URL).connectAndDecline(roomId)
    }

    actual fun setMuted(muted: Boolean) {
        webRtc.setMuted(muted)
    }

    actual fun setSpeaker(speaker: Boolean) {
        webRtc.setSpeaker(speaker)
    }

    actual fun dispose() {
        if (!disposed.compareAndSet(false, true)) return
        isCallActive = false
        println("🗑️ [VoiceCall] dispose() — cleaning up Socket.IO + WebRTC  isCallActive=false")
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        stopRingback()
        signalingClient.disconnect()
        webRtc.dispose()
        println("✅ [VoiceCall] Call session fully disposed")
    }
}