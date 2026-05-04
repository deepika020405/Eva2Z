package com.developer.ev2z.ui.screens.SOSParkingTag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────
// Alert — backend response se match karta hai
// ─────────────────────────────────────────────────────────────────

// Backend now returns a chat-list shape (one entry per visitor thread, like WhatsApp's chat list).
// Fields: chat_id, vehicle_number, visitor_name, visitor_phone, last_message, time
@Serializable
data class SosAlert(
    @SerialName("id")             val chat_id: Int?         = null,
    @SerialName("vehicle_number") val vehicle_number: String? = null,
    @SerialName("visitor_name")   val visitor_name: String?  = null,
    @SerialName("visitor_phone")  val visitor_phone: String? = null,
    @SerialName("reason")         val last_message: String?  = null,
    val time: String?                                       = null
) {
    val id: Int? get() = chat_id
    val reason: String? get() = last_message
    val created_at: String? get() = time
    val alert_message: String? get() = null
    val vehicle_color: String? get() = null
    val vehicle_type: String? get() = null
}

// ─────────────────────────────────────────────────────────────────
// Alert List Response  — GET /visitor-notifications
// ─────────────────────────────────────────────────────────────────

@Serializable
data class SosAlertListResponse(
    val status: Boolean       = false,
    val message: String?      = null,
    val data: List<SosAlert>? = null
)

// ─────────────────────────────────────────────────────────────────
// Alert Detail Response  — GET /visitor-notifications/{id}
// ─────────────────────────────────────────────────────────────────

@Serializable
data class SosAlertDetailResponse(
    val status: Boolean  = false,
    val message: String? = null,
    val data: SosAlert?  = null
)

// ─────────────────────────────────────────────────────────────────
// Owner Reply Request — POST /owner-replies
// ─────────────────────────────────────────────────────────────────

@Serializable
data class OwnerReplyRequest(
    @SerialName("chat_id")
    val chat_id: Int,
    val message: String
)

// ─────────────────────────────────────────────────────────────────
// Per-chat messages — GET /chat-messages/{chatId}
// Actual server shape:
//   { "status": true,
//     "closed": false,
//     "data": [
//       { "id": 21, "message": "Please move your vehicle",
//         "sender_id": null,        // null = visitor, owner_user_id = owner reply
//         "type": "received",       // "received" | "sent"
//         "time": "12:06 PM",
//         "timestamp": "2026-04-09T12:06:21.000000Z" },
//       ...
//     ] }
// Note: visitor name/phone are NOT in this response.
// ─────────────────────────────────────────────────────────────────

@Serializable
data class ChatMessagesResponse(
    val status: Boolean                 = false,
    val message: String?                = null,
    val closed: Boolean                 = false,
    val data: List<ChatMessageDto>      = emptyList()
)

@Serializable
data class ChatMessageDto(
    val id: Int?           = null,
    val message: String?   = null,
    @SerialName("sender_id")    val sender_id: Int?    = null,
    val type: String?      = null,   // "sent" (owner) | "received" (visitor)
    val time: String?      = null,
    val timestamp: String? = null,
    @SerialName("is_delivered") val is_delivered: Int? = null,  // 1 = visitor received
    @SerialName("is_seen")      val is_seen: Int?      = null   // 1 = visitor read
)

// ─────────────────────────────────────────────────────────────────
// Quick Reply Request
// ─────────────────────────────────────────────────────────────────

@Serializable
data class SosReplyRequest(
    @SerialName("reply_type")    val reply_type: String,
    @SerialName("custom_message")val custom_message: String? = null
)

// ─────────────────────────────────────────────────────────────────
// Quick Reply Response
// ─────────────────────────────────────────────────────────────────

@Serializable
data class SosReplyResponse(
    val status: Boolean  = false,
    val message: String? = null,
    val data: SosAlert?  = null
)

// ─────────────────────────────────────────────────────────────────
// Initiate Call Response — POST /call/initiate
// ─────────────────────────────────────────────────────────────────

@Serializable
data class InitiateCallResponse(
    val success: Boolean  = false,
    val room_id: String?  = null,
    val message: String?  = null
)

// ─────────────────────────────────────────────────────────────────
// Alert Type Enum — reason field se map karo
// ─────────────────────────────────────────────────────────────────

