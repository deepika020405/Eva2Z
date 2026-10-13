import Foundation
import AVFoundation
import WebRTC
import SocketIO
import ComposeApp

// Mirrors Android's WebRtcManager + SocketIOSignalingClient exactly.
//
// Server URL: https://gps.eva2z.in
// Events emitted : join-room, offer, answer, ice-candidate, end-call
// Events received: user-joined, offer, answer, ice-candidate, end-call

private let kSignalingServerURL = "https://gps.eva2z.in"
private let kStunServer         = "stun:stun.l.google.com:19302"
private let kStunServer2        = "stun:stun1.l.google.com:19302"

@objc class WebRtcBridge: NSObject, IOSWebRtcDelegate {

    @objc static let shared = WebRtcBridge()

    // MARK: - State

    private var roomId:   String = ""
    private var isCaller: Bool   = false

    // True while a call session is alive. AppDelegate uses this to guard against
    // duplicate incoming-call FCMs while a call is already active.
    private(set) var isCallActive: Bool = false

    // Guards against a race where the 500 ms deferred configureAudioSession fires
    // after onCallKitAudioActivated already ran (CallKit-answer path).
    private var audioSessionReady: Bool = false

    // Ring timeout — auto-ends the outgoing call if callee never joins within 30s.
    private var ringTimeoutItem: DispatchWorkItem?
    private let ringTimeoutSeconds: Double = 30.0

    // Separate socket+manager used only for connectAndDecline (visitor decline path).
    private var declineSocketManager: SocketManager?

    // WebRTC
    private var factory:         RTCPeerConnectionFactory?
    private var peerConnection:  RTCPeerConnection?
    private var localAudioTrack: RTCAudioTrack?

    // Socket.IO
    private var socketManager: SocketManager?
    private var socket:        SocketIOClient?

    // ICE candidate buffer — libwebrtc rejects addIceCandidate until the REMOTE
    // description is set, so candidates arriving before that are held here and
    // flushed once setRemoteDescription succeeds.
    private var pendingRemoteCandidates: [RTCIceCandidate] = []
    private var isRemoteDescriptionSet:  Bool = false

    // Serialises the two fields above — they are touched both from Socket.IO's
    // handler queue and from WebRTC's signaling thread (setRemoteDescription
    // completion), which would otherwise be a data race.
    private let candidateQueue = DispatchQueue(label: "com.developer.ev2z.webrtc.candidates")

    // MARK: - IOSWebRtcDelegate

