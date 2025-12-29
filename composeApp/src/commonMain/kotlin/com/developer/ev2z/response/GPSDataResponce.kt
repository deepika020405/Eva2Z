package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveLocationResponse(
    val success: Boolean,
    val data: LocationData? = null,
    val error: String? = null
)

@Serializable
data class LocationData(
    @SerialName("latest_location")
    val latest_location: LatestLocation? = null
)

@Serializable
data class LatestLocation(
    @SerialName("device_id")
    val device_id: String,
    val status: String,
    val battery: String? = null,
    @SerialName("battery_display")
    val battery_display: String? = null,
    @SerialName("battery_status")
    val battery_status: String? = null,
    val speed: String? = null,
    val latitude: String,
    val longitude: String,
    val timestamp: String
)