enum class AlertType(val key: String, val displayLabel: String, val emoji: String) {
    VEHICLE_BLOCKING("Vehicle Blocking", "Vehicle Blocking", "🚫"),
    LIGHTS_ON       ("Lights Left On",   "Lights Left On",  "💡"),
    EMERGENCY       ("Emergency",        "Emergency",       "🆘"),
    OTHER           ("other",            "Other Issue",     "💬");

    companion object {
        fun from(key: String?) = values().firstOrNull {
            it.key.equals(key, ignoreCase = true)
        } ?: OTHER
    }
}

// ─────────────────────────────────────────────────────────────────
// Quick Reply Options
// ─────────────────────────────────────────────────────────────────

enum class QuickReplyOption(
    val key: String,
    val title: String,
    val subtitle: String
) {
    COMING_SOON("coming_soon", "Coming in 2 minutes",     "Owner is on the way"),
    THANKS("thanks",  "Thanks for Informing me ",      "Thanking You"),
    URGENT("urgent",  "Is it Urgent? ",      "Owner is asked for urgency"),
    SORRY      ("sorry",       "Sorry for inconvenience", "Apologize for the block"),
    CUSTOM     ("custom",      "Send Custom Message",     "Type your own response")
}

// ─────────────────────────────────────────────────────────────────
// /my-chats-with-messages  — full thread list with embedded messages
// ─────────────────────────────────────────────────────────────────

@Serializable
data class MyChatsResponse(
    val status: Boolean                  = false,
    val user: ChatUserInfo?              = null,
    @SerialName("total_chats") val total_chats: Int = 0,
    val data: List<ChatThread>           = emptyList()
)

@Serializable
data class ChatUserInfo(
    val id: Int    = 0,
    val name: String = ""
)

@Serializable
data class ChatThread(
    @SerialName("chat_id")         val chat_id: Int          = 0,
    @SerialName("vehicle_id")      val vehicle_id: Int?      = null,
    val visitor: VisitorInfo?                                 = null,
    val status: String?                                       = null,
    @SerialName("last_message_at") val last_message_at: String? = null,
    @SerialName("total_messages")  val total_messages: Int   = 0,
    @SerialName("last_message")    val last_message: ChatMessageItem? = null,
    val messages: List<ChatMessageItem>                       = emptyList()
) {
    val isOpen: Boolean get() = status.equals("open", ignoreCase = true)
}

@Serializable
data class VisitorInfo(
    val name: String?  = null,
    val phone: String? = null
)

@Serializable
data class ChatMessageItem(
    val id: Int?           = null,
    val text: String?      = null,
    @SerialName("sender_id")     val sender_id: Int?     = null,
    val type: String?      = null,
    val time: String?      = null,
    val timestamp: String? = null,
    @SerialName("is_delivered")  val is_delivered: Int?  = null,
    @SerialName("is_seen")       val is_seen: Int?       = null
) {
    fun toDto() = ChatMessageDto(
        id           = id,
        message      = text,
        sender_id    = sender_id,
        type         = type,
        time         = time,
        timestamp    = timestamp,
        is_delivered = is_delivered,
        is_seen      = is_seen
    )
}

// ─────────────────────────────────────────────────────────────────
// UI States
// ─────────────────────────────────────────────────────────────────

sealed class ChatThreadListUiState {
    object Idle                                           : ChatThreadListUiState()
    object Loading                                        : ChatThreadListUiState()
    data class Success(val threads: List<ChatThread>)     : ChatThreadListUiState()
    data class Error(val message: String)                 : ChatThreadListUiState()
}

sealed class SosAlertUiState {
    object Idle                                 : SosAlertUiState()
    object Loading                              : SosAlertUiState()
    data class Success(val alert: SosAlert)     : SosAlertUiState()
    data class Error(val message: String)       : SosAlertUiState()
}

sealed class SosReplyUiState {
    object Idle                                 : SosReplyUiState()
    object Loading                              : SosReplyUiState()
    data class Success(val message: String)     : SosReplyUiState()
    data class Error(val message: String)       : SosReplyUiState()
}

sealed class SosAlertListUiState {
    object Idle                                         : SosAlertListUiState()
    object Loading                                      : SosAlertListUiState()
    data class Success(val alerts: List<SosAlert>)      : SosAlertListUiState()
    data class Error(val message: String)               : SosAlertListUiState()
}