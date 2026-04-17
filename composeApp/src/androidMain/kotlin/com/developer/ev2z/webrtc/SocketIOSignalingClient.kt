package com.developer.ev2z.webrtc

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

// Socket.IO signaling layer for WebRTC peer negotiation.
//
// Server-side events (must match your backend exactly):
//   EMIT  join-room  { roomId }                              → join a signaling room
//   RECV  user-joined                                        → other peer entered the room
//   EMIT  offer      { sdp, type, roomId }                   → send SDP offer (caller)
//   EMIT  answer     { sdp, type, roomId }                   → send SDP answer (callee)
//   EMIT  candidate  { sdpMid, sdpMLineIndex, candidate, roomId } → ICE candidate
//   EMIT  end-call   { roomId }                              → hang up
//   RECV  offer / answer / candidate / end-call              → received from remote peer
class SocketIOSignalingClient(private val serverUrl: String) {

    interface Listener {
        fun onRemoteOfferReceived(sdp: SessionDescription)
        fun onRemoteAnswerReceived(sdp: SessionDescription)
        fun onRemoteIceCandidateReceived(candidate: IceCandidate)
        fun onUserJoined()
        fun onCallEnded()
    }

    private val socket: Socket = IO.socket(serverUrl, IO.Options().apply {
        transports = arrayOf("websocket")   // skip XHR polling — force WebSocket directly
        reconnection = true
        reconnectionAttempts = 5
        reconnectionDelay = 1000

        forceNew = true
    })

    // Stored so every send-log can include which room the event went to
    private var activeRoomId: String = ""

    fun connect(roomId: String, listener: Listener) {
        activeRoomId = roomId
        println("📡 [Socket.IO] Connecting to $serverUrl (WebSocket) for room=$roomId")

        socket.on(Socket.EVENT_CONNECT) {
            println("✅ [Socket.IO] Connected — socket.id=${socket.id()} | emitting join-room {roomId=$roomId}")
            socket.emit("join-room", JSONObject().put("roomId", roomId))
            println("📤 [Socket.IO] join-room emitted for roomId=$roomId")
        }
        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            println("❌ [Socket.IO] Connection error: ${args.getOrNull(0)}")
        }
        socket.on(Socket.EVENT_DISCONNECT) { args ->
            println("🔌 [Socket.IO] Disconnected — reason=${args.getOrNull(0)}")
        }
        socket.on("user-joined") { args ->
            println("👤 [Socket.IO] user-joined event received — args=${args.toList()}")
            listener.onUserJoined()
        }
        socket.on("offer") { args ->
            println("📨 [Socket.IO] offer received — args=${args.size}")
            (args.getOrNull(0) as? JSONObject)?.runCatching {
                // Server sends: { roomId, offer: { sdp, type } }
                val offerObj = optJSONObject("offer") ?: this
                val sdp = offerObj.getString("sdp")
                println("✅ [Socket.IO] offer SDP parsed — length=${sdp.length}")
                listener.onRemoteOfferReceived(
                    SessionDescription(SessionDescription.Type.OFFER, sdp)
                )
            }?.onFailure { println("❌ [Socket.IO] Failed to parse offer: ${it.message}  raw=${args.getOrNull(0)}") }
        }
        socket.on("answer") { args ->
            println("📨 [Socket.IO] answer received — args=${args.size}")
            (args.getOrNull(0) as? JSONObject)?.runCatching {
                // Server sends: { roomId, answer: { sdp, type } }
                val answerObj = optJSONObject("answer") ?: this
                val sdp = answerObj.getString("sdp")
                println("✅ [Socket.IO] answer SDP parsed — length=${sdp.length}")
                listener.onRemoteAnswerReceived(
                    SessionDescription(SessionDescription.Type.ANSWER, sdp)
                )
            }?.onFailure { println("❌ [Socket.IO] Failed to parse answer: ${it.message}  raw=${args.getOrNull(0)}") }
        }
        socket.on("ice-candidate") { args ->
            println("🧊 [Socket.IO] ice-candidate received — args=${args.size}")
            (args.getOrNull(0) as? JSONObject)?.runCatching {
                // Server sends: { roomId, candidate: { candidate, sdpMid, sdpMLineIndex } }
                val candidateObj = optJSONObject("candidate") ?: this
                val mid = candidateObj.getString("sdpMid")
                val idx = candidateObj.getInt("sdpMLineIndex")
                val sdp = candidateObj.getString("candidate")
                println("🧊 [Socket.IO] ICE candidate parsed — sdpMid=$mid idx=$idx")
                listener.onRemoteIceCandidateReceived(IceCandidate(mid, idx, sdp))
            }?.onFailure { println("❌ [Socket.IO] Failed to parse ice-candidate: ${it.message}  raw=${args.getOrNull(0)}") }
        }
        socket.on("end-call") {
            println("📵 [Socket.IO] end-call event received — remote peer ended the call")
            listener.onCallEnded()
        }

        println("📡 [Socket.IO] socket.connect() called")
        socket.connect()
    }

    fun sendOffer(sdp: SessionDescription) {
        val sdpPreview = sdp.description.take(120).replace("\n", " ")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📤 [Socket.IO] EMIT offer → room=$activeRoomId")
        println("   SDP type   : ${sdp.type}")
        println("   SDP preview: $sdpPreview…")
        println("   Socket id  : ${socket.id()}")
        println("   Connected  : ${socket.connected()}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        socket.emit("offer", JSONObject()
            .put("sdp", sdp.description)
            .put("type", "offer")
            .put("roomId", activeRoomId))
    }

    fun sendAnswer(sdp: SessionDescription) {
        val sdpPreview = sdp.description.take(120).replace("\n", " ")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("📤 [Socket.IO] EMIT answer → room=$activeRoomId")
        println("   SDP type   : ${sdp.type}")
        println("   SDP preview: $sdpPreview…")
        println("   Socket id  : ${socket.id()}")
        println("   Connected  : ${socket.connected()}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        socket.emit("answer", JSONObject()
            .put("sdp", sdp.description)
            .put("type", "answer")
            .put("roomId", activeRoomId))
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        println("🧊 [Socket.IO] EMIT ice-candidate → room=$activeRoomId  sdpMid=${candidate.sdpMid} idx=${candidate.sdpMLineIndex}")
        // Server expects: { roomId, candidate: { candidate, sdpMid, sdpMLineIndex } }
        socket.emit("ice-candidate", JSONObject()
            .put("roomId", activeRoomId)
            .put("candidate", JSONObject()
                .put("candidate", candidate.sdp)
                .put("sdpMid", candidate.sdpMid)
                .put("sdpMLineIndex", candidate.sdpMLineIndex)))
    }

    fun sendEnd() {
        println("📤 [Socket.IO] EMIT end-call → room=$activeRoomId")
        socket.emit("end-call", JSONObject().put("roomId", activeRoomId))
    }

    fun disconnect() {
        println("🔌 [Socket.IO] Disconnecting socket")
        socket.off()
        socket.disconnect()
    }
}