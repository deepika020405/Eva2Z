package com.developer.ev2z.chat

import com.developer.ev2z.ui.screens.SOSParkingTag.ChatMessageDto

interface ChatSocketListener {
    fun onNewMessage(msg: ChatMessageDto)
    fun onMessageStatusUpdate(messageId: Int, isDelivered: Boolean, isSeen: Boolean)
    fun onChatClosed()
    fun onConnected()
    fun onDisconnected()
    fun onVisitorOnline() {}
    fun onVisitorOffline() {}
}

expect class ChatSocketManager(serverUrl: String) {
    fun connect(chatId: Int, userId: Int, token: String, listener: ChatSocketListener)
    fun emitUserOnline(chatId: Int, userId: Int)
    fun emitUserOffline(chatId: Int, userId: Int)
    fun disconnect()
    fun emitOwnerMessage(chatId: Int, text: String)
    fun emitCloseChat(chatId: Int)
    // Emits messages_seen to the server so it can relay to the visitor in real time.
    fun emitMessagesSeen(chatId: Int)
}