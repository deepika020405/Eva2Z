package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Data carried by every incoming-call event (FCM → HomeNavGraph)
data class IncomingCallData(
    val visitorName:  String,
    val visitorPhone: String,
    val roomId:       String,
    val chatId:       Int?
)

object SosNotificationHandler {

    // replay=1 for cold start, extraBufferCapacity=5 so tryEmit never drops
    private val _alertFlow = MutableSharedFlow<Int>(replay = 1, extraBufferCapacity = 5)
    val alertFlow: SharedFlow<Int> = _alertFlow.asSharedFlow()

    // Incoming-call flow
    private val _callFlow = MutableSharedFlow<IncomingCallData>(replay = 1, extraBufferCapacity = 5)
    val callFlow: SharedFlow<IncomingCallData> = _callFlow.asSharedFlow()

    // The most recent chat_id we've seen (from FCM or cold-start intent extra).
    var lastChatId: Int? = null
        private set

    // Latest visitor identity pulled off the FCM payload.
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

    fun onIncomingCallReceived(
        visitorName:  String? = null,
        visitorPhone: String? = null,
        roomId:       String  = "",
        chatId:       Int?    = null
    ) {
        val name  = visitorName?.takeIf { it.isNotBlank() } ?: "Visitor"
        val phone = visitorPhone ?: ""
        if (!visitorName.isNullOrBlank())  lastVisitorName  = visitorName
        if (!visitorPhone.isNullOrBlank()) lastVisitorPhone = visitorPhone
        chatId?.let { lastChatId = it }
        println("📞 INCOMING CALL: name=$name phone=$phone roomId=$roomId")
        _callFlow.tryEmit(IncomingCallData(name, phone, roomId, chatId))
    }

    // Call-ended signal — emitted when the caller cancels before the owner picks up.
    // No replay needed: if the screen isn't open when this fires, ignore it.
    private val _callEndedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val callEndedFlow: SharedFlow<Unit> = _callEndedFlow.asSharedFlow()

    fun onCallEnded() {
        println("📵 CALL ENDED by caller")
        clearCallFlow()   // discard any buffered ringing event
        _callEndedFlow.tryEmit(Unit)
    }

    // Emitted when another device (same account) has already accepted this call.
    private val _callAcceptedElsewhereFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val callAcceptedElsewhereFlow: SharedFlow<Unit> = _callAcceptedElsewhereFlow.asSharedFlow()

    fun onCallAcceptedElsewhere() {
        println("📱 CALL ACCEPTED on another device — dismissing ringing screen")
        clearCallFlow()
        _callAcceptedElsewhereFlow.tryEmit(Unit)
    }

    fun clear() {
        _alertFlow.resetReplayCache()
    }

    fun clearCallFlow() {
        _callFlow.resetReplayCache()
    }
}