package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteHistoryResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("terminal_id") val terminalId: String,

    @SerialName("count")
    val count: Int? = null,

    @SerialName("average_speed")
    val averageSpeed: Double? = 0.0,

    @SerialName("data")
    val data: List<RoutePoint>
)

@Serializable
data class RoutePoint(
    @SerialName("latitude") val latitude: String,
    @SerialName("longitude") val longitude: String,
    @SerialName("speed") val speed: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("movement") val movement: String
) {
    fun getLatDouble(): Double? = latitude.toDoubleOrNull()
    fun getLngDouble(): Double? = longitude.toDoubleOrNull()
    fun getSpeedInt(): Int = speed?.toDoubleOrNull()?.toInt() ?: 0

    fun isValid(): Boolean {
        val lat = getLatDouble() ?: return false
        val lng = getLngDouble() ?: return false
        return lat != 0.0 && lng != 0.0 &&
                lat in -90.0..90.0 &&
                lng in -180.0..180.0
    }
}