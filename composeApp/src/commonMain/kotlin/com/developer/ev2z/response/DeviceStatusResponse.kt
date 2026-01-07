package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class DeviceStatusResponse(
    val success: Boolean,
    val data: DeviceStatusData
)

@Serializable
data class DeviceStatusData(
    val status_count: StatusCount,
    val sessions: List<StatusSession>
)

@Serializable
data class StatusCount(
    val offline: Int,
    val moving: Int,
    val idle: Int,
    val parked: Int
)

@Serializable
data class StatusSession(
    val status: String,
    val start_time: String,
    val end_time: String,
    val latitude: String,
    val longitude: String,
    val duration_minutes: Int
)
