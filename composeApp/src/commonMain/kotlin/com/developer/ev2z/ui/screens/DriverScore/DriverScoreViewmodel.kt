package com.developer.ev2z.ui.screens.DriverScore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─────────────────────────────────────────────────────────────────────────────
//  UI State
// ─────────────────────────────────────────────────────────────────────────────

sealed class DriverScoreUiState {
    object Loading : DriverScoreUiState()
    data class SingleVehicle(val vehicle: VehicleScore) : DriverScoreUiState()
    data class MultiVehicle(
        val vehicles: List<VehicleScore>,
        val selectedIndex: Int = 0
    ) : DriverScoreUiState() {
        val selectedVehicle: VehicleScore get() = vehicles[selectedIndex]
        val averageScore: Int             get() = vehicles.map { it.overallScore }.average().toInt()
        val totalTrips: Int               get() = vehicles.sumOf { it.totalTrips }
        val overallSafeRate: Int          get() = vehicles.map { it.safeRatePercent }.average().toInt()
    }
    data class Error(val message: String) : DriverScoreUiState()
}

sealed class LeaderboardUiState {
    object Loading : LeaderboardUiState()
    data class Success(val entries: List<LeaderboardEntry>) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
//  ViewModel — plain class, no Android ViewModel dependency
// ─────────────────────────────────────────────────────────────────────────────

class DriverScoreViewModel {

    private val _uiState = MutableStateFlow<DriverScoreUiState>(DriverScoreUiState.Loading)
    val uiState: StateFlow<DriverScoreUiState> = _uiState.asStateFlow()

    private val _leaderboardState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val leaderboardState: StateFlow<LeaderboardUiState> = _leaderboardState.asStateFlow()

    init { loadScores() }

    fun loadScores() {
        val vehicles = DriverScoreSampleData.allVehicles
        _uiState.value = if (vehicles.size == 1) {
            DriverScoreUiState.SingleVehicle(vehicles.first())
        } else {
            DriverScoreUiState.MultiVehicle(vehicles)
        }
        _leaderboardState.value = LeaderboardUiState.Success(DriverScoreSampleData.leaderboard)
    }

    fun selectVehicle(index: Int) {
        val current = _uiState.value
        if (current is DriverScoreUiState.MultiVehicle) {
            _uiState.value = current.copy(selectedIndex = index)
        }
    }

    fun refresh() {
        _uiState.value = DriverScoreUiState.Loading
        loadScores()
    }
}