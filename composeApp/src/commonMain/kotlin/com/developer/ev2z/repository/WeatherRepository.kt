package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.WeatherForecastResponse

class WeatherRepository(
    private val api: ApiService = ApiService(),
) {

    /**
     * Current conditions plus hourly and 7-day forecast for a coordinate.
     * Throws when the service is unreachable or returns nothing usable.
     */
    suspend fun getForecast(lat: Double, lng: Double): WeatherForecastResponse {
        val response = api.getWeatherForecast(lat, lng)
        if (response.current == null && response.daily == null) {
            throw IllegalStateException("Couldn't load the weather right now")
        }
        return response
    }
}