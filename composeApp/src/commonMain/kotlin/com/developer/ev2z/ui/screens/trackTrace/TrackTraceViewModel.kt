package com.developer.ev2z.ui.screens.trackTrace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.LIVE_LOCATION_POLL_INTERVAL_MS
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.repository.TrackTraceRepository
import com.developer.ev2z.response.Device
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Simple lat/lng data class for trail points
data class TrailPoint(val latitude: Double, val longitude: Double)

@OptIn(kotlin.time.ExperimentalTime::class)
class TrackTraceViewModel(
    private val repository: TrackTraceRepository,
    private val tokenProvider: () -> String
) : ViewModel() {

    private var liveLocationJob: Job? = null
    private var devicesPollingJob: Job? = null

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    private val _liveLocation = MutableStateFlow<LiveLocationResponse?>(null)
    val liveLocation = _liveLocation.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _unauthorized = MutableStateFlow(false)
    val unauthorized = _unauthorized.asStateFlow()

    private val _trailPoints = MutableStateFlow<List<TrailPoint>>(emptyList())
    val trailPoints = _trailPoints.asStateFlow()

    fun start() {
        // First-time load shows a spinner; refreshes are silent.
        if (_devices.value.isEmpty()) {
            _isLoading.value = true
        }
        startDevicesPolling()
    }

    fun stop() {
        liveLocationJob?.cancel()
        devicesPollingJob?.cancel()
        liveLocationJob = null
        devicesPollingJob = null
    }

    fun selectDevice(device: Device) {
        if (_selectedDevice.value?.device_id == device.device_id) return
        _selectedDevice.value = device
        _liveLocation.value = null
        _trailPoints.value = emptyList()
        startLiveLocationPolling(device.device_id)
    }

    private suspend fun loadHistoricalTrail(deviceId: String) {
        try {
            val today = Instant.fromEpochMilliseconds(getTimeMillis())
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()
            val token = tokenProvider()
            println("📍 [TrackTrace] Loading today's trail for $deviceId date=$today")
            val result = repository.getTodayRouteHistory(token, deviceId, today)
            if (result.success) {
                val points = result.data
                    .filter { it.isValid() }
                    .mapNotNull { p ->
                        val lat = p.getLatDouble() ?: return@mapNotNull null
                        val lng = p.getLngDouble() ?: return@mapNotNull null
                        TrailPoint(lat, lng)
                    }
                println("📍 [TrackTrace] Historical trail: ${points.size} valid points")
                if (points.isNotEmpty()) {
                    _trailPoints.value = points.takeLast(100)
                }
            }
        } catch (e: Exception) {
            println("💥 [TrackTrace] History trail load failed: ${e.message}")
        }
    }

    private fun startDevicesPolling() {
        devicesPollingJob?.cancel()
        devicesPollingJob = viewModelScope.launch {
            while (isActive) {
                refreshDevices()
                delay(15_000)
            }
        }
    }

    private suspend fun refreshDevices() {
        try {
            val token = tokenProvider()
            println("🔑 [TrackTrace] Token used: ${if (token.isBlank()) "EMPTY ❌" else token.take(20) + "..."}")
            val res = repository.getMyDevices(token)
            println("📦 [TrackTrace] API success=${res.success}, devices=${res.devices.size}, username=${res.username}")
            res.devices.forEachIndexed { i, d ->
                println("  [$i] device_id=${d.device_id} name=${d.device_name} status=${d.engine_status}")
            }
            if (!res.success) {
                println("❌ [TrackTrace] API returned success=false")
                if (_devices.value.isEmpty()) {
                    _error.value = "Failed to load devices"
                }
                return
            }

            _username.value = res.username.orEmpty()
            _devices.value = res.devices
            _error.value = null

            val current = _selectedDevice.value
            if (current == null) {
                // First load — auto-select first device, load history, and start live polling.
                res.devices.firstOrNull()?.let { first ->
                    _selectedDevice.value = first
                    startLiveLocationPolling(first.device_id)
                }
            } else {
                // Refresh — preserve selection; update the Device object so
                // engine_status stays current for the selected row.
                val updated = res.devices.firstOrNull { it.device_id == current.device_id }
                if (updated != null) {
                    _selectedDevice.value = updated
                }
            }
        } catch (e: UnauthorizedException) {
            println("🔒 [TrackTrace] 401 Unauthorized — stopping polling")
            stop()
            _unauthorized.value = true
        } catch (e: Exception) {
            println("💥 [TrackTrace] Exception in refreshDevices: ${e::class.simpleName} — ${e.message}")
            if (_devices.value.isEmpty()) {
                _error.value = e.message ?: "Failed to load devices"
            }
        } finally {
            _isLoading.value = false
        }
    }

    private fun startLiveLocationPolling(deviceId: String) {
        liveLocationJob?.cancel()
        liveLocationJob = viewModelScope.launch {
            while (isActive) {
                fetchLiveLocation(deviceId)
                delay(LIVE_LOCATION_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun fetchLiveLocation(deviceId: String) {
        try {
            println("📍 [TrackTrace] Fetching live location for deviceId=$deviceId")
            val res = repository.getLiveLocation(tokenProvider(), deviceId)
            println("📍 [TrackTrace] LiveLocation success=${res.success} data=${res.data}")
            if (!res.success) {
                println("❌ [TrackTrace] LiveLocation returned success=false")
                return
            }

            _liveLocation.value = res

            val lat = res.data?.latestLocation?.latitude?.toDoubleOrNull()
            val lng = res.data?.latestLocation?.longitude?.toDoubleOrNull()

            if (lat != null && lng != null) {
                val newPoint = TrailPoint(lat, lng)
                val current = _trailPoints.value
                if (current.lastOrNull() != newPoint) {
                    _trailPoints.value = (current + newPoint).takeLast(100)
                }
            }
        } catch (e: UnauthorizedException) {
            println("🔒 [TrackTrace] 401 Unauthorized (live location) — stopping polling")
            stop()
            _unauthorized.value = true
        } catch (e: Exception) {
            println("💥 [TrackTrace] Exception in fetchLiveLocation: ${e::class.simpleName} — ${e.message}")
        }
    }

    /**
     * The authoritative status for the selected device:
     * prefers the live location's `status` field (real-time) and
     * falls back to the device list's `engine_status` snapshot.
     */
    fun effectiveSelectedStatus(): String {
        val live = _liveLocation.value?.data?.latestLocation?.status
        if (!live.isNullOrBlank()) return live
        return _selectedDevice.value?.engine_status?.ifBlank { "offline" } ?: "offline"
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}