package com.developer.ev2z.response

@kotlinx.serialization.Serializable
data class GeoFenceDto(
    val id: Int = 0,
    val device_id: String = "",
    val name: String = "",
    val latitude: String = "0",
    val longitude: String = "0",
    val radius: String = "0",
    val coordinates_json: String? = null,
    val is_active: Int = 1,
    val created_at: String = "",
    val updated_at: String = ""
)

@kotlinx.serialization.Serializable
data class GeoFenceResponse(
    val success: Boolean = false,
    val total: Int = 0,
    val data: List<GeoFenceDto> = emptyList(),
    val message: String? = null
)