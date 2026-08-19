package com.developer.ev2z.ui.screens.weather

import com.developer.ev2z.LocationPermissionEvents
import com.developer.ev2z.fetchPhoneLocation
import com.developer.ev2z.hasLocationPermission
import com.developer.ev2z.isLocationEnabled
import com.developer.ev2z.openAppSettings
import com.developer.ev2z.openLocationSettings
import com.developer.ev2z.repository.WeatherRepository
import com.developer.ev2z.response.WeatherForecastResponse
import com.developer.ev2z.util.getCityFromLatLng
import com.developer.ev2z.util.getStateFromLatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.coroutines.resume

/**
 * Resolves where the phone is, then keeps [WeatherUiState] filled with current
 * conditions and the hourly / 7-day forecast for that spot.
 *
 * Location handling mirrors [com.developer.ev2z.ui.screens.fuelPrice.FuelPriceViewModel]:
 * the popup only appears when the user actually asked for their location, never as a
 * surprise on app open.
 */
class WeatherViewModel(
    private val repository: WeatherRepository = WeatherRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    // The coordinate the shown forecast belongs to — lets refresh skip a second GPS fix.
    private var lastFix: Pair<Double, Double>? = null

    // Guards against overlapping loads: the startup load can still be waiting on the
    // permission prompt when the grant event arrives and tries to start another one.
    private var loadInFlight = false

    init {
        // The startup permission prompt lives in MainActivity/AppDelegate, so pick up its
        // answer here rather than making the user tap again.
        scope.launch {
            LocationPermissionEvents.changed.collect {
                if (hasLocationPermission() && _uiState.value.current == null) {
                    load(userInitiated = false)
                }
            }
        }
        scope.launch { load(userInitiated = false) }
    }

    /** Called when the app returns to the foreground — catches a location toggle flip. */
    fun onAppResumed() {
        val state = _uiState.value
        if (state.isLoading || state.isLocating || state.current != null) return
        if (!hasLocationPermission() || !isLocationEnabled()) return
        scope.launch { load(userInitiated = false) }
    }

    fun retry() {
        scope.launch { load(userInitiated = true) }
    }

    /** Pull-to-refresh / refresh button — reuses the last fix so it stays quick. */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        scope.launch { load(userInitiated = false, refreshing = _uiState.value.current != null) }
    }

    fun requestLocation() {
        if (_uiState.value.isLocating) return
        scope.launch { load(userInitiated = true) }
    }

    fun dismissLocationIssue() {
        _uiState.update { it.copy(locationIssue = null) }
    }

    /** Sends the user to wherever they can actually fix the blocked request. */
    fun openSettingsForIssue() {
        when (_uiState.value.locationIssue) {
            WeatherLocationIssue.SERVICE_OFF -> openLocationSettings()
            WeatherLocationIssue.PERMISSION_DENIED -> openAppSettings()
            else -> Unit
        }
        dismissLocationIssue()
    }

    private suspend fun load(userInitiated: Boolean, refreshing: Boolean = false) {
        if (loadInFlight) return
        loadInFlight = true
        try {
            loadInternal(userInitiated, refreshing)
        } finally {
            loadInFlight = false
        }
    }

    private suspend fun loadInternal(userInitiated: Boolean, refreshing: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = it.current == null && !refreshing,
                isRefreshing = refreshing,
                isLocating = true,
                error = null,
                locationIssue = null
            )
        }

        // A refresh reuses the coordinate we already resolved; only a cold load or an
        // explicit retry pays for a new fix.
        val fix = (if (refreshing) lastFix else null) ?: currentLatLng()
        if (fix == null) {
            val granted = hasLocationPermission()
            val servicesOn = isLocationEnabled()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLocating = false,
                    // Only block the screen when there is nothing on it already.
                    permissionRequired = it.current == null,
                    locationIssue = when {
                        !userInitiated -> null
                        !servicesOn -> WeatherLocationIssue.SERVICE_OFF
                        granted -> WeatherLocationIssue.NO_FIX
                        else -> WeatherLocationIssue.PERMISSION_DENIED
                    }
                )
            }
            return
        }

        lastFix = fix
        _uiState.update { it.copy(isLocating = false, permissionRequired = false) }

        val (lat, lng) = fix
        val forecast = try {
            repository.getForecast(lat, lng)
        } catch (t: Throwable) {
            println("⚠️ Weather: forecast failed — ${t.message}")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    // Keep whatever is already on screen; only show the error when blank.
                    error = if (it.current == null) {
                        t.message ?: "Couldn't load the weather"
                    } else {
                        null
                    }
                )
            }
            return
        }

        val city = runCatching { getCityFromLatLng(lat, lng) }.getOrNull().orEmpty()
        val region = runCatching { getStateFromLatLng(lat, lng) }.getOrNull().orEmpty()

        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                isLocating = false,
                error = null,
                locationLabel = locationLabel(city, region),
                current = forecast.toCurrentWeather(),
                hourly = forecast.toHourly(),
                daily = forecast.toDaily()
            )
        }
    }

    private fun locationLabel(city: String, region: String): String = when {
        city.isNotBlank() && region.isNotBlank() && !city.equals(region, ignoreCase = true) ->
            "$city, $region"
        city.isNotBlank() -> city
        region.isNotBlank() -> region
        else -> "Your location"
    }

    // Suspends until the platform delivers a fix. (0,0) means denied/unavailable.
    // The timeout covers the permission dialog still being on screen.
    private suspend fun currentLatLng(): Pair<Double, Double>? {
        val fix = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Pair<Double, Double>> { cont ->
                var delivered = false
                fetchPhoneLocation { lat, lng ->
                    if (!delivered) {
                        delivered = true
                        cont.resume(lat to lng)
                    }
                }
            }
        } ?: return null
        return fix.takeIf { it.first != 0.0 || it.second != 0.0 }
    }

    // ───────────────────────── response → UI models ─────────────────────────

    private fun WeatherForecastResponse.toCurrentWeather(): CurrentWeather? {
        val now = current ?: return null
        val temp = now.temperature ?: return null
        // Index 0 of the daily series is today, so today's high/low and sun times come
        // from there rather than being derived from the hourly list.
        val today = 0
        return CurrentWeather(
            temperature = temp,
            feelsLike = now.feelsLike,
            condition = weatherConditionFor(now.weatherCode),
            humidity = now.humidity,
            windSpeed = now.windSpeed,
            precipitation = now.precipitation,
            pressure = now.pressure,
            isDay = now.isDay != 0,
            todayHigh = daily?.tempMax?.getOrNull(today),
            todayLow = daily?.tempMin?.getOrNull(today),
            sunrise = daily?.sunrise?.getOrNull(today)?.let { formatClock(it) },
            sunset = daily?.sunset?.getOrNull(today)?.let { formatClock(it) },
            uvIndexMax = daily?.uvIndexMax?.getOrNull(today),
            rainChanceToday = daily?.precipitationProbabilityMax?.getOrNull(today),
            observedAt = now.time?.let { formatClock(it) } ?: ""
        )
    }

    /**
     * The next 24 hours starting from the current hour. Open-Meteo returns the whole
     * day from midnight, so everything before now is dropped rather than shown as a
     * forecast that has already happened.
     */
    private fun WeatherForecastResponse.toHourly(): List<HourlyForecast> {
        val series = hourly ?: return emptyList()
        val nowStamp = current?.time ?: return emptyList()
        val nowHour = parseLocalDateTime(nowStamp) ?: return emptyList()

        val startIndex = series.time.indexOfFirst { stamp ->
            val at = parseLocalDateTime(stamp)
            at != null && at >= nowHour.atHourStart()
        }.takeIf { it >= 0 } ?: return emptyList()

        return series.time.drop(startIndex).take(HOURLY_COUNT).mapIndexedNotNull { offset, stamp ->
            val index = startIndex + offset
            val temp = series.temperature.getOrNull(index) ?: return@mapIndexedNotNull null
            val at = parseLocalDateTime(stamp) ?: return@mapIndexedNotNull null
            // The first card is labelled "Now", so it shows the live reading rather than
            // the hour's forecast bucket. The two routinely disagree, and a "Now" card
            // contradicting the headline above it reads as a bug.
            val isNow = offset == 0
            HourlyForecast(
                label = if (isNow) "Now" else formatHour(at.hour),
                temperature = if (isNow) current?.temperature ?: temp else temp,
                condition = weatherConditionFor(
                    if (isNow) current?.weatherCode else series.weatherCode.getOrNull(index)
                ),
                rainChance = series.precipitationProbability.getOrNull(index),
                isNow = isNow,
                isDay = if (isNow) current?.isDay != 0 else true
            )
        }
    }

    private fun WeatherForecastResponse.toDaily(): List<DailyForecast> {
        val series = daily ?: return emptyList()
        val todayDate = parseLocalDateTime(current?.time)?.date
            ?: series.time.firstOrNull()?.let { parseLocalDate(it) }

        return series.time.mapIndexedNotNull { index, stamp ->
            val date = parseLocalDate(stamp) ?: return@mapIndexedNotNull null
            val isToday = todayDate != null && date == todayDate
            DailyForecast(
                label = when {
                    isToday -> "Today"
                    todayDate != null && index == 1 -> "Tomorrow"
                    else -> WEEKDAYS[date.dayOfWeek.ordinal]
                },
                dateLabel = "${date.day} ${MONTHS[date.month.ordinal]}",
                condition = weatherConditionFor(series.weatherCode.getOrNull(index)),
                high = series.tempMax.getOrNull(index),
                low = series.tempMin.getOrNull(index),
                rainChance = series.precipitationProbabilityMax.getOrNull(index),
                isToday = isToday
            )
        }
    }

    // ───────────────────────────── time helpers ─────────────────────────────

    // Open-Meteo sends local wall-clock stamps ("2026-07-31T16:00") because the request
    // asks for timezone=auto, so they parse straight to a LocalDateTime with no offset.
    private fun parseLocalDateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(raw) }.getOrNull()
    }

    private fun parseLocalDate(raw: String?): LocalDate? {
        if (raw.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
    }

    /** Drops minutes so a 16:15 reading still matches the 16:00 hourly bucket. */
    private fun LocalDateTime.atHourStart(): LocalDateTime =
        LocalDateTime(date.year, date.month, date.day, hour, 0)

    private fun formatHour(hour24: Int): String {
        val hour = if (hour24 % 12 == 0) 12 else hour24 % 12
        return "$hour ${if (hour24 < 12) "AM" else "PM"}"
    }

    private fun formatClock(raw: String): String {
        val at = parseLocalDateTime(raw) ?: return ""
        val hour = if (at.hour % 12 == 0) 12 else at.hour % 12
        val minute = at.minute.toString().padStart(2, '0')
        return "$hour:$minute ${if (at.hour < 12) "AM" else "PM"}"
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 30_000L
        const val HOURLY_COUNT = 24

        val MONTHS = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        // kotlinx.datetime.DayOfWeek is ordered MONDAY..SUNDAY.
        val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }
}