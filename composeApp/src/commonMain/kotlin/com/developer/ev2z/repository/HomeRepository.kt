package com.developer.ev2z.repository

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.ApiService

class HomeRepository(private val apiService: ApiService) {

    suspend fun getDevicesLatestLocations(token: String): Result<DeviceResponse> {
        return try {
            val response = apiService.getDevicesLatestLocations(token)
            Result.success(response)
        } catch (e: Exception) {
            println("❌ Repository Error: ${e.message}")
            Result.failure(e)
        }
    }
}