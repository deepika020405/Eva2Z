package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------- DeviceResponse (Dashboard) ----------------------

@Serializable
data class DeviceResponse(
    val success: Boolean,
    @SerialName("user_id")
    val user_id: Int? = null,
    val name: String? = null,
    @SerialName("total_devices")
    val total_devices: Int? = null,
    @SerialName("running_count")
    val running_count: Int? = null,
    @SerialName("idle_count")
    val idle_count: Int? = null,
    @SerialName("parked_count")
    val parked_count: Int? = null,
    @SerialName("offline_count")
    val offline_count: Int? = null,
    val data: List<DeviceItem>? = null
)

@Serializable
data class DeviceItem(
    @SerialName("device_id")
    val device_id: String? = null,
    @SerialName("device_name")
    val device_name: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val speed: Int? = null,
    val status: String? = null,
    val timestamp: String? = null,
    @SerialName("last_seen_minutes_ago")
    val last_seen_minutes_ago: Int? = null
)


// ---------------------- MyDevicesResponse (Devices List) ----------------------

@Serializable
data class MyDevicesResponse(
    val success: Boolean,
    @SerialName("total_devices")
    val total_devices: Int,
    val devices: List<Device>,
    val username: String?
)

@Serializable
data class Device(
    val id: Int,
    @SerialName("device_id")
    val device_id: String,
    @SerialName("device_name")
    val device_name: String,
    @SerialName("sim_no")
    val sim_no: String? = null,
    @SerialName("device_type")
    val device_type: String? = null,
    @SerialName("engine_status")
    val engine_status: String,
    val status: String,
    @SerialName("created_at")
    val created_at: String
)
