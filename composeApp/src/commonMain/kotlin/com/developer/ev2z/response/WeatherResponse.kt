package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Open-Meteo forecast payload (https://open-meteo.com — open source, keyless).
 *
 * Every list Open-Meteo returns is index-aligned with its `time` array, and any entry
 * can come back null for hours the model has no value for, so the element types are
 * nullable rather than trusting a dense series.
 */
@Serializable
data class WeatherForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    @SerialName("timezone_abbreviation") val timezoneAbbreviation: String? = null,
    val current: CurrentWeatherDto? = null,
    val hourly: HourlyWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

@Serializable
data class CurrentWeatherDto(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("relative_humidity_2m") val humidity: Int? = null,
    @SerialName("apparent_temperature") val feelsLike: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("pressure_msl") val pressure: Double? = null
)

@Serializable
data class HourlyWeatherDto(
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList()
)

@Serializable
data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val tempMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeedMax: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList()
)