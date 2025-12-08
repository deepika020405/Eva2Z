package com.developer.ev2z.request

import kotlinx.serialization.Serializable

@Serializable
data class DeviceAnalyticsRequest(
    val device_id: String,
    val range: String    // "today" / "week" / "month" (lowercase)
)