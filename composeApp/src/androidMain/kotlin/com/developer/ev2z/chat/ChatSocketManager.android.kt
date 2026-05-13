package com.developer.ev2z.chat

import com.developer.ev2z.ui.screens.SOSParkingTag.ChatMessageDto
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

actual class ChatSocketManager actual constructor(private val serverUrl: String) {

    private var socket: Socket? = null

    actual fun connect(chatId: Int, userId: Int, token: String, listener: ChatSocketListener) {
        disconnect()

        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 2000
                reconnectionDelayMax = 10000
                timeout = 10000
                forceNew = true
                auth = mapOf("token" to token)
            }

            socket = IO.socket(serverUrl, options).apply {
                on(Socket.EVENT_CONNECT) {
                    println("✅ [ChatSocket] Connected to $serverUrl — joining chatId=$chatId")
                    // Server contract: event=joinChat, data=chatId (integer)
                    emit("joinChat", chatId)
                    emitUserOnline(chatId, userId)
                    listener.onConnected()
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    println("❌ [ChatSocket] Connect error: ${args.getOrNull(0)}")
                    listener.onDisconnected()
                }

                on(Socket.EVENT_DISCONNECT) { args ->
                    println("🔌 [ChatSocket] Disconnected — reason=${args.getOrNull(0)}")
                    listener.onDisconnected()
                }

                // Server emits this when a visitor sends a new message to the owner.
                // Event name is "newMessage" (camelCase) — do NOT change to "new_message".
                on("newMessage") { args ->
                    try {
                        val data = args.getOrNull(0) as? JSONObject ?: return@on
                        val msgObj = data.optJSONObject("message") ?: data
                        val dto = ChatMessageDto(
                            id        = msgObj.optInt("id").takeIf { it != 0 },
                            message   = msgObj.optString("text").ifBlank { null }
                                ?: msgObj.optString("message").ifBlank { null },
                            type      = msgObj.optString("type").ifBlank { null },
                            time      = msgObj.optString("time").ifBlank { null },
                            timestamp = msgObj.optString("timestamp").ifBlank { null }
                        )
                        println("📨 [ChatSocket] newMessage — id=${dto.id} type=${dto.type} text='${dto.message?.take(40)}'")
                        // System message = visitor ended the chat from their side
                        if (dto.type.equals("system", ignoreCase = true)) {
                            println("🔒 [ChatSocket] system newMessage → onChatClosed")
                            listener.onNewMessage(dto)
                            listener.onChatClosed()
                            return@on
                        }
                        listener.onNewMessage(dto)
                    } catch (e: Exception) {
                        println("❌ [ChatSocket] Failed to parse newMessage: ${e.message}  raw=${args.getOrNull(0)}")
                    }
                }

                // Server emits when visitor receives or reads owner messages.
                // Handles both singular message_id (int) and bulk message_ids (array).
                on("message_status") { args ->
                    try {
                        val data = args.getOrNull(0) as? JSONObject ?: return@on
                        val isDelivered = data.optInt("is_delivered") == 1
                        val isSeen      = data.optInt("is_seen") == 1
                        // Singular: { message_id, is_delivered, is_seen }
                        val singularId = data.optInt("message_id")
                        if (singularId > 0) {
                            println("📬 [ChatSocket] message_status — id=$singularId delivered=$isDelivered seen=$isSeen")
                            listener.onMessageStatusUpdate(singularId, isDelivered, isSeen)
                        }
                        // Bulk: { message_ids: [...], is_delivered, is_seen }
                        val ids = data.optJSONArray("message_ids")
                        if (ids != null) {
                            for (i in 0 until ids.length()) {
                                val id = ids.optInt(i)
                                if (id > 0) {
                                    println("📬 [ChatSocket] message_status (bulk) — id=$id delivered=$isDelivered seen=$isSeen")
                                    listener.onMessageStatusUpdate(id, isDelivered, isSeen)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ [ChatSocket] Failed to parse message_status: ${e.message}")
                    }
                }

                // Fired when owner marks all messages in the chat as seen (bulk seen event).
                // Backend PHP posts to Node.js which may emit this to the room.
                on("messages_seen") { args ->
                    try {
                        val data = args.getOrNull(0) as? JSONObject ?: return@on
                        val isDelivered = data.optInt("is_delivered", 1) == 1
                        val ids = data.optJSONArray("message_ids")
                        if (ids != null) {
                            for (i in 0 until ids.length()) {
                                val id = ids.optInt(i)
                                if (id > 0) {
                                    println("📬 [ChatSocket] messages_seen — id=$id seen=true")
                                    listener.onMessageStatusUpdate(id, isDelivered, true)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ [ChatSocket] Failed to parse messages_seen: ${e.message}")
                    }
                }

                on("user_online") { args ->
                    val data = args.getOrNull(0) as? JSONObject ?: return@on
                    val eventUserId = data.optInt("user_id", 0)
                    val role        = data.optString("role", "")
                    println("🟢 [ChatSocket] user_online received — eventUserId=$eventUserId ownerUserId=$userId role=$role raw=$data")
                    // Accept if explicitly a different user OR if role=visitor (website may omit user_id)
                    val isDifferentUser = eventUserId > 0 && eventUserId != userId
                    val isVisitorRole   = role.equals("visitor", ignoreCase = true)
                    if (isDifferentUser || isVisitorRole) {
                        println("🟢 [ChatSocket] visitor_online confirmed — chatId=$chatId")
                        listener.onVisitorOnline()
                    }
                }

                on("user_offline") { args ->
                    val data = args.getOrNull(0) as? JSONObject ?: return@on
                    val eventUserId = data.optInt("user_id", 0)
                    val role        = data.optString("role", "")
                    println("🔴 [ChatSocket] user_offline received — eventUserId=$eventUserId ownerUserId=$userId role=$role raw=$data")
                    val isDifferentUser = eventUserId > 0 && eventUserId != userId
                    val isVisitorRole   = role.equals("visitor", ignoreCase = true)
                    if (isDifferentUser || isVisitorRole) {
                        println("🔴 [ChatSocket] visitor_offline confirmed — chatId=$chatId")
                        listener.onVisitorOffline()
                    }
                }

                on("chat_closed") { args ->
                    try {
                        val data       = args.getOrNull(0) as? JSONObject
                        val eventChatId = data?.optInt("chat_id", 0) ?: 0
                        val closedBy   = data?.optString("closed_by") ?: "unknown"
                        println("🔒 [ChatSocket] chat_closed — eventChatId=$eventChatId activeChatId=$chatId closedBy=$closedBy")
                        // Only act if this event belongs to the room we joined
                        if (eventChatId == 0 || eventChatId == chatId) {
                            listener.onChatClosed()
                        }
                    } catch (e: Exception) {
                        println("❌ [ChatSocket] Failed to parse chat_closed: ${e.message}")
                        listener.onChatClosed()   // fallback: treat as closed
                    }
                }

                connect()
            }

            println("📡 [ChatSocket] socket.connect() called for chatId=$chatId userId=$userId")
        } catch (e: Exception) {
            println("❌ [ChatSocket] Failed to initialise socket: ${e.message}")
        }
    }

    actual fun emitUserOnline(chatId: Int, userId: Int) {
        val s = socket ?: return
        try {
            s.emit("user_online", JSONObject()
                .put("chat_id", chatId)
                .put("user_id", userId)
                .put("role", "owner"))
            println("🟢 [ChatSocket] user_online — chatId=$chatId userId=$userId")
        } catch (e: Exception) {
            println("❌ [ChatSocket] emitUserOnline failed: ${e.message}")
        }
    }

    actual fun emitUserOffline(chatId: Int, userId: Int) {
        val s = socket ?: return
        try {
            s.emit("user_offline", JSONObject()
                .put("chat_id", chatId)
                .put("user_id", userId)
                .put("role", "owner"))
            println("🔴 [ChatSocket] user_offline — chatId=$chatId userId=$userId")
        } catch (e: Exception) {
            println("❌ [ChatSocket] emitUserOffline failed: ${e.message}")
        }
    }

    actual fun emitCloseChat(chatId: Int) {
        val s = socket ?: return
        try {
            s.emit("close_chat", JSONObject().put("chat_id", chatId))
            println("📤 [ChatSocket] close_chat emitted — chatId=$chatId")
        } catch (e: Exception) {
            println("❌ [ChatSocket] emitCloseChat failed: ${e.message}")
        }
    }

    actual fun emitOwnerMessage(chatId: Int, text: String) {
        val s = socket ?: return
        try {
            val payload = JSONObject()
                .put("chat_id", chatId)
                .put("message", text)
            s.emit("send_message", payload)
            println("📤 [ChatSocket] send_message emitted — chatId=$chatId text='${text.take(40)}'")
        } catch (e: Exception) {
            println("❌ [ChatSocket] emitOwnerMessage failed: ${e.message}")
        }
    }

    actual fun emitMessagesSeen(chatId: Int) {
        val s = socket ?: return
        try {
            s.emit("messages_seen", JSONObject().put("chat_id", chatId))
            println("👁️ [ChatSocket] messages_seen emitted — chatId=$chatId")
        } catch (e: Exception) {
            println("❌ [ChatSocket] emitMessagesSeen failed: ${e.message}")
        }
    }

    actual fun disconnect() {
        socket?.run {
            try {
                emit("leave_chat", JSONObject().put("chat_id", 0))
            } catch (_: Exception) { /* best-effort leave */ }
            off()
            disconnect()
        }
        socket = null
        println("🔌 [ChatSocket] Socket cleaned up")
    }
}