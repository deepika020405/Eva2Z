package com.developer.ev2z.request

import kotlinx.serialization.Serializable



@Serializable
data class RenewPlanRequest(
    val device_id: String,   // ✅ IMEI here
    val plan_days: Int,
    val amount: Int
)