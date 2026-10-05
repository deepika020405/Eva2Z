package com.developer.ev2z.ui.screens.device_Report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.LIVE_LOCATION_POLL_INTERVAL_MS
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.repository.DeviceReportRepository
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.DeviceAnalyticsData
import com.developer.ev2z.response.TotalMilagesData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AnalyticsRange(val apiValue: String) {
    TODAY("today"),
    WEEK("week"),
    MONTH("month")
}

class DeviceReportViewModel(
    private val repository: DeviceReportRepository,
    private val platformAddressProvider: PlatformAddressProvider
) : ViewModel() {

    // ── polling jobs ────────────────────────────────────────────────
    private var liveLocationJob: Job? = null
    private var devicesPollingJob: Job? = null

    // ── screen state ────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<DeviceReportUiState>(DeviceReportUiState.Loading)
    val uiState: StateFlow<DeviceReportUiState> = _uiState.asStateFlow()

    private val _myDevices = MutableStateFlow<List<Device>>(emptyList())
    val myDevices: StateFlow<List<Device>> = _myDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice: StateFlow<Device?> = _selectedDevice.asStateFlow()

    private val _liveLocation = MutableStateFlow<LiveLocationResponse?>(null)
    val liveLocation: StateFlow<LiveLocationResponse?> = _liveLocation.asStateFlow()

    private val _totalDevices = MutableStateFlow(0)
    val totalDevices: StateFlow<Int> = _totalDevices.asStateFlow()

    private val _address = MutableStateFlow<String?>(null)
    val address: StateFlow<String?> = _address.asStateFlow()

    // ── mileage + mobilize ──────────────────────────────────────────
    private val _totalMilages = MutableStateFlow<TotalMilagesData?>(null)
    val totalMilages: StateFlow<TotalMilagesData?> = _totalMilages.asStateFlow()

    private val _mobilizeMessage = MutableStateFlow<String?>(null)
    val mobilizeMessage: StateFlow<String?> = _mobilizeMessage.asStateFlow()

    private val _showConfirmationDialog = MutableStateFlow<MobilizeDialogData?>(null)
    val showConfirmationDialog = _showConfirmationDialog.asStateFlow()

    // ── analytics ───────────────────────────────────────────────────
    private val _selectedAnalyticsRange = MutableStateFlow(AnalyticsRange.TODAY)
    val selectedAnalyticsRange: StateFlow<AnalyticsRange> = _selectedAnalyticsRange.asStateFlow()

    private val _deviceAnalytics = MutableStateFlow<DeviceAnalyticsData?>(null)
    val deviceAnalytics: StateFlow<DeviceAnalyticsData?> = _deviceAnalytics.asStateFlow()

    private val _isAnalyticsLoading = MutableStateFlow(false)
    val isAnalyticsLoading: StateFlow<Boolean> = _isAnalyticsLoading.asStateFlow()

    private val _analyticsError = MutableStateFlow<String?>(null)
    val analyticsError: StateFlow<String?> = _analyticsError.asStateFlow()

    data class MobilizeDialogData(
        val command: String,   // "cut" or "restore"
        val terminalId: String
    )

    // ────────────────────────────────────────────────────────────────
    //  DEVICES + LIVE LOCATION
    // ────────────────────────────────────────────────────────────────

    /**
     * Tears the session's state down. This ViewModel is remembered at App root, so it
     * outlives a logout: without this, both polling jobs keep running against the old
     * token and — worse — [_selectedDevice] still holds the previous account's vehicle,
     * so the next user's session polls a device that isn't theirs.
     */
    fun reset() {
        stopPolling()
        _uiState.value = DeviceReportUiState.Loading
        _myDevices.value = emptyList()
        _selectedDevice.value = null
        _liveLocation.value = null
        _totalDevices.value = 0
        _address.value = null
        _totalMilages.value = null
        _mobilizeMessage.value = null
        _showConfirmationDialog.value = null
        _deviceAnalytics.value = null
        _analyticsError.value = null
        _isAnalyticsLoading.value = false
        _selectedAnalyticsRange.value = AnalyticsRange.TODAY
    }

    private fun stopPolling() {
        devicesPollingJob?.cancel()
        devicesPollingJob = null
        liveLocationJob?.cancel()
        liveLocationJob = null
    }

    /**
     * Entry point called from the screen on composition. Kicks off the
     * device-list poll (which auto-selects the first device on cold start
     * and begins live-location polling for it).
     */
    fun fetchMyDevices(token: String) {
        if (_myDevices.value.isEmpty()) {
            _uiState.value = DeviceReportUiState.Loading
        }
        startDevicesPolling(token)
    }

    /**
     * Alias kept for the existing Retry button in DeviceReportScreen.
     */
    fun refreshData(token: String) {
        fetchMyDevices(token)
    }

    /**
     * Polls `getMyDevices` every 15s so the dropdown's engine_status stays
     * in sync with real vehicle state. The first successful response flips
     * ui state to Success immediately — we do NOT wait for live-location to
     * return before showing the UI (that was the old bug).
     */
    private fun startDevicesPolling(token: String) {
        devicesPollingJob?.cancel()
        devicesPollingJob = viewModelScope.launch {
            while (isActive) {
                refreshDevices(token)
                delay(15_000)
            }
        }
    }

    private suspend fun refreshDevices(token: String) {
        try {
            val response = repository.getMyDevices(token)

            if (!response.success) {
                if (_myDevices.value.isEmpty()) {
                    _uiState.value = DeviceReportUiState.Error("Failed to fetch devices")
                }
                return
            }

            _myDevices.value = response.devices
            _totalDevices.value = response.total_devices

            val current = _selectedDevice.value
            if (current == null) {
                // First load — pick the first device and start live polling.
                response.devices.firstOrNull()?.let { first ->
                    _selectedDevice.value = first
                    startLiveLocationPolling(token, first.device_id)
                }
            } else {
                // Refresh — keep the user's selection but update its status.
                val updated = response.devices.firstOrNull {
                    it.device_id == current.device_id
                }
                if (updated != null) {
                    _selectedDevice.value = updated
                }
            }

            // Devices are in hand — render the screen immediately. Don't
            // wait for the first live-location tick like we used to.
            _uiState.value = DeviceReportUiState.Success

        } catch (e: UnauthorizedException) {
            // The session died under us (logout, expiry). Stop instead of looping forever
            // against a dead token — App calls reset() too, this is the backstop.
            println("🔒 [DeviceReport] 401 Unauthorized — stopping polling")
            stopPolling()
        } catch (e: Exception) {
            if (_myDevices.value.isEmpty()) {
                _uiState.value = DeviceReportUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun startLiveLocationPolling(token: String, deviceId: String) {
        liveLocationJob?.cancel()
        liveLocationJob = viewModelScope.launch {
            while (isActive) {
                fetchDeviceLiveLocation(token, deviceId)
                delay(LIVE_LOCATION_POLL_INTERVAL_MS)
            }
        }
    }

    fun fetchDeviceLiveLocation(token: String, deviceId: String) {
        viewModelScope.launch {
            try {
                val response = repository.getDeviceLiveLocation(token, deviceId)
                if (response.success) {
                    _liveLocation.value = response
                    response.data?.latestLocation?.let {
                        getAddressFromLatLng(
                            it.latitude.toDouble(),
                            it.longitude.toDouble()
                        )
                    }
                }
                // Note: we do NOT blank out _liveLocation on a failed tick —
                // that would cause flicker. We keep the last-good location on
                // screen until the next successful poll.
            } catch (e: UnauthorizedException) {
                println("🔒 [DeviceReport] 401 Unauthorized — stopping live-location polling")
                stopPolling()
            } catch (_: Exception) {
                // Silent — live-location failures should never tear down the UI.
            }
        }
    }

    /**
     * User tapped a different device in the dropdown.
     * Clears stale per-device state so the map / address / chip don't
     * briefly show the previous vehicle's data.
     */
    fun selectDevice(device: Device, token: String) {
        if (_selectedDevice.value?.device_id == device.device_id) return

        _selectedDevice.value = device
        _liveLocation.value = null
        _address.value = null

        startLiveLocationPolling(token, device.device_id)
    }

    /**
     * Authoritative status for the currently selected device:
     * prefers the real-time live-location `status` and falls back to the
     * polled `engine_status` from the device list.
     */
    fun effectiveSelectedStatus(): String {
        val live = _liveLocation.value?.data?.latestLocation?.status
        if (!live.isNullOrBlank()) return live
        return _selectedDevice.value?.engine_status?.ifBlank { "offline" } ?: "offline"
    }

    // ── address reverse geocoding ───────────────────────────────────
    fun getAddressFromLatLng(lat: Double, lng: Double) {
        viewModelScope.launch {
            platformAddressProvider.getAddress(lat, lng) { address ->
                _address.value = address ?: "Address not found"
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    //  MILEAGE + MOBILIZE
    // ────────────────────────────────────────────────────────────────

    fun fetchTotalMilages(token: String, terminalId: String) {
        viewModelScope.launch {
            try {
                val response = repository.getTotalMilages(token, terminalId)
                if (response.success && response.data != null) {
                    _totalMilages.value = response.data
                } else {
                    _mobilizeMessage.value = "Failed to fetch total milages"
                }
            } catch (e: Exception) {
                _mobilizeMessage.value = e.message ?: "Unknown error while fetching total milages"
            }
        }
    }

    fun sendMobilizeCommand(token: String, terminalId: String, command: String) {
        viewModelScope.launch {
            try {
                val response = repository.sendMobilizeCommand(token, terminalId, command)
                if (response.success) {
                    _mobilizeMessage.value = response.message
                } else {
                    _mobilizeMessage.value = "Failed to send command"
                }
            } catch (e: Exception) {
                _mobilizeMessage.value = e.message ?: "Unknown error while sending command"
            }
        }
    }

    fun openMobilizeDialog(command: String, terminalId: String) {
        _showConfirmationDialog.value = MobilizeDialogData(command, terminalId)
    }

    fun closeMobilizeDialog() {
        _showConfirmationDialog.value = null
    }

    // ────────────────────────────────────────────────────────────────
    //  DEVICE ANALYTICS
    // ────────────────────────────────────────────────────────────────

    fun fetchDeviceAnalytics(
        token: String,
        deviceId: String,
        range: AnalyticsRange = _selectedAnalyticsRange.value
    ) {
        viewModelScope.launch {
            _isAnalyticsLoading.value = true
            _analyticsError.value = null

            try {
                val response = repository.getDeviceAnalytics(
                    token = token,
                    deviceId = deviceId,
                    range = range.apiValue
                )

                if (response.success && response.data != null) {
                    _deviceAnalytics.value = response.data
                    _selectedAnalyticsRange.value = range
                } else {
                    _analyticsError.value = response.message ?: "Failed to fetch analytics"
                }
            } catch (e: Exception) {
                _analyticsError.value = e.message ?: "Unknown error while fetching analytics"
            } finally {
                _isAnalyticsLoading.value = false
            }
        }
    }

    fun onAnalyticsRangeSelected(token: String, range: AnalyticsRange) {
        val deviceId = _selectedDevice.value?.device_id ?: return
        fetchDeviceAnalytics(token, deviceId, range)
    }

    fun refreshAnalytics(token: String) {
        val deviceId = _selectedDevice.value?.device_id ?: return
        fetchDeviceAnalytics(token, deviceId, _selectedAnalyticsRange.value)
    }

    // ────────────────────────────────────────────────────────────────
    //  LIFECYCLE
    // ────────────────────────────────────────────────────────────────

    override fun onCleared() {
        liveLocationJob?.cancel()
        devicesPollingJob?.cancel()
        super.onCleared()
    }
}

sealed class DeviceReportUiState {
    object Loading : DeviceReportUiState()
    object Success : DeviceReportUiState()
    data class Error(val message: String) : DeviceReportUiState()
}