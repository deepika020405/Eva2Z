package com.developer.ev2z.data.local.model

data class GeoFencePolygon(
    val points: List<LatLng>
)

data class LatLng(
    val lat: Double,
    val lng: Double
)