package com.developer.ev2z.network


import com.developer.ev2z.response.NominatimResponse
import com.developer.ev2z.ui.screens.GeoFencing.LocationResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

object NominatimApi {

    private val client = KtorClient.client

    suspend fun search(query: String): List<LocationResult> {
        if (query.isBlank()) return emptyList()

        val response: List<NominatimResponse> =
            client.get("https://nominatim.openstreetmap.org/search") {
                parameter("q", query)
                parameter("format", "json")
                parameter("polygon_geojson", "0") // ⭐ IMPORTANT
                parameter("limit", "5")

                // ⚠️ REQUIRED by Nominatim
                header("User-Agent", "ev2z-app")
            }.body()

        return response.map {
            LocationResult(
                name = it.display_name,
                lat = it.lat.toDouble(),
                lng = it.lon.toDouble(),
            )
        }
    }
}