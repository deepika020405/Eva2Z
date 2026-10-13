package com.developer.ev2z.ui.screens.device_Report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.network.pollAtFixedRate
import com.developer.ev2z.repository.DeviceReportRepository
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.DeviceAnalyticsData
import com.developer.ev2z.response.TotalMilagesData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

enum class AnalyticsRange(val apiValue: String) {
    TODAY("today"),
    WEEK("week"),
    MONTH("month")
}

/**
 * How far the vehicle has to move before the shown address is looked up again. Street
 * granularity — anything smaller resolves to the same text.
 */
private const val ADDRESS_REFRESH_METERS = 40.0

/** Flat-earth distance — accurate enough at this scale and far cheaper than haversine. */
private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val meanLat = (lat1 + lat2) / 2 * PI / 180
    val dLat = (lat2 - lat1) * 111_320.0
    val dLng = (lon2 - lon1) * 111_320.0 * cos(meanLat)
    return sqrt(dLat * dLat + dLng * dLng)
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

    /**
     * Coordinates the address currently on screen was resolved from.
     *
     * Set only once a lookup has actually come back with an address. Recording it up
     * front — which is what this used to do — meant a geocode that failed (no network,
     * the Geocoder service unavailable, which is routine) counted as done: the next
     * forty metres of driving were inside the "already resolved" radius, so the retry
     * never came and the row sat on "Address not found" while the vehicle drove away.
     */
    private var lastGeocodedAt: Pair<Double, Double>? = null

    /** The lookup in flight, so a slow geocode cannot have the next poll start another. */
    private var addressJob: Job? = null

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
        addressJob?.cancel()
        _address.value = null
        // Cleared with the address it belongs to: left behind, it tells the next
        // session's first fix that it has already been resolved, and the row opens
        // blank until the vehicle moves forty metres.
        lastGeocodedAt = null
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
     * Polls `getMyDevices` every [DEVICES_POLL_INTERVAL_MS] so the dropdown's
     * engine_status stays in sync with real vehicle state. The first successful response flips
     * ui state to Success immediately — we do NOT wait for live-location to
     * return before showing the UI (that was the old bug).
     */
    private fun startDevicesPolling(token: String) {
        devicesPollingJob?.cancel()
        devicesPollingJob = viewModelScope.launch {
            pollAtFixedRate(DEVICES_POLL_INTERVAL_MS) { refreshDevices(token) }
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
            // Fixed rate, and the tick is awaited: this loop used to fire a
            // fire-and-forget coroutine every interval regardless of whether the
            // previous request had come back, so on a slow connection two calls were in
            // flight at once and the older response landing last jumped the marker
            // backwards.
            pollAtFixedRate { fetchDeviceLiveLocation(token, deviceId) }
        }
    }

    suspend fun fetchDeviceLiveLocation(token: String, deviceId: String) {
        try {
            val response = repository.getDeviceLiveLocation(token, deviceId)
            if (response.success) {
                _liveLocation.value = response
                // validPosition() drops the "0"/"0" a tracker sends while it has no
                // satellite fix, which would otherwise be geocoded and shown as an
                // address in the Atlantic.
                response.data?.latestLocation?.validPosition()?.let { (lat, lng) ->
                    refreshAddressIfMoved(lat, lng)
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

    /**
     * Reverse-geocodes only once the vehicle has left the area the shown address
     * describes.
     *
     * This used to run on every tick, so a parked vehicle was geocoded every few
     * seconds forever to be told the same street name — work that competes with the
     * live-location requests themselves and, on Android, blocks on a Geocoder call.
     */
    fun refreshAddressIfMoved(lat: Double, lng: Double) {
        val last = lastGeocodedAt
        if (last != null && distanceMeters(last.first, last.second, lat, lng) < ADDRESS_REFRESH_METERS) {
            return
        }
        getAddressFromLatLng(lat, lng)
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
        // Cancelled, not just blanked: a lookup still running for the previous vehicle
        // would otherwise land afterwards and write its street under the new one.
        addressJob?.cancel()
        _address.value = null
        lastGeocodedAt = null

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
    /**
     * Resolves [lat]/[lng] into the street address shown under the vehicle.
     *
     * **Off the main thread.** The Android provider calls `Geocoder.getFromLocation`,
     * which goes to the network and blocks until it answers, and `viewModelScope` is
     * `Dispatchers.Main` — so every refresh froze the UI for as long as the lookup took.
     * That is the same fault as the API calls in `ApiService`, on a screen that then
     * repeats it every poll.
     *
     * A failed lookup keeps whatever address is already on screen rather than replacing
     * a real street with "Address not found": the coordinates have not changed, only
     * the lookup failed, and [lastGeocodedAt] is left alone so the next poll tries
     * again instead of waiting for the vehicle to drive another forty metres.
     */
    fun getAddressFromLatLng(lat: Double, lng: Double) {
        // One at a time: a geocode slower than the poll interval would otherwise have
        // two in flight, and the older answer landing last puts a stale street on screen.
        addressJob?.cancel()
        addressJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                platformAddressProvider.getAddress(lat, lng) { resolved ->
                    if (resolved != null) {
                        lastGeocodedAt = lat to lng
                        _address.value = resolved
                    } else if (_address.value == null) {
                        _address.value = "Address not found"
                    }
                }
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