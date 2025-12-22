package com.developer.ev2z.response

import kotlinx.serialization.Serializable

@Serializable
data class NominatimResponse(
    val display_name: String,
    val lat: String,
    val lon: String,
    val geojson: GeoJson? = null
)

@Serializable
data class GeoJson(
    val type: String,
    val coordinates: List<List<List<Double>>>
)