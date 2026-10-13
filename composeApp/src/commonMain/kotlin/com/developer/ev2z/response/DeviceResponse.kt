package com.developer.ev2z.response

import com.developer.ev2z.data.local.model.statusKindOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// `user/devices/latest-locations` was modelled twice — once here as
// DeviceResponse/DeviceItem and once as data.local.model.DeviceResponse/Device.
// Only the latter was ever parsed; this pair was dead, and a second model of one
// endpoint is how the two drift apart unnoticed. Removed 2026-10-10.

// ---------------------- MyDevicesResponse (Devices List) ----------------------

@Serializable
data class MyDevicesResponse(
    val success: Boolean = false,
    @SerialName("total_devices")
    val total_devices: Int = 0,
    val devices: List<Device> = emptyList(),
    val username: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Device(
    val id: Int = 0,
    @SerialName("device_id")
    @JsonNames("terminal_id")
    val device_id: String = "",
    @SerialName("device_name")
    val device_name: String = "",
    // The SIM is `sim_no` in this response but `sim_number` in the add-device
    // request — same field, two names, so both are read.
    @SerialName("sim_no")
    @JsonNames("sim_number")
    val sim_no: String? = null,
    @SerialName("device_type")
    val device_type: String? = null,
    // Ignition, not motion — "on"/"off"/"unknown". Deliberately NOT aliased to
    // `status`: this endpoint sends engine_status "unknown" with status "moving",
    // so folding them together would report a running vehicle as unknown.
    @SerialName("engine_status")
    val engine_status: String = "offline",
    @JsonNames("db_status", "movement")
    val status: String? = null,
    @SerialName("created_at")
    val created_at: String = ""
) {
    /**
     * The motion state in the app's own vocabulary, preferring the real motion field
     * and falling back to ignition when the endpoint sent only that.
     */
    fun statusKind(): String = statusKindOf(status ?: engine_status)
}
