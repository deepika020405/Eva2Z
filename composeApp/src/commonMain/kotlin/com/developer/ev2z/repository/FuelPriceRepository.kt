package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.FuelStateDto

class FuelPriceRepository(
    private val api: ApiService = ApiService(),
) {

    /** Every state / union territory — powers the picker and the nearby-state list. */
    suspend fun getAllStates(): List<FuelStateDto> {
        val response = api.getFuelPrices()
        if (!response.status) {
            throw IllegalStateException(response.message ?: "Couldn't load fuel prices")
        }
        return response.data.filter { it.stateName.isNotBlank() }
    }

    /**
     * Re-fetches a single state so its rates are current when the user switches to it.
     * Returns null when the backend knows no such state.
     */
    suspend fun getState(stateName: String): FuelStateDto? {
        val response = api.getFuelPriceForState(stateName)
        if (!response.status) {
            throw IllegalStateException(response.message ?: "Couldn't load fuel prices")
        }
        return response.data?.takeIf { it.stateName.isNotBlank() }
    }
}