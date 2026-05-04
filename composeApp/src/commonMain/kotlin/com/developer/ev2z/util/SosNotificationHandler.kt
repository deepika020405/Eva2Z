package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.TimeSource

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

    // Dedup guard — ignore repeated onIncomingCallReceived for the same roomId until
    // the call ends / is accepted / is declined.
    private var activeRoomId: String = ""

    // After a call is accepted or ended, remember the roomId for 60 s so that any
    // stale FCM notification tap (or Firebase auto-notification) cannot re-open the
    // incoming-call screen for the same call.
    private var lastHandledRoomId: String               = ""
    private var lastHandledMark:  TimeSource.Monotonic.ValueTimeMark? = null
    private const val HANDLED_COOLDOWN_MS               = 60_000L

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
        if (roomId.isNotBlank() && roomId == activeRoomId) {
            println("⚠️ Duplicate incoming_call for roomId=$roomId — ignoring (already ringing)")
            return
        }
        // Reject any re-trigger for a roomId that was already accepted/ended recently.
        // This prevents stale notification taps (Firebase auto-notification persists after
        // the call ends) from reopening IncomingCallScreen for the same call.
        if (roomId.isNotBlank() && roomId == lastHandledRoomId && isInCooldown()) {
            println("⚠️ Skipping already-handled roomId=$roomId (within 60 s cooldown)")
            return
        }
        if (roomId.isNotBlank()) activeRoomId = roomId
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

    // Emitted when WebRTC ICE connection is fully established (audio is live)
    private val _callConnectedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val callConnectedFlow: SharedFlow<Unit> = _callConnectedFlow.asSharedFlow()

    fun onCallConnected() {
        println("🟢 CALL CONNECTED — audio live")
        _callConnectedFlow.tryEmit(Unit)
    }

    // Emitted when the visitor (callee) joins the signaling room — ringing → connecting transition
    private val _calleeJoinedFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val calleeJoinedFlow: SharedFlow<Unit> = _calleeJoinedFlow.asSharedFlow()

    fun onCalleeJoined() {
        println("📲 CALLEE JOINED ROOM — transitioning ringing → connecting")
        _calleeJoinedFlow.tryEmit(Unit)
    }

    fun onCallEnded() {
        println("📵 CALL ENDED by caller")
        markHandled(activeRoomId)
        activeRoomId = ""
        clearCallFlow()
        _callEndedFlow.tryEmit(Unit)
    }

    // Emitted when another device (same account) has already accepted this call.
    private val _callAcceptedElsewhereFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val callAcceptedElsewhereFlow: SharedFlow<Unit> = _callAcceptedElsewhereFlow.asSharedFlow()

    fun onCallAcceptedElsewhere() {
        println("📱 CALL ACCEPTED on another device — dismissing ringing screen")
        markHandled(activeRoomId)
        activeRoomId = ""
        clearCallFlow()
        _callAcceptedElsewhereFlow.tryEmit(Unit)
    }

    // Emitted by IncomingCallActivity when owner accepts call from lock screen / killed state.
    // replay=1 so HomeNavGraph can consume it even after a short boot delay.
    private val _callAcceptedByOwnerFlow = MutableSharedFlow<IncomingCallData>(replay = 1, extraBufferCapacity = 1)
    val callAcceptedByOwnerFlow: SharedFlow<IncomingCallData> = _callAcceptedByOwnerFlow.asSharedFlow()

    fun onCallAcceptedByOwner(data: IncomingCallData) {
        println("✅ CALL ACCEPTED BY OWNER from lock screen — roomId=${data.roomId}")
        markHandled(data.roomId)
        activeRoomId = ""
        clearCallFlow()
        _callAcceptedByOwnerFlow.tryEmit(data)
    }

    // Convenience overload for iOS CallKit delegate (Swift can't easily construct IncomingCallData)
    fun onCallAcceptedByOwner(
        visitorName:  String,
        visitorPhone: String,
        roomId:       String,
        chatId:       Int?   = null
    ) {
        onCallAcceptedByOwner(IncomingCallData(visitorName, visitorPhone, roomId, chatId))
    }

    fun clearAcceptedByOwnerFlow() {
        _callAcceptedByOwnerFlow.resetReplayCache()
    }

    fun clear() {
        _alertFlow.resetReplayCache()
    }

    fun clearCallFlow() {
        // Reset replay cache only — do NOT clear activeRoomId here.
        // HomeNavGraph calls clearCallFlow() right after showing IncomingCallScreen;
        // clearing activeRoomId here would let the same roomId be re-processed if
        // a stale notification is tapped before the call finishes.
        // activeRoomId is cleared by onCallEnded / onCallAcceptedByOwner / onCallAcceptedElsewhere.
        _callFlow.resetReplayCache()
    }

    // Emitted by MainActivity.handleSosIntent when it takes over call handling.
    // IncomingCallActivity collects this and finishes itself (without calling declineCall),
    // preventing its 30-second auto-decline timer from killing an already-accepted call.
    private val _mainActivityTookOverFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val mainActivityTookOverFlow: SharedFlow<Unit> = _mainActivityTookOverFlow.asSharedFlow()

    fun onMainActivityTookOver() {
        _mainActivityTookOverFlow.tryEmit(Unit)
    }

    // ── Internal helpers ─────────────────────────────────────────────────
    private fun markHandled(roomId: String) {
        if (roomId.isNotBlank()) {
            lastHandledRoomId = roomId
            lastHandledMark   = TimeSource.Monotonic.markNow()
        }
    }

    private fun isInCooldown(): Boolean {
        val mark = lastHandledMark ?: return false
        return mark.elapsedNow().inWholeMilliseconds < HANDLED_COOLDOWN_MS
    }
}