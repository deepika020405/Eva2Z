package com.developer.ev2z.ui.screens.weather

/**
 * The visual families the WMO weather codes collapse into. The screen picks an icon
 * and a background gradient from this rather than switching on the raw code in the UI.
 */
enum class WeatherKind {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDERSTORM, UNKNOWN
}

/**
 * WMO weather interpretation codes as returned by Open-Meteo.
 * Reference: https://open-meteo.com/en/docs (Weather variable documentation)
 */
data class WeatherCondition(
    val kind: WeatherKind,
    val label: String
)

fun weatherConditionFor(code: Int?): WeatherCondition = when (code) {
    0 -> WeatherCondition(WeatherKind.CLEAR, "Clear sky")
    1 -> WeatherCondition(WeatherKind.CLEAR, "Mainly clear")
    2 -> WeatherCondition(WeatherKind.PARTLY_CLOUDY, "Partly cloudy")
    3 -> WeatherCondition(WeatherKind.CLOUDY, "Overcast")
    45, 48 -> WeatherCondition(WeatherKind.FOG, "Foggy")
    51 -> WeatherCondition(WeatherKind.DRIZZLE, "Light drizzle")
    53 -> WeatherCondition(WeatherKind.DRIZZLE, "Drizzle")
    55 -> WeatherCondition(WeatherKind.DRIZZLE, "Heavy drizzle")
    56, 57 -> WeatherCondition(WeatherKind.DRIZZLE, "Freezing drizzle")
    61 -> WeatherCondition(WeatherKind.RAIN, "Light rain")
    63 -> WeatherCondition(WeatherKind.RAIN, "Rain")
    65 -> WeatherCondition(WeatherKind.RAIN, "Heavy rain")
    66, 67 -> WeatherCondition(WeatherKind.RAIN, "Freezing rain")
    71 -> WeatherCondition(WeatherKind.SNOW, "Light snow")
    73 -> WeatherCondition(WeatherKind.SNOW, "Snow")
    75 -> WeatherCondition(WeatherKind.SNOW, "Heavy snow")
    77 -> WeatherCondition(WeatherKind.SNOW, "Snow grains")
    80 -> WeatherCondition(WeatherKind.RAIN, "Light showers")
    81 -> WeatherCondition(WeatherKind.RAIN, "Rain showers")
    82 -> WeatherCondition(WeatherKind.RAIN, "Violent showers")
    85, 86 -> WeatherCondition(WeatherKind.SNOW, "Snow showers")
    95 -> WeatherCondition(WeatherKind.THUNDERSTORM, "Thunderstorm")
    96, 99 -> WeatherCondition(WeatherKind.THUNDERSTORM, "Thunderstorm with hail")
    else -> WeatherCondition(WeatherKind.UNKNOWN, "—")
}

/** Conditions right now, at the coordinate the fix came from. */
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double?,
    val condition: WeatherCondition,
    val humidity: Int?,
    val windSpeed: Double?,
    val precipitation: Double?,
    val pressure: Double?,
    val isDay: Boolean,
    val todayHigh: Double?,
    val todayLow: Double?,
    val sunrise: String?,
    val sunset: String?,
    val uvIndexMax: Double?,
    val rainChanceToday: Int?,
    /** Local clock time of the reading, already formatted (e.g. "4:20 PM"). */
    val observedAt: String
)

data class HourlyForecast(
    /** "Now", "3 PM", "12 AM" … */
    val label: String,
    val temperature: Double,
    val condition: WeatherCondition,
    val rainChance: Int?,
    val isNow: Boolean,
    /** Only meaningful for the "Now" card; future hours are drawn in their day form. */
    val isDay: Boolean = true
)

data class DailyForecast(
    /** "Today", "Tomorrow", "Mon" … */
    val label: String,
    val dateLabel: String,
    val condition: WeatherCondition,
    val high: Double?,
    val low: Double?,
    val rainChance: Int?,
    val isToday: Boolean
)

/** Why a location request couldn't be answered — mirrors the fuel screen's handling. */
enum class WeatherLocationIssue {
    /** Phone-wide location services (GPS) are switched off. */
    SERVICE_OFF,

    /** The app doesn't have permission and the OS won't prompt again. */
    PERMISSION_DENIED,

    /** Granted and on, but no fix arrived (indoors, timeout). */
    NO_FIX
}

data class WeatherUiState(
    val isLoading: Boolean = false,
    val isLocating: Boolean = false,
    /** True while refreshing over content that is already on screen. */
    val isRefreshing: Boolean = false,
    val locationLabel: String = "",
    val permissionRequired: Boolean = false,
    val locationIssue: WeatherLocationIssue? = null,
    val current: CurrentWeather? = null,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
    val error: String? = null
) {
    /** Nothing to show and no location to base it on — the screen asks instead. */
    val needsLocation: Boolean
        get() = current == null && permissionRequired
}

/** KMP-safe rounding to a whole-degree label — String.format is JVM-only. */
fun formatTemperature(value: Double): String {
    val rounded = if (value < 0) (value - 0.5).toInt() else (value + 0.5).toInt()
    return "$rounded°"
}

/** One decimal place, for wind/UV readings where the fraction matters. */
fun formatOneDecimal(value: Double): String {
    val scaled = (value * 10 + if (value < 0) -0.5 else 0.5).toInt()
    val magnitude = if (scaled < 0) -scaled else scaled
    // The sign is carried separately so values between -1 and 0 don't come out as "0.x".
    val sign = if (scaled < 0) "-" else ""
    return "$sign${magnitude / 10}.${magnitude % 10}"
}