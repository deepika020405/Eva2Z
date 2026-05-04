package com.developer.ev2z.chat

interface IOSChatSocketDelegate {
    fun connect(serverUrl: String, chatId: Int, userId: Int, token: String)
    fun emitUserOnline(chatId: Int, userId: Int)
    fun emitUserOffline(chatId: Int, userId: Int)
    fun emitOwnerMessage(chatId: Int, text: String)
    fun emitCloseChat(chatId: Int)
    fun emitMessagesSeen(chatId: Int)
    fun disconnect()
}

interface IOSChatSocketCallback {
    fun onConnected()
    fun onDisconnected()
    fun onNewMessage(id: Int, message: String?, type: String?, time: String?, timestamp: String?)
    fun onMessageStatus(messageId: Int, isDelivered: Boolean, isSeen: Boolean)
    fun onVisitorOnline(eventUserId: Int)
    fun onVisitorOffline(eventUserId: Int)
    fun onChatClosed()
}

object ChatSocketBridgeHolder {
    var delegate: IOSChatSocketDelegate? = null
    var callback: IOSChatSocketCallback? = null
}