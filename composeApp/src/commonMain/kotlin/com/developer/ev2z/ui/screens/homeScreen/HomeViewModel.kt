// ui/screens/homeScreen/HomeViewModel.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.network.pollAtFixedRate
import com.developer.ev2z.repository.HomeRepository
import com.developer.ev2z.util.IgnitionAlerts
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
            // Delay first: loadDevices has already made the opening request, so firing
            // one here too would just double it.
            delay(DEVICES_POLL_INTERVAL_MS)
            // pollAtFixedRate rather than a bare while/delay loop, for what the loop
            // carries with it: it waits out a server throttle instead of hammering
            // through one, keeps the cadence from drifting by the request's latency, and
            // survives a tick that throws. A hand-rolled loop here ended permanently on
            // the first exception, and nothing restarts it.
            pollAtFixedRate(DEVICES_POLL_INTERVAL_MS) {
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
                // `success=false` is not a fleet, and must never be drawn as one.
                //
                // An account with no devices answers 200 with `success=true` and an
                // empty list, so this branch is only ever reached by a body that is not
                // a fleet reply at all — the 429 throttle page being the one seen in the
                // wild. DeviceResponse defaults every field, so such a body decodes into
                // a valid-looking response with zero devices; publishing it wiped every
                // marker off the map and read as GPS tracking turning itself off.
                // ApiService now fails the call outright (see RateLimit.kt); this is the
                // second line of defence, for a 200 that still says success=false.
                if (!response.success) {
                    println("⚠️ Fleet poll answered success=false — keeping the last known positions")
                    if (_uiState.value !is HomeUiState.Success) {
                        _uiState.value = HomeUiState.Error("Could not load devices")
                    }
                    return@fold
                }

                // Status comes from whether the coordinates moved, not from the
                // server's field. The tracker holds the reference position across polls
                // so the comparison always spans MOVEMENT_WINDOW_MS.
                val nowMs = getTimeMillis()
                val withStatus = movementTracker.apply(response, nowMs)

                // The Moving / Parked notifications, raised by the app itself so they
                // arrive whether or not a push does. Shared with the background check
                // rather than owned here, so the two cannot each announce the same spell.
                //
                // Fed the server's own words, not `withStatus`: the tracker answers one
                // question — moving or standing still — and collapses idle and parked
                // into "stopped" on the way.
                //
                // The ignition lookup is what tells an idling vehicle from a parked one:
                // this response calls an engine-on standstill "parked". Idle carries no
                // banner of its own, but telling the two apart is what keeps the Parked
                // banner honest — it waits for the engine to go off instead of firing at
                // the first red light. Only spent on a vehicle that is standing still,
                // and cached; the session fallback behind it covers the trackers whose
                // ignition line reads nothing at all. See IdleStatusResolver.
                VehicleStatusAlerts.onPoll(
                    response,
                    sessionKindOf = { repository.getDeviceLatestSessionStatus(token, it) },
                ) {
                    repository.getDeviceIgnition(token, it)
                }

                // Engine on/off, from `ignition-log/{id}` rather than inferred from the
                // fleet's "parked". Called every five seconds, taking the vehicles in
                // turn — see IGNITION_DEVICES_PER_TICK: this endpoint is per device, so
                // reading the whole fleet every tick would be four times the request rate
                // of everything else in the app put together.
                IgnitionAlerts.onFleetPoll(response.data) {
                    repository.getDeviceAlarms(token, it)
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
                    // the previous session's idle spells — or its engine events, which
                    // would otherwise announce the last account's ignition to the new one.
                    VehicleStatusAlerts.reset()
                    IgnitionAlerts.reset()
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