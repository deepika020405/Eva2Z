package com.developer.ev2z.ui.screens.VechicleParkingTag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────
// Request  — API expects: vehicle_type, vehicle_number, alternate_phone_1, alternate_phone_2
// ─────────────────────────────────────────────────────────────────

@Serializable
data class ParkingRequest(
    @SerialName("vehicle_type")       val vehicle_type: String,
    @SerialName("vehicle_number")     val vehicle_number: String,
    @SerialName("phone")              val phone: String,
    @SerialName("alternate_phone_1")  val alternate_phone_1: String,
    @SerialName("alternate_phone_2")  val alternate_phone_2: String? = null
)

// ─────────────────────────────────────────────────────────────────
// Response  — API returns "status": true
// ─────────────────────────────────────────────────────────────────

@Serializable
data class ParkingResponse(
    val status: Boolean             = false,
    val message: String?            = null,
    val data: ParkingData?          = null,
    val errors: ParkingFieldErrors? = null
)

// ─── Get User Details Response ────────────────────────────────
@Serializable
data class UserParkingListResponse(
    val status: Boolean          = false,
    val message: String?         = null,
    val data: List<ParkingData>? = null
)

@Serializable
data class ParkingData(
    val id: Int?                                                      = null,
    @SerialName("vehicle_type")      val vehicle_type: String?        = null,
    @SerialName("vehicle_number")    val vehicle_number: String?      = null,
    @SerialName("alternate_phone_1") val alternate_phone_1: String?   = null,
    @SerialName("alternate_phone_2") val alternate_phone_2: String?   = null,
    // Keep old field names for backward compatibility with existing API responses
    @SerialName("phone")             val phone: String?               = null,
    @SerialName("alternate_phone")   val alternate_phone: String?     = null,
    @SerialName("parked_at")         val parked_at: String?           = null,
    @SerialName("created_at")        val created_at: String?          = null,
    @SerialName("updated_at")        val updated_at: String?          = null,
    val status: String?                                               = null,
    @SerialName("user_id")           val user_id: Int?                = null
)

@Serializable
data class ParkingFieldErrors(
    @SerialName("vehicle_type")      val vehicle_type: List<String>?      = null,
    @SerialName("vehicle_number")    val vehicle_number: List<String>?    = null,
    @SerialName("alternate_phone_1") val alternate_phone_1: List<String>? = null,
    @SerialName("alternate_phone_2") val alternate_phone_2: List<String>? = null,
    // Keep old field names for backward compatibility
    @SerialName("phone")             val phone: List<String>?             = null,
    @SerialName("alternate_phone")   val alternate_phone: List<String>?   = null
)

// ─────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────

sealed class ParkingUiState {
    object Idle    : ParkingUiState()
    object Loading : ParkingUiState()
    data class Success(val response: ParkingResponse) : ParkingUiState()
    data class Error(
        val message: String,
        val fieldErrors: ParkingFieldErrors? = null
    ) : ParkingUiState()
}