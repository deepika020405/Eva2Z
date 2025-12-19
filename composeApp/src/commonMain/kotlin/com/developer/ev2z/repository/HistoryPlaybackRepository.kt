// composeApp/src/commonMain/kotlin/com/developer/ev2z/repository/HistoryPlaybackRepository.kt
package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.RouteHistoryResponse

class HistoryPlaybackRepository(
    private val apiService: ApiService
) {
    suspend fun getRouteHistory(
        token: String,
        startDate: String,
        endDate: String,
        terminalId: String
    ): Result<RouteHistoryResponse> {
        return try {
            val response = apiService.getRouteHistory(
                token = token,
                startDate = startDate,
                endDate = endDate,
                terminalId = terminalId
            )

            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to fetch route history"))
            }
        } catch (e: Exception) {
            println("❌ Repository Error: ${e.message}")
            Result.failure(e)
        }
    }
}