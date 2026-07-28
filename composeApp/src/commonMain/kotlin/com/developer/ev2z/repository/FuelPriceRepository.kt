package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.FuelStateDto

class FuelPriceRepository(
    private val api: ApiService = ApiService(),
) {

    /** Every state / union territory — powers the picker and the nearby-state list. */
    suspend fun getAllStates(): List<FuelStateDto> = fetch(state = null)

    /**
     * Re-fetches a single state so its rates are current when the user switches to it.
     * Returns null when the backend knows no such state.
     */
    suspend fun getState(stateName: String): FuelStateDto? {
        val states = fetch(state = stateName)
        return states.firstOrNull { it.stateName.equals(stateName, ignoreCase = true) }
            ?: states.firstOrNull()
    }

    private suspend fun fetch(state: String?): List<FuelStateDto> {
        val response = api.getFuelPrices(state)
        val payload = response.data
        if (!response.status || payload == null || !payload.success) {
            throw IllegalStateException(response.message ?: "Couldn't load fuel prices")
        }
        return payload.data
    }
}