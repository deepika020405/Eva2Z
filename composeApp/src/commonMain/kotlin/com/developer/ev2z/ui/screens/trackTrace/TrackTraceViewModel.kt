package com.developer.ev2z.ui.screens.trackTrace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.repository.TrackTraceRepository
import com.developer.ev2z.response.Device
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Simple lat/lng data class for trail points
data class TrailPoint(val latitude: Double, val longitude: Double)

class TrackTraceViewModel(
    private val repository: TrackTraceRepository,
    private val tokenProvider: () -> String
) : ViewModel() {

    private var pollingJob: Job? = null

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    private val _liveLocation = MutableStateFlow<LiveLocationResponse?>(null)
    val liveLocation = _liveLocation.asStateFlow()

    private val _username = MutableStateFlow<String>("")
    val username = _username.asStateFlow()

    // ✅ Trail history — max 100 points rakhenge (memory safe)
    private val _trailPoints = MutableStateFlow<List<TrailPoint>>(emptyList())
    val trailPoints = _trailPoints.asStateFlow()

    fun loadDevices() {
        viewModelScope.launch {
            val res = repository.getMyDevices(tokenProvider())
            if (res.success) {
                _username.value = res.username.orEmpty()
                if (res.devices.isNotEmpty()) {
                    _devices.value = res.devices
                    selectDevice(res.devices.first())
                }
            }
        }
    }

    fun selectDevice(device: Device) {
        _selectedDevice.value = device
        // ✅ Device change hone pe trail reset karo
        _trailPoints.value = emptyList()
        startPolling(device.device_id)
    }

    private fun startPolling(deviceId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchLiveLocation(deviceId)
                delay(10_000)
            }
        }
    }

    private suspend fun fetchLiveLocation(deviceId: String) {
        try {
            val res = repository.getLiveLocation(tokenProvider(), deviceId)
            if (res.success) {
                _liveLocation.value = res

                // ✅ Naya point trail mein add karo
                val lat = res.data?.latestLocation?.latitude?.toDoubleOrNull()
                val lng = res.data?.latestLocation?.longitude?.toDoubleOrNull()

                if (lat != null && lng != null) {
                    val newPoint = TrailPoint(lat, lng)
                    val current = _trailPoints.value

                    // Duplicate point mat add karo (device parked ho toh)
                    if (current.lastOrNull() != newPoint) {
                        _trailPoints.value = (current + newPoint)
                            .takeLast(100) // max 100 points
                    }
                }
            }
        } catch (_: Exception) { }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }
}