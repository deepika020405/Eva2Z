package com.developer.ev2z.response

@kotlinx.serialization.Serializable
data class GeoFenceDto(
    val id: Int,
    val device_id: String,
    val name: String,
    val latitude: String,
    val longitude: String,
    val radius: String,
    val coordinates_json: String? = null,
    val is_active: Int,
    val created_at: String,
    val updated_at: String
)

@kotlinx.serialization.Serializable
data class GeoFenceResponse(
    val success: Boolean,
    val total: Int,
    val data: List<GeoFenceDto>
)
