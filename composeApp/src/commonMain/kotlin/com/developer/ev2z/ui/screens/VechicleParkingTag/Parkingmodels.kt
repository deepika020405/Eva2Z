package com.developer.ev2z.ui.screens.VechicleParkingTag

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────────
// Request
// ─────────────────────────────────────────────────────────────────

@Serializable
data class ParkingRequest(
    @SerialName("vehicle_type")   val vehicle_type: String,
    @SerialName("vehicle_number") val vehicle_number: String
)

// ─────────────────────────────────────────────────────────────────
// Response  — API returns "status": true  (NOT "success")
// ─────────────────────────────────────────────────────────────────

@Serializable
data class ParkingResponse(
    val status: Boolean              = false,   // ✅ FIXED: was "success"
    val message: String?             = null,
    val data: ParkingData?           = null,
    val errors: ParkingFieldErrors?  = null
)

@Serializable
data class ParkingData(
    val id: Int?                               = null,
    @SerialName("vehicle_type")   val vehicle_type: String?   = null,
    @SerialName("vehicle_number") val vehicle_number: String? = null,
    @SerialName("parked_at")      val parked_at: String?      = null,
    @SerialName("created_at")     val created_at: String?     = null,
    @SerialName("updated_at")     val updated_at: String?     = null,
    val status: String?                        = null,
    @SerialName("user_id")        val user_id: Int?           = null
)

@Serializable
data class ParkingFieldErrors(
    @SerialName("vehicle_type")   val vehicle_type: List<String>?   = null,
    @SerialName("vehicle_number") val vehicle_number: List<String>? = null
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