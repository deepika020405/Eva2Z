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

    private val today = Instant.fromEpochMilliseconds(getTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    val maxSelectableDate = today
    val minSelectableDate = today.minus(3, DateTimeUnit.MONTH)

    private val _startDate = MutableStateFlow(today.toString())
    val startDate: StateFlow<String> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(today.toString())
    val endDate: StateFlow<String> = _endDate.asStateFlow()

    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlaybackIndex = MutableStateFlow(0)
    val currentPlaybackIndex: StateFlow<Int> = _currentPlaybackIndex.asStateFlow()

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance.asStateFlow()

    // ✅ Average speed now comes ONLY from backend
    private val _averageSpeed = MutableStateFlow(0.0)
    val averageSpeed: StateFlow<Double> = _averageSpeed.asStateFlow()

    private val _showDeviceDropdown = MutableStateFlow(false)
    val showDeviceDropdown: StateFlow<Boolean> = _showDeviceDropdown.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

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

    fun dismissNoDataDialog() {
        _showNoDataDialog.value = false
    }


    fun selectDateRange(start: String, end: String) {
        _startDate.value = start
        _endDate.value = end
        _currentPlaybackIndex.value = 0

        if (_selectedDevice.value != null) {
            fetchHistory()
        }
    }


    fun navigateToPreviousDate() {
        val current = LocalDate.parse(_startDate.value)
        val previous = current.minus(1, DateTimeUnit.DAY)

        if (previous >= minSelectableDate) {
            val date = previous.toString()
            _startDate.value = date
            _endDate.value = date
            _currentPlaybackIndex.value = 0
            fetchHistory()
        }
    }

    fun navigateToNextDate() {
        val current = LocalDate.parse(_startDate.value)
        val next = current.plus(1, DateTimeUnit.DAY)

        if (next <= maxSelectableDate) {
            val date = next.toString()
            _startDate.value = date
            _endDate.value = date
            _currentPlaybackIndex.value = 0
            fetchHistory()
        }
    }



    fun fetchHistory() {
        val device = _selectedDevice.value ?: return

        viewModelScope.launch {
            _uiState.value = HistoryPlaybackUiState.Loading

            try {
                val token = storageManager.getString("token", "") ?: ""
                val result = repository.getRouteHistory(
                    token = token,
                    startDate = _startDate.value,
                    endDate = _endDate.value,
                    terminalId = device.device_id
                )

                result.fold(
                    onSuccess = { response ->
                        val validPoints = response.data.filter { it.isValid() }

                        if (validPoints.isEmpty()) {
                            _showNoDataDialog.value = true
                            resetStats()
                            _uiState.value = HistoryPlaybackUiState.Idle
                        } else {
                            _routePoints.value = validPoints
                            calculateTotalDistance(validPoints)

                            // ✅ CORRECT PROPERTY
                            _averageSpeed.value = response.averageSpeed ?: 0.0

                            _uiState.value = HistoryPlaybackUiState.Success
                        }
                    },
                    onFailure = {
                        resetStats()
                        _uiState.value = HistoryPlaybackUiState.Error("Failed to fetch history")
                    }
                )
            } catch (e: Exception) {
                resetStats()
                _uiState.value = HistoryPlaybackUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    private fun calculateTotalDistance(points: List<RoutePoint>) {
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
    }

    private fun resetStats() {
        _routePoints.value = emptyList()
        _totalDistance.value = 0.0
        _averageSpeed.value = 0.0
        _currentSpeed.value = 0
    }

    private fun haversineDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6371.0
        val dLat = (lat2 - lat1) * PI / 180
        val dLng = (lng2 - lng1) * PI / 180

        val a = sin(dLat / 2).pow(2.0) +
                cos(lat1 * PI / 180) *
                cos(lat2 * PI / 180) *
                sin(dLng / 2).pow(2.0)

        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    fun togglePlayback() {
        if (_isPlaying.value) pausePlayback() else startPlayback()
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playbackJob = viewModelScope.launch {
            while (_isPlaying.value && _currentPlaybackIndex.value < _routePoints.value.size - 1) {
                delay(500)
                _currentPlaybackIndex.value += 1
                _currentSpeed.value = _routePoints.value.getOrNull(_currentPlaybackIndex.value)?.getSpeedInt() ?: 0
            }
            _isPlaying.value = false
        }
    }

    private fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    override fun onCleared() {
        playbackJob?.cancel()
        super.onCleared()
    }
}

