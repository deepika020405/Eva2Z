package com.developer.ev2z.chat

import com.developer.ev2z.ui.screens.SOSParkingTag.ChatMessageDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

actual class ChatSocketManager actual constructor(private val serverUrl: String) : IOSChatSocketCallback {

    private var listener: ChatSocketListener? = null
    private var activeUserId: Int = 0

    actual fun connect(chatId: Int, userId: Int, token: String, listener: ChatSocketListener) {
        this.listener     = listener
        this.activeUserId = userId
        ChatSocketBridgeHolder.callback = this
        ChatSocketBridgeHolder.delegate?.connect(serverUrl, chatId, userId, token)
    }

    actual fun emitUserOnline(chatId: Int, userId: Int) {
        ChatSocketBridgeHolder.delegate?.emitUserOnline(chatId, userId)
    }

    actual fun emitUserOffline(chatId: Int, userId: Int) {
        ChatSocketBridgeHolder.delegate?.emitUserOffline(chatId, userId)
    }

    actual fun emitOwnerMessage(chatId: Int, text: String) {
        ChatSocketBridgeHolder.delegate?.emitOwnerMessage(chatId, text)
    }

    actual fun emitCloseChat(chatId: Int) {
        ChatSocketBridgeHolder.delegate?.emitCloseChat(chatId)
    }

    actual fun emitMessagesSeen(chatId: Int) {
        ChatSocketBridgeHolder.delegate?.emitMessagesSeen(chatId)
    }

    actual fun disconnect() {
        ChatSocketBridgeHolder.delegate?.disconnect()
        if (ChatSocketBridgeHolder.callback === this) {
            ChatSocketBridgeHolder.callback = null
        }
        listener = null
    }

    // IOSChatSocketCallback — invoked from Swift via the bridge

    override fun onConnected() {
        println("✅ [ChatSocket-iOS] Connected")
        listener?.onConnected()
    }

    override fun onDisconnected() {
        println("🔌 [ChatSocket-iOS] Disconnected")
        listener?.onDisconnected()
    }

    @OptIn(ExperimentalTime::class)
    override fun onNewMessage(id: Int, message: String?, type: String?, time: String?, timestamp: String?) {
        println("📨 [ChatSocket-iOS] newMessage id=$id type=$type text='${message?.take(40)}'")
        val resolvedId: Int? = when {
            id > 0  -> id
            else    -> {
                // Swift bridge should always deliver a positive id after the NSNumber fix.
                // Using a negative epoch-second fallback so the message still renders instead
                // of being silently dropped by applyServerMessage's `msg.id ?: return` guard.
                val fallback = -(kotlin.math.abs(Clock.System.now().toEpochMilliseconds() / 1000)).toInt()
                println("⚠️ [ChatSocket-iOS] id=0 from bridge — using local fallback id=$fallback (text='${message?.take(20)}')")
                fallback
            }
        }
        listener?.onNewMessage(
            ChatMessageDto(
                id        = resolvedId,
                message   = message,
                type      = type,
                time      = time,
                timestamp = timestamp
            )
        )
    }

    override fun onMessageStatus(messageId: Int, isDelivered: Boolean, isSeen: Boolean) {
        println("📬 [ChatSocket-iOS] message_status id=$messageId delivered=$isDelivered seen=$isSeen")
        listener?.onMessageStatusUpdate(messageId, isDelivered, isSeen)
    }

    override fun onVisitorOnline(eventUserId: Int) {
        println("🟢 [ChatSocket-iOS] user_online eventUserId=$eventUserId ownerUserId=$activeUserId")
        // Swift bridge already filters by role/userId before calling here; just forward.
        listener?.onVisitorOnline()
    }

    override fun onVisitorOffline(eventUserId: Int) {
        println("🔴 [ChatSocket-iOS] user_offline eventUserId=$eventUserId ownerUserId=$activeUserId")
        listener?.onVisitorOffline()
    }

    override fun onChatClosed() {
        println("🔒 [ChatSocket-iOS] chat_closed")
        listener?.onChatClosed()
    }
}