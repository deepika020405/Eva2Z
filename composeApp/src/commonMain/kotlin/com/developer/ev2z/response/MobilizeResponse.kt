package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class MobilizeResponse(
    val success: Boolean,
    val message: String
)