package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SosNotificationHandler {

    // replay=1 for cold start, extraBufferCapacity=5 so tryEmit never drops
    private val _alertFlow = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 5)
    val alertFlow: SharedFlow<Int> = _alertFlow.asSharedFlow()

    // The most recent chat_id we've seen (from FCM or cold-start intent extra).
    // Used as the fallback chat for the manual SOS button on Home, since the backend
    // no longer exposes a "list all chats" endpoint.
    var lastChatId: Int? = null
        private set

    // Latest visitor identity pulled off the FCM payload. The /chat-messages/{chatId}
    // endpoint does not return visitor name/phone, so FCM data is our only source for
    // the chat top-bar header.
    var lastVisitorName: String? = null
        private set
    var lastVisitorPhone: String? = null
        private set

    fun onSosAlertReceived(
        alertId: Int,
        visitorName: String? = null,
        visitorPhone: String? = null
    ) {
        println("🚨 SOS ALERT RECEIVED: $alertId (visitor=$visitorName, phone=$visitorPhone)")
        lastChatId = alertId
        if (!visitorName.isNullOrBlank()) lastVisitorName = visitorName
        if (!visitorPhone.isNullOrBlank()) lastVisitorPhone = visitorPhone
        val emitted = _alertFlow.tryEmit(alertId)
        println("🚨 SOS ALERT EMITTED: $emitted")
    }

    fun clear() {
        _alertFlow.resetReplayCache()
    }
}