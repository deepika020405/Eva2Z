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
    @SerialName("chat_id")        val chat_id: Int?         = null,
    @SerialName("vehicle_number") val vehicle_number: String? = null,
    @SerialName("visitor_name")   val visitor_name: String?  = null,
    @SerialName("visitor_phone")  val visitor_phone: String? = null,
    @SerialName("last_message")   val last_message: String?  = null,
    val time: String?                                       = null
) {
    // Backwards-compat aliases so existing call sites in Quickreplyscreen keep working.
    val id: Int? get() = chat_id
    val reason: String? get() = last_message
    val created_at: String? get() = time
    // Fields no longer present in the new payload — kept as null getters for compile compatibility.
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
    @SerialName("sender_id") val sender_id: Int? = null,
    val type: String?      = null,   // "sent" (owner) | "received" (visitor)
    val time: String?      = null,   // pre-formatted display, e.g. "05:13 AM"
    val timestamp: String? = null    // ISO-8601 for ordering
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
// UI States
// ─────────────────────────────────────────────────────────────────

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