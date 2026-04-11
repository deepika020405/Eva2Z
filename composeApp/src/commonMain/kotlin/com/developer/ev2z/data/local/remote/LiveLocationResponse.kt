package com.developer.ev2z.data.local.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveLocationResponse(
    val success: Boolean,
    val data: LiveLocationData? = null
)

@Serializable
data class LiveLocationData(
    @SerialName("latest_location")
    val latestLocation: LatestLocation? = null,
    @SerialName("status_count")
    val statusCount: StatusCount? = null
)

@Serializable
data class LatestLocation(
    @SerialName("device_id")
    val deviceId: String = "",
    val latitude: String = "0",
    val longitude: String = "0",
    val speed: Int = 0,
    val status: String = "offline",
    val timestamp: String = "",
    @SerialName("battery_level")
    val batteryLevel: String = "",
    @SerialName("battery_display")
    val batteryDisplay: String = "",
    @SerialName("battery_status")
    val batteryStatus: String = "",
    @SerialName("last_seen_minutes_ago")
    val lastSeenMinutesAgo: Int = 0
)

@Serializable
data class StatusCount(
    val idle: Int = 0,
    val moving: Int = 0,
    val parked: Int = 0,
    val offline: Int = 0
)
//package com.developer.ev2z.data.local.remote
//
//
//
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//
//@Serializable
//data class LiveLocationResponse(
//    val success: Boolean,
//    val data: LiveLocationData? = null
//)
//
//@Serializable
//data class LiveLocationData(
//    @SerialName("latest_location")
//    val latestLocation: LatestLocation? = null,
//    @SerialName("status_count")
//    val statusCount: StatusCount? = null
//)
//
//@Serializable
//data class LatestLocation(
//    @SerialName("device_id") val deviceId: String,
//    val latitude: String,
//    val longitude: String,
//    val speed: Int,
//    val status: String,
//    val timestamp: String,
//    @SerialName("battery_level") val batteryLevel: String,
//    @SerialName("battery_display") val batteryDisplay: String,
//    @SerialName("battery_status") val batteryStatus: String,
//    @SerialName("last_seen_minutes_ago") val lastSeenMinutesAgo: Int
//)
//
//@Serializable
//data class StatusCount(
//    val idle: Int,
//    val moving: Int,
//    val parked: Int,
//    val offline: Int
//)