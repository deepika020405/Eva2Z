package com.developer.ev2z.data.local.model



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceResponse(
    val success: Boolean,
    @SerialName("user_id") val userId: Int,
    val name: String,
    @SerialName("total_devices") val totalDevices: Int,
    @SerialName("running_count") val runningCount: Int,
    @SerialName("idle_count") val idleCount: Int,
    @SerialName("parked_count") val parkedCount: Int,
    @SerialName("offline_count") val offlineCount: Int,
    val data: List<Device>
)

@Serializable
data class Device(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String?,
    val latitude: String?,
    val longitude: String?,
    val speed: Int,
    val status: String, // "moving", "idle", "offline", "no_data"
    val timestamp: String?,
    @SerialName("last_seen_minutes_ago") val lastSeenMinutesAgo: Int?
) {
    fun getDisplayName(): String = deviceName ?: "Device ${deviceId.take(8)}"

    fun getLatitudeDouble(): Double? = latitude?.toDoubleOrNull()

    fun getLongitudeDouble(): Double? = longitude?.toDoubleOrNull()

    fun hasValidLocation(): Boolean = getLatitudeDouble() != null && getLongitudeDouble() != null

    fun getStatusDisplay(): String = when(status) {
        "moving" -> "Running"
        "idle" -> "Idle"
        "offline" -> "Offline"
        "no_data" -> "No Data"
        else -> status.capitalize()
    }

    fun getStatusColor(): Long = when(status) {
        "moving" -> 0xFF4CAF50  // Green
        "idle" -> 0xFFFFC107    // Yellow
        "offline" -> 0xFFBDBDBD // Gray
        "no_data" -> 0xFF9E9E9E // Light Gray
        else -> 0xFF9E9E9E
    }
}