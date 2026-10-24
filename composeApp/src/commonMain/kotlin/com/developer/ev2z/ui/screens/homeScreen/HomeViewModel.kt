// ui/screens/homeScreen/HomeViewModel.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.repository.HomeRepository
import com.developer.ev2z.util.SessionState
import com.developer.ev2z.util.VehicleStatusAlerts
import io.ktor.util.date.getTimeMillis
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()


    private val _showDeviceDropdown = MutableStateFlow(false)
    val showDeviceDropdown: StateFlow<Boolean> = _showDeviceDropdown.asStateFlow()

    private var devicesPollingJob: Job? = null

    // The whole basis of the Running/Stopped status: the server's own `status` field is
    // overridden with what the coordinates are actually doing. The tracker owns the
    // reference position and decides when it is old enough to compare against, so the
    // verdict stays a 15s one however often this screen polls.
    private val movementTracker = MovementStatusTracker()

    fun loadDevices(token: String) {
        viewModelScope.launch { fetchDevices(token, showLoading = true) }
    }

    /**
     * Re-asks the server for every device's coordinates on a timer.
     *
     * The map marker's position comes straight out of `response.data`, so without
     * this the coordinates read when the screen opened are the only ones it ever
     * sees and the marker sits still however far the vehicle travels. The interval is
     * [DEVICES_POLL_INTERVAL_MS], shared with the two dropdown screens that poll the
     * same endpoint.
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
     * camera and all — on every poll.
     */
    private suspend fun fetchDevices(token: String, showLoading: Boolean) {
        if (showLoading) _uiState.value = HomeUiState.Loading

        repository.getDevicesLatestLocations(token).fold(
            onSuccess = { response ->
                // Status comes from whether the coordinates moved, not from the
                // server's field. The tracker holds the reference position across polls
                // so the comparison always spans MOVEMENT_WINDOW_MS.
                val nowMs = getTimeMillis()
                val withStatus = movementTracker.apply(response, nowMs)

                // The Moving / Idle / Parked notifications, raised by the app itself
                // so they arrive whether or not a push does. Shared with the background
                // check rather than owned here, so the two cannot each announce the
                // same spell.
                //
                // Fed the server's own words, not `withStatus`: the tracker answers one
                // question — moving or standing still — and collapses idle and parked
                // into "stopped" on the way.
                //
                // The ignition lookup is what makes the Idle alert possible at all: this
                // response calls an engine-on standstill "parked". It is only spent on a
                // vehicle that is standing still, and cached — see IdleStatusResolver.
                // The session fallback behind it covers the trackers whose ignition line
                // reads nothing at all — without it, Vehicle Idle works on one vehicle
                // of this account and never on the other.
                VehicleStatusAlerts.onPoll(
                    response,
                    sessionKindOf = { repository.getDeviceLatestSessionStatus(token, it) },
                ) {
                    repository.getDeviceIgnition(token, it)
                }

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
                    // Whoever logs in next starts from silence rather than inheriting
                    // the previous session's idle spells.
                    VehicleStatusAlerts.reset()
                    _uiState.value = HomeUiState.Unauthorized
                } else {
                    println("❌ Error loading devices: ${error.message}")
                    // One dropped poll must not replace coordinates already on the
                    // map with an error screen — another tick is only DEVICES_POLL_INTERVAL_MS away.
                    if (_uiState.value !is HomeUiState.Success) {
                        _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                    }
                }
            }
        )
    }

    /**
     * Picks a device, or lets go of the one already picked.
     *
     * Tapping the same vehicle again clears the selection, and that is not a nicety: a
     * selected device is the only one the map labels, so with no way to deselect, one
     * tap on a marker stripped the chips off every other vehicle for the rest of the
     * session. The overview sheet used to clear it on dismiss; the sheet is gone, so
     * the tap has to.
     */
    fun selectDevice(device: Device) {
        _selectedDevice.value =
            if (_selectedDevice.value?.deviceId == device.deviceId) null else device
        _showDeviceDropdown.value = false
    }

    fun toggleDeviceDropdown() {
        _showDeviceDropdown.value = !_showDeviceDropdown.value
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
}