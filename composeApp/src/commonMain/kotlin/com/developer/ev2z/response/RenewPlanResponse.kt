package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class RenewPlanResponse(
    @SerialName("merchantOrderId")
    val merchantOrderId: String,

    @SerialName("finalAmount")
    val finalAmount: Int,

    @SerialName("status")
    val status: String,

    @SerialName("phonepeOrderId")
    val phonepeOrderId: String,

    @SerialName("orderToken")
    val orderToken: String,

    @SerialName("merchantId")
    val merchantId: String
)