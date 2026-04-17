package com.developer.ev2z.webrtc

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import org.webrtc.*
import java.util.concurrent.Executors

class WebRtcManager(private val context: Context) {

    interface Listener {
        fun onLocalDescriptionCreated(sdp: SessionDescription)
        fun onIceCandidateGenerated(candidate: IceCandidate)
        fun onConnectionEstablished()
        fun onConnectionFailed()
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioManager: AudioManager? = null
    private var listener: Listener? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun init(listener: Listener) {
        this.listener = listener
        println("🔧 [WebRTC] Initializing PeerConnectionFactory…")
        executor.execute {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )
            factory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .createPeerConnectionFactory()
            println("✅ [WebRTC] PeerConnectionFactory created")

            audioManager = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).also {
                // Request audio focus before changing mode — prevents AudioRecord error -20 on OPPO/Realme
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .build()
                    val result = it.requestAudioFocus(focusRequest)
                    println("🔊 [WebRTC] Audio focus request result=$result (1=GRANTED)")
                } else {
                    @Suppress("DEPRECATION")
                    val result = it.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    println("🔊 [WebRTC] Audio focus request result=$result (1=GRANTED)")
                }
                it.mode = AudioManager.MODE_IN_COMMUNICATION
                it.isSpeakerphoneOn = false
                println("🔊 [WebRTC] AudioManager mode=IN_COMMUNICATION speaker=off")
            }

            val audioConstraints = MediaConstraints().apply {
                // Disable processing that can cause AudioRecord init failure on some OEMs
                mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            }
            val audioSource = factory!!.createAudioSource(audioConstraints)
            localAudioTrack = factory!!.createAudioTrack("local_audio", audioSource)
            localAudioTrack?.setEnabled(true)
            println("🎤 [WebRTC] Local audio track created (enabled=${localAudioTrack?.enabled()})")

            createPeerConnectionInternal()
        }
    }

    private fun createPeerConnectionInternal() {
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        println("🔧 [WebRTC] Creating PeerConnection with ${iceServers.size} ICE servers")
        peerConnection = factory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                println("🔄 [WebRTC] Signaling state → $state")
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                println("🧊 [WebRTC] ICE connection state → $state")
                when (state) {
                    PeerConnection.IceConnectionState.CHECKING    -> println("🧊 [WebRTC] ICE: checking candidates…")
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED   -> {
                        println("✅ [WebRTC] ICE connected — audio call is live!")
                        listener?.onConnectionEstablished()
                    }
                    PeerConnection.IceConnectionState.FAILED      -> {
                        println("❌ [WebRTC] ICE failed — no valid candidate pair found")
                        listener?.onConnectionFailed()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        println("⚠️ [WebRTC] ICE disconnected")
                        listener?.onConnectionFailed()
                    }
                    PeerConnection.IceConnectionState.CLOSED      -> {
                        println("🔒 [WebRTC] ICE closed")
                        listener?.onConnectionFailed()
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                println("🧊 [WebRTC] ICE receiving=$receiving")
            }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                println("🧊 [WebRTC] ICE gathering state → $state")
            }
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    println("🧊 [WebRTC] Local ICE candidate generated — mid=${it.sdpMid} idx=${it.sdpMLineIndex}")
                    listener?.onIceCandidateGenerated(it)
                }
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {
                println("🔄 [WebRTC] Renegotiation needed")
            }
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                println("🎧 [WebRTC] Remote track added — receiver=${receiver?.id()}")
            }
        })
        localAudioTrack?.let {
            peerConnection?.addTrack(it)
            println("🎤 [WebRTC] Local audio track added to PeerConnection")
        }
    }

    fun createOffer() {
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📋 [WebRTC] createOffer() START")
        println("   PeerConnection null? ${peerConnection == null}")
        println("   Signaling state    : ${peerConnection?.signalingState()}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        executor.execute {
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }
            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    println("✅ [WebRTC] createOffer SUCCESS — type=${sdp.type}")
                    println("   SDP preview: ${sdp.description.take(120).replace("\n"," ")}…")
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            println("✅ [WebRTC] setLocalDescription (offer) SUCCESS — forwarding to signaling")
                            listener?.onLocalDescriptionCreated(sdp)
                        }
                        override fun onCreateFailure(err: String?) {}
                        override fun onSetFailure(err: String?) {
                            println("❌ [WebRTC] setLocalDescription (offer) FAILED: $err")
                        }
                    }, sdp)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(err: String?) {
                    println("❌ [WebRTC] createOffer FAILED: $err")
                }
                override fun onSetFailure(err: String?) {}
            }, constraints)
        }
    }

    fun createAnswer() {
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📋 [WebRTC] createAnswer() START")
        println("   PeerConnection null? ${peerConnection == null}")
        println("   Signaling state    : ${peerConnection?.signalingState()}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        executor.execute {
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            }
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    println("✅ [WebRTC] createAnswer SUCCESS — type=${sdp.type}")
                    println("   SDP preview: ${sdp.description.take(120).replace("\n"," ")}…")
                    // Force sendrecv so app mic audio is sent to web caller (fixes one-way audio)
                    var fixedSdpStr = sdp.description
                        .replace("a=recvonly", "a=sendrecv")
                        .replace("a=sendonly", "a=sendrecv")
                    println("📋 [WebRTC] Has sendrecv: ${fixedSdpStr.contains("a=sendrecv")}")
                    println("📋 [WebRTC] Has recvonly: ${fixedSdpStr.contains("a=recvonly")}")
                    val fixedAnswer = SessionDescription(sdp.type, fixedSdpStr)
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            println("✅ [WebRTC] setLocalDescription (answer) SUCCESS — forwarding to signaling")
                            listener?.onLocalDescriptionCreated(fixedAnswer)
                        }
                        override fun onCreateFailure(err: String?) {}
                        override fun onSetFailure(err: String?) {
                            println("❌ [WebRTC] setLocalDescription (answer) FAILED: $err")
                        }
                    }, fixedAnswer)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(err: String?) {
                    println("❌ [WebRTC] createAnswer FAILED: $err")
                }
                override fun onSetFailure(err: String?) {}
            }, constraints)
        }
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        println("📥 [WebRTC] setRemoteDescription() type=${sdp.type}")
        executor.execute {
            peerConnection?.setRemoteDescription(noopObserver(), sdp)
        }
    }

    fun addIceCandidate(candidate: IceCandidate) {
        println("🧊 [WebRTC] addIceCandidate() mid=${candidate.sdpMid}")
        executor.execute { peerConnection?.addIceCandidate(candidate) }
    }

    fun setMuted(muted: Boolean) {
        println("🎤 [WebRTC] setMuted($muted) — audio track enabled=${!muted}")
        localAudioTrack?.setEnabled(!muted)
    }

    fun setSpeaker(speaker: Boolean) {
        println("🔊 [WebRTC] setSpeaker($speaker)")
        audioManager?.isSpeakerphoneOn = speaker
    }

    fun dispose() {
        println("🗑️ [WebRTC] dispose() — closing peer connection and releasing resources")
        executor.execute {
            peerConnection?.close()
            peerConnection = null
            localAudioTrack?.dispose()
            localAudioTrack = null
            factory?.dispose()
            factory = null
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = false
            audioManager = null
            println("✅ [WebRTC] Resources released, AudioManager restored to normal mode")
        }
        executor.shutdown()
    }

    private fun sdpObserver(onSuccess: (SessionDescription) -> Unit): SdpObserver =
        object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) = onSuccess(sdp)
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {}
        }

    private fun noopObserver(): SdpObserver = sdpObserver {}
}