// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/HistoryPlayback/HistoryPlaybackViewModel.kt
package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.HistoryPlaybackRepository
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.RoutePoint
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.math.*

sealed class HistoryPlaybackUiState {
    object Idle : HistoryPlaybackUiState()
    object Loading : HistoryPlaybackUiState()
    object Success : HistoryPlaybackUiState()
    data class Error(val message: String) : HistoryPlaybackUiState()
}

@OptIn(kotlin.time.ExperimentalTime::class)
class HistoryPlaybackViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) : ViewModel() {

    private val repository = HistoryPlaybackRepository(apiService)

    private val _uiState = MutableStateFlow<HistoryPlaybackUiState>(HistoryPlaybackUiState.Idle)
    val uiState: StateFlow<HistoryPlaybackUiState> = _uiState.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    // ✅ FIXED: Initialize with YESTERDAY's date
//    private val today = Instant.fromEpochMilliseconds(getTimeMillis())
//        .toLocalDateTime(TimeZone.currentSystemDefault())
//        .date
    private val today = Instant.fromEpochMilliseconds(getTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date



    private val yesterday = today.minus(1, DateTimeUnit.DAY)

//    private val _startDate = MutableStateFlow(yesterday.toString())
//    val startDate: StateFlow<String> = _startDate.asStateFlow()
//
//    private val _endDate = MutableStateFlow(yesterday.toString())
//    val endDate: StateFlow<String> = _endDate.asStateFlow()

    private val _startDate = MutableStateFlow(today.toString())
    val startDate: StateFlow<String> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(today.toString())
    val endDate: StateFlow<String> = _endDate.asStateFlow()


//    val maxSelectableDate = today.minus(1, DateTimeUnit.DAY)
//    val minSelectableDate = today.minus(3, DateTimeUnit.MONTH)
val maxSelectableDate = today
    val minSelectableDate = today.minus(3, DateTimeUnit.MONTH)


    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlaybackIndex = MutableStateFlow(0)
    val currentPlaybackIndex: StateFlow<Int> = _currentPlaybackIndex.asStateFlow()

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance.asStateFlow()

    private val _averageSpeed = MutableStateFlow(0.0)
    val averageSpeed: StateFlow<Double> = _averageSpeed.asStateFlow()

    private val _showStartDatePicker = MutableStateFlow(false)
    val showStartDatePicker: StateFlow<Boolean> = _showStartDatePicker.asStateFlow()

    private val _showEndDatePicker = MutableStateFlow(false)
    val showEndDatePicker: StateFlow<Boolean> = _showEndDatePicker.asStateFlow()

    private val _showDeviceDropdown = MutableStateFlow(false)
    val showDeviceDropdown: StateFlow<Boolean> = _showDeviceDropdown.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showNoDataDialog = MutableStateFlow(false)
    val showNoDataDialog: StateFlow<Boolean> = _showNoDataDialog.asStateFlow()

    private var playbackJob: Job? = null

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = HistoryPlaybackUiState.Loading
            try {
                val token = storageManager.getString("token", "") ?: ""
                val response = apiService.getMyDevices(token)

                if (response.success) {
                    _devices.value = response.devices
                    _uiState.value = HistoryPlaybackUiState.Idle
                } else {
                    _uiState.value = HistoryPlaybackUiState.Error("Failed to load devices")
                }
            } catch (e: Exception) {
                _uiState.value = HistoryPlaybackUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        _showDeviceDropdown.value = false
        _routePoints.value = emptyList()
        _currentPlaybackIndex.value = 0
    }

    fun toggleDeviceDropdown() {
        _showDeviceDropdown.value = !_showDeviceDropdown.value
    }

    fun selectStartDate(date: String) {
        _startDate.value = date
        _showStartDatePicker.value = false
        _routePoints.value = emptyList()
        _currentPlaybackIndex.value = 0
    }

    fun selectEndDate(date: String) {
        _endDate.value = date
        _showEndDatePicker.value = false
        _routePoints.value = emptyList()
        _currentPlaybackIndex.value = 0
    }

    fun toggleStartDatePicker() {
        _showStartDatePicker.value = !_showStartDatePicker.value
    }

    fun toggleEndDatePicker() {
        _showEndDatePicker.value = !_showEndDatePicker.value
    }

    fun dismissNoDataDialog() {
        _showNoDataDialog.value = false
    }

    fun fetchHistory() {
        val device = _selectedDevice.value ?: return
        val start = _startDate.value
        val end = _endDate.value

        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = HistoryPlaybackUiState.Loading

            try {
                val token = storageManager.getString("token", "") ?: ""
                val result = repository.getRouteHistory(
                    token = token,
                    startDate = start,
                    endDate = end,
                    terminalId = device.device_id
                )

                result.fold(
                    onSuccess = { response ->
                        val validPoints = response.data.filter { it.isValid() }

                        if (validPoints.isEmpty()) {
                            _showNoDataDialog.value = true
                            _routePoints.value = emptyList()
                            _totalDistance.value = 0.0
                            _averageSpeed.value = 0.0
                            _uiState.value = HistoryPlaybackUiState.Idle
                        } else {
                            _routePoints.value = validPoints
                            calculateStats(validPoints)
                            _uiState.value = HistoryPlaybackUiState.Success
                        }
                    },
                    onFailure = { error ->
                        _routePoints.value = emptyList()
                        _totalDistance.value = 0.0
                        _averageSpeed.value = 0.0
                        _uiState.value = HistoryPlaybackUiState.Error(
                            error.message ?: "Failed to fetch history"
                        )
                    }
                )
            } catch (e: Exception) {
                _routePoints.value = emptyList()
                _totalDistance.value = 0.0
                _averageSpeed.value = 0.0
                _uiState.value = HistoryPlaybackUiState.Error(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateStats(points: List<RoutePoint>) {
        var totalDist = 0.0

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val lat1 = p1.getLatDouble() ?: continue
            val lng1 = p1.getLngDouble() ?: continue
            val lat2 = p2.getLatDouble() ?: continue
            val lng2 = p2.getLngDouble() ?: continue

            totalDist += haversineDistance(lat1, lng1, lat2, lng2)
        }

        _totalDistance.value = totalDist

        val movingPoints = points.filter { it.movement == "moving" }
        _averageSpeed.value = if (movingPoints.isNotEmpty() && totalDist > 0) {
            (totalDist / (points.size / 60.0)).coerceAtMost(200.0)
        } else {
            0.0
        }
    }

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val lat1Rad = lat1 * PI / 180.0
        val lat2Rad = lat2 * PI / 180.0
        val dLatRad = (lat2 - lat1) * PI / 180.0
        val dLngRad = (lng2 - lng1) * PI / 180.0

        val a = sin(dLatRad / 2) * sin(dLatRad / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLngRad / 2) * sin(dLngRad / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob = viewModelScope.launch {
            while (_isPlaying.value && _currentPlaybackIndex.value < _routePoints.value.size - 1) {
                delay(500)
                _currentPlaybackIndex.value += 1
            }

            if (_currentPlaybackIndex.value >= _routePoints.value.size - 1) {
                _isPlaying.value = false
            }
        }
    }

    private fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun stopPlayback() {
        _isPlaying.value = false
        _currentPlaybackIndex.value = 0
        playbackJob?.cancel()
    }

    fun restartPlayback() {
        stopPlayback()
        startPlayback()
    }

    fun seekTo(index: Int) {
        _currentPlaybackIndex.value = index.coerceIn(0, _routePoints.value.size - 1)
    }

    fun navigateToPreviousDate() {
        val currentDate = LocalDate.parse(_startDate.value)
        val previousDate = currentDate.minus(1, DateTimeUnit.DAY)

        if (previousDate >= minSelectableDate) {
            _startDate.value = previousDate.toString()
            _endDate.value = previousDate.toString()
        }
    }

    fun navigateToNextDate() {
        val currentDate = LocalDate.parse(_startDate.value)
        val nextDate = currentDate.plus(1, DateTimeUnit.DAY)

        if (nextDate <= maxSelectableDate) {
            _startDate.value = nextDate.toString()
            _endDate.value = nextDate.toString()
        }
    }

    fun selectDateRange(start: String, end: String) {
        _startDate.value = start
        _endDate.value = end
        _routePoints.value = emptyList()
        _currentPlaybackIndex.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
    }
}