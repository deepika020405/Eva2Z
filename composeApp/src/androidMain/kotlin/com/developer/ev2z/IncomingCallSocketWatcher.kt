package com.developer.ev2z

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

// Lightweight Socket.IO watcher used during the ringing phase (before Accept).
// Joins the call room and listens for two server broadcasts:
//   "call-resolved" { roomId, action="accepted" } — another device accepted
//   "call-ended"    { roomId }                    — caller cancelled / another device declined
//
// Callbacks are invoked on the Socket.IO thread; callers must marshal to main thread.
class IncomingCallSocketWatcher(
    serverUrl: String,
    private val roomId: String,
    private val onAcceptedElsewhere: () -> Unit,
    private val onEnded: () -> Unit,
) {
    private val socket: Socket = IO.socket(serverUrl, IO.Options().apply {
        transports = arrayOf("websocket")
        reconnection = false
        forceNew = true
    })

    fun connect() {
        println("🔌 [CallWatcher] Connecting for room=$roomId")

        socket.on(Socket.EVENT_CONNECT) {
            println("✅ [CallWatcher] Connected — joining room=$roomId")
            socket.emit("join-room", JSONObject().put("roomId", roomId))
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            println("❌ [CallWatcher] Connection error: ${args.getOrNull(0)}")
        }

        socket.on("call-resolved") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val eventRoom = data.optString("roomId")
            val action    = data.optString("action")
            println("📡 [CallWatcher] call-resolved — room=$eventRoom action=$action")
            if (eventRoom == roomId && action == "accepted") onAcceptedElsewhere()
        }

        // "call-ended" = caller cancelled, or another device declined
        socket.on("call-ended") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val eventRoom = data.optString("roomId")
            println("📡 [CallWatcher] call-ended — room=$eventRoom")
            if (eventRoom == roomId) onEnded()
        }

        // Backend may also broadcast "end-call" on decline
        socket.on("end-call") { args ->
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val eventRoom = data.optString("roomId")
            println("📡 [CallWatcher] end-call — room=$eventRoom")
            if (eventRoom == roomId) onEnded()
        }

        socket.connect()
    }

    // Emit end-call through the already-connected socket (used when owner declines).
    // Call this before disconnect() so the message flushes first.
    fun sendEndCall() {
        println("📵 [CallWatcher] Emitting end-call (decline) for room=$roomId")
        socket.emit("end-call", JSONObject().put("roomId", roomId))
    }

    fun disconnect() {
        println("🔌 [CallWatcher] Disconnecting for room=$roomId")
        socket.off()
        socket.disconnect()
    }
}