package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceAnalyticsResponse(
    val success: Boolean,
    val range: String? = null,              // "today/week/month"
    val message: String? = null,
    val data: DeviceAnalyticsData? = null
)

@Serializable
data class AnalyticsSegment(
    @SerialName("minutes")
    val minutes: Int = 0,

    @SerialName("hours")
    val hours: Double = 0.0,

    @SerialName("percent")
    val percent: Int = 0
)

@Serializable
data class DeviceAnalyticsData(
    @SerialName("total_distance_km")
    val totalDistanceKm: Double = 0.0,

    @SerialName("total_activity_minutes")
    val totalActivityMinutes: Int = 0,

    @SerialName("total_activity_formatted")
    val totalActivityFormatted: String = "0 hr",

    @SerialName("running")
    val running: AnalyticsSegment = AnalyticsSegment(),

    @SerialName("stopped")
    val stopped: AnalyticsSegment = AnalyticsSegment(),

    @SerialName("parked")
    val parked: AnalyticsSegment = AnalyticsSegment()
)