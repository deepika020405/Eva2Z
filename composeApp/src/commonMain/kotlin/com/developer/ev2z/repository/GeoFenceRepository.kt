package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.GeoFenceDto

class GeoFenceRepository(
    private val apiService: ApiService
) {

    suspend fun fetchGeoFences(): Result<List<GeoFenceDto>> {
        return try {
            val res = apiService.getGeoFences()
            if (res.success) {
                Result.success(res.data)
            } else {
                Result.failure(Exception("Failed to load geofences"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}