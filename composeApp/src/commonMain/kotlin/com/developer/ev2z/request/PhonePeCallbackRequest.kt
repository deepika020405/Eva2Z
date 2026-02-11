package com.developer.ev2z.request

import kotlinx.serialization.Serializable

@Serializable
data class PhonePeCallbackRequest(
    val merchantOrderId: String,
    val phonepeOrderId: String,
    val status: String // SUCCESS | FAILED
)