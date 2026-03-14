package com.developer.ev2z.ui.screens.SOSParkingTag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────
// Alert — backend response se match karta hai
// ─────────────────────────────────────────────────────────────────

@Serializable
data class SosAlert(
    val id: Int?                                     = null,
    val status: String?                              = null,

    // ✅ Backend actual fields
    @SerialName("vehicle_id")    val vehicle_id: Int?       = null,
    @SerialName("owner_id")      val owner_id: Int?         = null,
    @SerialName("visitor_name")  val visitor_name: String?  = null,
    @SerialName("visitor_phone") val visitor_phone: String? = null,
    val reason: String?                              = null,
    @SerialName("created_at")    val created_at: String?    = null,
    @SerialName("updated_at")    val updated_at: String?    = null,

    // Legacy / future fields — null aayenge abhi
    @SerialName("tag_id")        val tag_id: String?        = null,
    @SerialName("alert_type")    val alert_type: String?    = null,
    @SerialName("alert_message") val alert_message: String? = null,
    @SerialName("reported_at")   val reported_at: String?   = null,
    @SerialName("vehicle_number")val vehicle_number: String? = null,
    @SerialName("vehicle_type")  val vehicle_type: String?  = null,
    @SerialName("vehicle_color") val vehicle_color: String? = null,
    @SerialName("vehicle_image") val vehicle_image: String? = null,
    @SerialName("reporter_name") val reporter_name: String? = null,
    @SerialName("user_id")       val user_id: Int?          = null
)

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
    @SerialName("notification_id")
    val notification_id: Int,
    val message: String
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
    MOVING_NOW ("moving_now",  "Moving vehicle now",      "Owner is removing the vehicle"),
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