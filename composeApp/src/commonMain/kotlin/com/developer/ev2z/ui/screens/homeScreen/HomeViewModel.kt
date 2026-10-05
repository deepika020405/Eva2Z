// ui/screens/homeScreen/HomeViewModel.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.repository.HomeRepository
import com.developer.ev2z.util.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val deviceResponse: DeviceResponse) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    object Unauthorized : HomeUiState()
}

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _showOverview = MutableStateFlow(false)
    val showOverview: StateFlow<Boolean> = _showOverview.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()


    private val _showDeviceDropdown = MutableStateFlow(false)
    val showDeviceDropdown: StateFlow<Boolean> = _showDeviceDropdown.asStateFlow()

    private var devicesPollingJob: Job? = null

    // Where each device was on the previous poll. This is the whole basis of the
    // Running/Stopped status: the server's own `status` field is overridden with what
    // the coordinates are actually doing between two polls (see withMovementStatus).
    private var previousPositions: Map<String, GeoPoint> = emptyMap()

    fun loadDevices(token: String) {
        viewModelScope.launch { fetchDevices(token, showLoading = true) }
    }

    /**
     * Re-asks the server for every device's coordinates on a timer.
     *
     * The map marker's position comes straight out of `response.data`, so without
     * this the coordinates read when the screen opened are the only ones it ever
     * sees and the marker sits still however far the vehicle travels. 15s matches
     * DeviceReportViewModel and TrackTraceViewModel, the two screens that already
     * poll.
     *
     * The caller owns the lifetime: start it when the map appears and call
     * [stopDevicesPolling] when it goes away, or it keeps spending requests behind
     * screens nobody is looking at.
     */
    fun startDevicesPolling(token: String) {
        devicesPollingJob?.cancel()
        devicesPollingJob = viewModelScope.launch {
            while (isActive) {
                // Delay first: loadDevices has already made the opening request, so
                // firing one here too would just double it.
                delay(DEVICES_POLL_INTERVAL_MS)
                fetchDevices(token, showLoading = false)
            }
        }
    }

    fun stopDevicesPolling() {
        devicesPollingJob?.cancel()
        devicesPollingJob = null
    }

    /**
     * [showLoading] is false for a poll and for a refresh that has data to fall back
     * on, and that matters for more than the spinner: `HomeUiState.Loading` makes
     * LiveLocationScreen swap the whole map out for a progress indicator, so a
     * polled refresh announcing Loading would tear the map down and rebuild it —
     * camera and all — every 15 seconds.
     */
    private suspend fun fetchDevices(token: String, showLoading: Boolean) {
        if (showLoading) _uiState.value = HomeUiState.Loading

        repository.getDevicesLatestLocations(token).fold(
            onSuccess = { response ->
                // Status comes from whether the coordinates moved since the last poll,
                // not from the server's field. Compared against the previous positions
                // first, then those are replaced for the next tick to compare against.
                val withStatus = response.withMovementStatus(previousPositions)
                previousPositions = response.data.positionsById()

                _uiState.value = HomeUiState.Success(withStatus)
                // First proof this run that the stored token is still good. FCM registration
                // waits on this so a revoked session can never re-register the device.

                SessionState.markVerified()
                println("✅ Devices loaded: ${response.totalDevices}")

                withStatus.data.forEach { device ->
                    println(
                        "   ${device.getDisplayName()}: server=${
                            response.data.firstOrNull { it.deviceId == device.deviceId }?.status
                        } → derived=${device.status} (${device.latitude},${device.longitude})"
                    )
                }
            },
            onFailure = { error ->
                if (error is UnauthorizedException) {
                    println("🔒 [Home] 401 Unauthorized — redirecting to login")
                    SessionState.invalidate()
                    _uiState.value = HomeUiState.Unauthorized
                } else {
                    println("❌ Error loading devices: ${error.message}")
                    // One dropped poll must not replace coordinates already on the
                    // map with an error screen — the next tick is only 15s away.
                    if (_uiState.value !is HomeUiState.Success) {
                        _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                    }
                }
            }
        )
    }

    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        _showDeviceDropdown.value = false
        if (device.hasValidLocation()) {
            _showOverview.value = true
        }
    }

    fun toggleDeviceDropdown() {
        _showDeviceDropdown.value = !_showDeviceDropdown.value
    }

    fun showOverviewSheet() {
        _showOverview.value = true
    }

    fun hideOverviewSheet() {
        _showOverview.value = false
        _selectedDevice.value = null
    }

    fun refreshDevices(token: String) {
        if (_isRefreshing.value) return // 👈 double click block

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Only the very first load may blank the screen; once there are
                // coordinates to show, a refresh keeps the map up and swaps the data
                // underneath it. isRefreshing drives the spinner instead.
                fetchDevices(token, showLoading = _uiState.value !is HomeUiState.Success)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    override fun onCleared() {
        stopDevicesPolling()
        super.onCleared()
    }

    private companion object {
        const val DEVICES_POLL_INTERVAL_MS = 15_000L
    }
}