    func startAsCallee(roomId: String) {
        teardownInternal()
        isCallActive          = true
        audioSessionReady     = false
        resetCandidateBuffer()
        self.roomId   = roomId
        self.isCaller = false
        print("📞 [WebRtcBridge] startAsCallee roomId=\(roomId)")
        setupWebRtc()
        connectSignaling()
        // Defer audio session setup by 500 ms. If CallKit was ringing (background/locked),
        // it holds the audio hardware; releasing and re-claiming it in the same run-loop
        // causes AUIOClient_StartIO to fail and drops ICE. The 500 ms window lets the
        // prior session fully deactivate before WebRTC opens the hardware.
        // If CallKit answers the call first, onCallKitAudioActivated sets audioSessionReady
        // immediately and this deferred call is skipped.
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard let self = self, self.isCallActive, !self.audioSessionReady else { return }
            self.audioSessionReady = true
            self.configureAudioSession()
        }
    }

    func startAsCaller(roomId: String) {
        teardownInternal()
        isCallActive          = true
        audioSessionReady     = false
        resetCandidateBuffer()
        self.roomId   = roomId
        self.isCaller = true
        print("📞 [WebRtcBridge] startAsCaller roomId=\(roomId)")
        setupWebRtc()
        connectSignaling()
        startRingTimeout()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            guard let self = self, self.isCallActive, !self.audioSessionReady else { return }
            self.audioSessionReady = true
            self.configureAudioSession()
        }
    }

    func endCall() {
        guard isCallActive else { return }
        print("📵 [WebRtcBridge] endCall — emitting end-call to room \(roomId)")
        // Emit BEFORE teardown so socket is still connected; flush then close.
        socket?.emit("end-call", ["roomId": roomId])
        DispatchQueue.global().asyncAfter(deadline: .now() + 0.3) { [weak self] in
            self?.teardownInternal()
        }
    }

    func setMuted(muted: Bool) {
        localAudioTrack?.isEnabled = !muted
        print("🎤 [WebRtcBridge] muted=\(muted)")
    }

    func setSpeaker(speaker: Bool) {
        do {
            try AVAudioSession.sharedInstance().overrideOutputAudioPort(speaker ? .speaker : .none)
            print("🔊 [WebRtcBridge] speaker=\(speaker)")
        } catch {
            print("❌ [WebRtcBridge] setSpeaker error: \(error)")
        }
    }

    func connectAndDecline(roomId: String) {
        print("📵 [WebRtcBridge] connectAndDecline → room=\(roomId)")
        guard let url = URL(string: kSignalingServerURL) else { return }

        let mgr = SocketManager(socketURL: url, config: [
            .log(false), .compress, .forceWebsockets(true), .reconnects(false)
        ])
        let s = mgr.defaultSocket
        declineSocketManager = mgr  // retain so ARC doesn't free it

        s.once(clientEvent: .connect) { _, _ in
            print("✅ [WebRtcBridge] Decline connected — joining room=\(roomId)")
            s.emit("join-room", ["roomId": roomId])
            // Wait 500ms for server to add us to the room before emitting end-call
            DispatchQueue.global().asyncAfter(deadline: .now() + 0.5) {
                print("📵 [WebRtcBridge] Decline: emitting end-call → room=\(roomId)")
                s.emit("end-call", ["roomId": roomId])
                DispatchQueue.global().asyncAfter(deadline: .now() + 1.0) {
                    s.disconnect()
                    self.declineSocketManager = nil
                }
            }
        }
        s.on(clientEvent: .error) { _, _ in
            print("❌ [WebRtcBridge] connectAndDecline: server unreachable for room=\(roomId)")
            s.disconnect()
            self.declineSocketManager = nil
        }
        s.connect()
    }

    func dispose() {
        print("🗑️ [WebRtcBridge] dispose")
        teardownInternal()
    }

    // Called by CallKitManager when the system activates the audio session after answer.
    // Reconfigure so WebRTC audio tracks use the CallKit-managed session.
    // Setting audioSessionReady=true here prevents the deferred 500 ms call from
    // firing a second time (the CallKit-activation path takes priority).
    func onCallKitAudioActivated() {
        guard isCallActive else { return }
        print("🔊 [WebRtcBridge] onCallKitAudioActivated — reconfiguring audio session")
        audioSessionReady = true
        configureAudioSession()
    }

    // MARK: - Ring timeout

    private func startRingTimeout() {
        let item = DispatchWorkItem { [weak self] in
            guard let self = self, self.isCallActive else { return }
            print("⏰ [WebRtcBridge] Ring timeout — no answer after \(Int(self.ringTimeoutSeconds))s, ending call")
            DispatchQueue.main.async { SosNotificationHandler.shared.onCallEnded() }
            self.endCall()
        }
        ringTimeoutItem = item
        DispatchQueue.main.asyncAfter(deadline: .now() + ringTimeoutSeconds, execute: item)
        print("⏱️ [WebRtcBridge] Ring timeout scheduled — \(Int(ringTimeoutSeconds))s")
    }

    private func cancelRingTimeout() {
        ringTimeoutItem?.cancel()
        ringTimeoutItem = nil
    }

    // MARK: - Audio session

    private func configureAudioSession() {
        do {
            let s = AVAudioSession.sharedInstance()
            try s.setCategory(.playAndRecord,
                              mode: .voiceChat,
                              options: [.allowBluetoothHFP, .allowBluetoothA2DP, .defaultToSpeaker])
            try s.setActive(true)
            print("✅ [WebRtcBridge] Audio session configured (voiceChat)")
        } catch {
            print("❌ [WebRtcBridge] Audio session error: \(error)")
        }
    }

    // MARK: - WebRTC setup

    private func setupWebRtc() {
        RTCInitializeSSL()

        factory = RTCPeerConnectionFactory(
            encoderFactory: RTCDefaultVideoEncoderFactory(),
            decoderFactory: RTCDefaultVideoDecoderFactory()
        )

        let config = RTCConfiguration()
        config.iceServers = [
            RTCIceServer(urlStrings: [kStunServer]),
            RTCIceServer(urlStrings: [kStunServer2])
        ]
        config.sdpSemantics             = .unifiedPlan
        config.continualGatheringPolicy = .gatherContinually
        config.iceTransportPolicy       = .all

        let constraints = RTCMediaConstraints(
            mandatoryConstraints: nil,
            optionalConstraints:  ["DtlsSrtpKeyAgreement": "true"]
        )

        peerConnection = factory?.peerConnection(with: config,
                                                  constraints: constraints,
                                                  delegate: self)

        // Audio track with the same noise/echo constraints as Android
        let audioConstraints = RTCMediaConstraints(
            mandatoryConstraints: [
                "googEchoCancellation": "true",
                "googNoiseSuppression": "true",
                "googAutoGainControl":  "true",
                "googHighpassFilter":   "true"
            ],
            optionalConstraints: nil
        )
        if let factory = factory {
            let audioSource = factory.audioSource(with: audioConstraints)
            let track       = factory.audioTrack(with: audioSource, trackId: "ARDAMSa0")
            localAudioTrack = track
            peerConnection?.add(track, streamIds: ["ARDAMS"])
        }
        print("✅ [WebRtcBridge] WebRTC peer connection ready")
    }

    // MARK: - Socket.IO signaling

    private func connectSignaling() {
        guard let url = URL(string: kSignalingServerURL) else {
            print("❌ [WebRtcBridge] Bad signaling URL"); return
        }

        socketManager = SocketManager(socketURL: url, config: [
            .log(false),
            .compress,
            .forceWebsockets(true),
            .reconnects(true),
            .reconnectAttempts(5),
            .reconnectWait(1)
        ])
        socket = socketManager?.defaultSocket

        socket?.on(clientEvent: .connect) { [weak self] _, _ in
            guard let self = self else { return }
            print("✅ [WebRtcBridge] Socket.IO connected — joining room \(self.roomId)")
            self.socket?.emit("join-room", ["roomId": self.roomId])
        }

        socket?.on(clientEvent: .error) { data, _ in
            print("❌ [WebRtcBridge] Socket.IO error: \(data)")
        }

        socket?.on(clientEvent: .disconnect) { data, _ in
            print("🔌 [WebRtcBridge] Socket.IO disconnected: \(data)")
        }

        socket?.on("user-joined") { [weak self] _, _ in
            guard let self = self else { return }
            print("👤 [WebRtcBridge] user-joined — isCaller=\(self.isCaller)")
            if self.isCaller {
                self.cancelRingTimeout()
                DispatchQueue.main.async { SosNotificationHandler.shared.onCalleeJoined() }
                self.createOffer()
            } else {
                print("📋 [WebRtcBridge] Callee — waiting for offer")
            }
        }

        socket?.on("offer") { [weak self] data, _ in
            guard let self = self,
                  let dict    = data.first as? [String: Any] else { return }
            let offerDict = (dict["offer"] as? [String: Any]) ?? dict
            guard let sdpStr = offerDict["sdp"] as? String else {
                print("❌ [WebRtcBridge] offer missing sdp"); return
            }
            print("📥 [WebRtcBridge] Received offer (sdp length=\(sdpStr.count))")
            let sdp = RTCSessionDescription(type: .offer, sdp: sdpStr)
            self.peerConnection?.setRemoteDescription(sdp) { error in
                if let error = error {
                    print("❌ [WebRtcBridge] setRemoteDescription(offer) error: \(error)"); return
                }
                self.onRemoteDescriptionSet()
                self.createAnswer()
            }
        }

        socket?.on("answer") { [weak self] data, _ in
            guard let self = self,
                  let dict     = data.first as? [String: Any] else { return }
            let answerDict = (dict["answer"] as? [String: Any]) ?? dict
            guard let sdpStr = answerDict["sdp"] as? String else {
                print("❌ [WebRtcBridge] answer missing sdp"); return
            }
            print("📥 [WebRtcBridge] Received answer (sdp length=\(sdpStr.count))")
            let sdp = RTCSessionDescription(type: .answer, sdp: sdpStr)
            self.peerConnection?.setRemoteDescription(sdp) { [weak self] error in
                if let error = error {
                    print("❌ [WebRtcBridge] setRemoteDescription(answer) error: \(error)"); return
                }
                // Caller: flush buffered remote ICE candidates now that the remote desc exists
                self?.onRemoteDescriptionSet()
            }
        }

        socket?.on("ice-candidate") { [weak self] data, _ in
            guard let self = self,
                  let dict     = data.first as? [String: Any],
                  let candDict = dict["candidate"] as? [String: Any],
                  let sdp      = candDict["candidate"]      as? String,
                  let sdpMid   = candDict["sdpMid"]         as? String else { return }
            let sdpMLineIndex = (candDict["sdpMLineIndex"] as? Int).map { Int32($0) } ?? 0
            print("🧊 [WebRtcBridge] Received ICE candidate sdpMid=\(sdpMid)")
            let candidate = RTCIceCandidate(sdp: sdp, sdpMLineIndex: sdpMLineIndex, sdpMid: sdpMid)

            self.receiveRemoteCandidate(candidate)
        }

        socket?.on("end-call") { [weak self] _, _ in
            guard let self = self, self.isCallActive else { return }
            self.isCallActive = false
            print("📵 [WebRtcBridge] end-call received — remote peer ended call")
            DispatchQueue.main.async { SosNotificationHandler.shared.onCallEnded() }
        }

        // Alternative event names the backend may emit on REST decline/reject.
        // isCallActive is flipped immediately so only the first event fires onCallEnded.
        for eventName in ["call_declined", "call_rejected", "call_ended"] {
            socket?.on(eventName) { [weak self] _, _ in
                guard let self = self, self.isCallActive else { return }
                self.isCallActive = false
                print("📵 [WebRtcBridge] \(eventName) received — ending call")
                DispatchQueue.main.async { SosNotificationHandler.shared.onCallEnded() }
            }
        }

        print("📡 [WebRtcBridge] Connecting to \(kSignalingServerURL)")
        socket?.connect()
    }

    // MARK: - SDP helpers

    private func createOffer() {
        let constraints = RTCMediaConstraints(
            mandatoryConstraints: ["OfferToReceiveAudio": "true"],
            optionalConstraints:  nil
        )
        peerConnection?.offer(for: constraints) { [weak self] sdp, error in
            guard let self = self, let sdp = sdp else {
                print("❌ [WebRtcBridge] createOffer error: \(String(describing: error))"); return
            }
            self.peerConnection?.setLocalDescription(sdp) { error in
                if let error = error {
                    print("❌ [WebRtcBridge] setLocalDescription(offer) error: \(error)"); return
                }
                print("📤 [WebRtcBridge] Emitting offer to room \(self.roomId)")
                self.socket?.emit("offer", [
                    "sdp": sdp.sdp, "type": "offer", "roomId": self.roomId
                ])
            }
        }
    }

    private func createAnswer() {
        let constraints = RTCMediaConstraints(
            mandatoryConstraints: ["OfferToReceiveAudio": "true"],
            optionalConstraints:  nil
        )
        peerConnection?.answer(for: constraints) { [weak self] sdp, error in
            guard let self = self, let sdp = sdp else {
                print("❌ [WebRtcBridge] createAnswer error: \(String(describing: error))"); return
            }
            self.peerConnection?.setLocalDescription(sdp) { error in
                if let error = error {
                    print("❌ [WebRtcBridge] setLocalDescription(answer) error: \(error)"); return
                }
                print("📤 [WebRtcBridge] Emitting answer to room \(self.roomId)")
                self.socket?.emit("answer", [
                    "sdp": sdp.sdp, "type": "answer", "roomId": self.roomId
                ])
            }
        }
    }

    // MARK: - Remote ICE candidates

    private func resetCandidateBuffer() {
        candidateQueue.sync {
            isRemoteDescriptionSet = false
            pendingRemoteCandidates.removeAll()
        }
    }

    // A remote candidate is only usable once the remote description exists;
    // before that libwebrtc rejects it outright, so hold it back.
    private func receiveRemoteCandidate(_ candidate: RTCIceCandidate) {
        candidateQueue.async { [weak self] in
            guard let self = self else { return }
            if self.isRemoteDescriptionSet {
                self.applyRemoteCandidate(candidate)
            } else {
                self.pendingRemoteCandidates.append(candidate)
                print("🧊 [WebRtcBridge] Buffering ICE candidate (remote desc not set yet)")
            }
        }
    }

    private func onRemoteDescriptionSet() {
        candidateQueue.async { [weak self] in
            guard let self = self else { return }
            self.isRemoteDescriptionSet = true
            guard !self.pendingRemoteCandidates.isEmpty else { return }
            let buffered = self.pendingRemoteCandidates
            self.pendingRemoteCandidates.removeAll()
            print("🧊 [WebRtcBridge] Flushing \(buffered.count) buffered ICE candidates")
            buffered.forEach { self.applyRemoteCandidate($0) }
        }
    }

    // The completion-handler overload surfaces failures that the deprecated
    // add(_:) swallowed silently.
    private func applyRemoteCandidate(_ candidate: RTCIceCandidate) {
        peerConnection?.add(candidate) { error in
            if let error = error {
                print("❌ [WebRtcBridge] addIceCandidate error: \(error)")
            }
        }
    }

    // MARK: - Teardown (idempotent)

    private func teardownInternal() {
        guard isCallActive else { return }
        isCallActive          = false
        audioSessionReady     = false
        resetCandidateBuffer()

        cancelRingTimeout()

        socket?.off(clientEvent: .connect)
        socket?.off(clientEvent: .error)
        socket?.off(clientEvent: .disconnect)
        ["user-joined", "offer", "answer", "ice-candidate",
         "end-call", "call_declined", "call_rejected", "call_ended"].forEach { socket?.off($0) }
        socket?.disconnect()
        socket        = nil
        socketManager = nil

        peerConnection?.close()
        peerConnection  = nil
        localAudioTrack = nil
        factory         = nil

        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        print("✅ [WebRtcBridge] Teardown complete")
    }
}

