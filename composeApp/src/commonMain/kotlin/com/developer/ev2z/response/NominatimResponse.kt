package com.developer.ev2z.response

@kotlinx.serialization.Serializable
data class NominatimResponse(
    val display_name: String,
    val lat: String,
    val lon: String
)