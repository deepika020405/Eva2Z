package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class TotalMilagesResponse(
    val success: Boolean,
    val data: TotalMilagesData? = null
)

@Serializable
data class TotalMilagesData(
    val terminal_id: String,
    val total_distance_km: Double,
    val today_distance_km: Double
)