// MARK: - RTCPeerConnectionDelegate

extension WebRtcBridge: RTCPeerConnectionDelegate {

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didGenerate candidate: RTCIceCandidate) {
        print("🧊 [WebRtcBridge] Generated ICE candidate sdpMid=\(candidate.sdpMid ?? "") idx=\(candidate.sdpMLineIndex)")
        socket?.emit("ice-candidate", [
            "roomId": roomId,
            "candidate": [
                "candidate":     candidate.sdp,
                "sdpMid":        candidate.sdpMid ?? "",
                "sdpMLineIndex": candidate.sdpMLineIndex
            ]
        ])
    }

    func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {}

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didChange stateChanged: RTCSignalingState) {
        print("🔀 [WebRtcBridge] Signaling state: \(stateChanged.rawValue)")
    }

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didAdd stream: RTCMediaStream) {
        print("📡 [WebRtcBridge] Remote stream added")
    }

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didRemove stream: RTCMediaStream) {}

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didChange newState: RTCIceConnectionState) {
        print("🧊 [WebRtcBridge] ICE connection state: \(newState.rawValue)")
        switch newState {
        case .connected, .completed:
            print("🟢 [WebRtcBridge] ══ CALL CONNECTED — audio is live ══")
            DispatchQueue.main.async {
                SosNotificationHandler.shared.onCallConnected()
            }
        case .failed:
            guard isCallActive else { return }
            print("🔴 [WebRtcBridge] ICE failed — ending call")
            DispatchQueue.main.async { SosNotificationHandler.shared.onCallEnded() }
        case .disconnected:
            // Short disconnection can recover; give it 3s before giving up
            guard isCallActive else { return }
            print("⚠️ [WebRtcBridge] ICE disconnected — waiting for recovery")
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) { [weak self] in
                guard let self = self, self.isCallActive,
                      peerConnection.iceConnectionState == .disconnected else { return }
                print("🔴 [WebRtcBridge] ICE still disconnected after 3s — ending call")
                SosNotificationHandler.shared.onCallEnded()
            }
        case .closed:
            guard isCallActive else { return }
            print("🔴 [WebRtcBridge] ICE closed — ending call")
            DispatchQueue.main.async { SosNotificationHandler.shared.onCallEnded() }
        default:
            break
        }
    }

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didChange newState: RTCIceGatheringState) {
        let stateStr = ["new", "gathering", "complete"][safe: Int(newState.rawValue)] ?? "\(newState.rawValue)"
        print("🧊 [WebRtcBridge] ICE gathering: \(stateStr)")
    }

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didRemove candidates: [RTCIceCandidate]) {}

    func peerConnection(_ peerConnection: RTCPeerConnection,
                        didOpen dataChannel: RTCDataChannel) {}
}

// MARK: - Array safe subscript

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}