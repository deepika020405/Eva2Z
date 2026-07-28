package com.developer.ev2z.ui.screens.fuelPrice

import com.developer.ev2z.repository.FuelPriceRepository
import com.developer.ev2z.response.FuelStateDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class FuelPriceViewModel(
    private val repository: FuelPriceRepository = FuelPriceRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(FuelPriceUiState())
    val uiState: StateFlow<FuelPriceUiState> = _uiState.asStateFlow()

    private var allStates: List<FuelStateDto> = emptyList()

    init {
        loadCatalogue()
    }

    fun retry() = loadCatalogue()

    fun onStateSelected(stateName: String) {
        if (stateName == _uiState.value.selectedState) return
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val refreshed = try {
                repository.getState(stateName)
            } catch (t: Throwable) {
                println("⚠️ Fuel: state refresh failed for $stateName — ${t.message}")
                null
            }
            // Fall back to the catalogue copy so a failed refresh still switches state.
            val state = refreshed ?: allStates.firstOrNull { it.stateName == stateName }
            if (state == null) {
                _uiState.update { it.copy(isLoading = false, error = "No prices for $stateName") }
                return@launch
            }
            if (refreshed != null) {
                allStates = allStates.map { if (it.stateName == state.stateName) state else it }
            }
            _uiState.update { it.showState(state) }
        }
    }

    private fun loadCatalogue() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                allStates = repository.getAllStates().filter { it.stateName.isNotBlank() }
                if (allStates.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "No fuel prices available") }
                    return@launch
                }
                val initial = allStates.firstOrNull { it.isDefault }
                    ?: allStates.firstOrNull { it.stateName.equals(DEFAULT_STATE, ignoreCase = true) }
                    ?: allStates.first()
                _uiState.update { it.showState(initial) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, error = t.message ?: "Couldn't load fuel prices")
                }
            }
        }
    }

    private fun FuelPriceUiState.showState(state: FuelStateDto): FuelPriceUiState = copy(
        isLoading = false,
        error = null,
        selectedState = state.stateName,
        availableStates = allStates.map { it.stateName }.sorted(),
        current = FuelPrice(
            state = state.stateName,
            petrolPrice = state.fuelPrices?.petrolPrice ?: 0.0,
            dieselPrice = state.fuelPrices?.dieselPrice ?: 0.0,
            updatedAt = nowLabel()
        ),
        nearbyStates = nearbyTo(state)
    )


    private fun nearbyTo(state: FuelStateDto): List<NearbyStatePrice> {
        val lat = state.coordinates?.lat ?: return emptyList()
        val long = state.coordinates.long ?: return emptyList()
        return allStates
            .filter { it.stateName != state.stateName && it.coordinates?.lat != null && it.coordinates.long != null }
            .sortedBy { distanceKm(lat, long, it.coordinates!!.lat!!, it.coordinates.long!!) }
            .take(3)
            .map {
                NearbyStatePrice(
                    stateName = it.stateName,
                    petrolPrice = it.fuelPrices?.petrolPrice ?: 0.0,
                    dieselPrice = it.fuelPrices?.dieselPrice ?: 0.0
                )
            }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    /** The endpoint re-fetches upstream rates per call, so "now" is when they were read. */
    @OptIn(ExperimentalTime::class)
    private fun nowLabel(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = if (now.hour % 12 == 0) 12 else now.hour % 12
        val minute = now.minute.toString().padStart(2, '0')
        val meridiem = if (now.hour < 12) "AM" else "PM"
        return "today, $hour:$minute $meridiem"
    }

    private companion object {
        const val DEFAULT_STATE = "Uttar Pradesh"
    }
}