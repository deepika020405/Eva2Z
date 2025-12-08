package com.developer.ev2z.response

import kotlinx.serialization.Serializable

data class DeviceResponse(
    val success: Boolean,
    val user_id: Int?,
    val name: String?,
    val total_devices: Int?,
    val running_count: Int?,
    val idle_count: Int?,
    val parked_count: Int?,
    val offline_count: Int?,
    val data: List<DeviceItem>?
)
data class DeviceItem(
    val device_id: String?,
    val device_name: String?,
    val latitude: String?,
    val longitude: String?,
    val speed: Int?,
    val status: String?,
    val timestamp: String?,
    val last_seen_minutes_ago: Int?
)

@Serializable
data class MyDevicesResponse(
    val success: Boolean,
    val total_devices: Int,
    val devices: List<Device>
)

@Serializable
data class Device(
    val id: Int,
    val device_id: String,
    val device_name: String,
    val sim_no: String?,
    val device_type: String?,
    val engine_status: String,
    val status: String,
    val created_at: String
)