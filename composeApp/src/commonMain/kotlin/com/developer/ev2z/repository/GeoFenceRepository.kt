package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.GeoFenceDto
import com.developer.ev2z.util.AppTokenProvider
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

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

    suspend fun fetchMyDevices(token: String): Result<List<Device>> =
        try {
            val res = apiService.getMyDevices(token)

            if (res.success) {
                Result.success(res.devices) // ✅ CORRECT
            } else {
                Result.failure(Exception("Failed to load devices"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }

    // ✅ SAVE GEOFENCE
    suspend fun saveGeoFence(
        token: String,
        deviceId: String,
        name: String,
        latitude: Double,
        longitude: Double,
        radius: Int
    ): Result<Unit> =
        try {
            val res = apiService.saveGeoFence(
                token = token,
                deviceId = deviceId,
                name = name,
                latitude = latitude,
                longitude = longitude,
                radius = radius
            )

            if (res["success"]?.jsonPrimitive?.boolean == true) {
                Result.success(Unit)
            } else {
                Result.failure(
                    Exception(res["message"]?.jsonPrimitive?.content ?: "Save failed")
                )
            }

        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun deleteGeoFence(
        geoFence: GeoFenceDto,
        token: String
    ): Result<Unit> {
        return try {
            apiService.deleteGeoFence(
                token = token,
                geoFenceId = geoFence.id,
                deviceId = geoFence.device_id,
                name = geoFence.name,
                latitude = geoFence.latitude,
                longitude = geoFence.longitude,
                radius = geoFence.radius
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun updateGeoFence(
        geoFence: GeoFenceDto,
        name: String,
        radius: Int,
        lat: String,
        lng: String
    ): Result<Unit> {
        return try {
            apiService.updateGeoFence(
                geoFenceId = geoFence.id,
                token = AppTokenProvider.getToken(),
                deviceId = geoFence.device_id,
                name = name,
                latitude = lat,
                longitude = lng,
                radius = radius
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}


