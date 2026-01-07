package com.developer.ev2z.ui.screens.deviceStatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.repository.DeviceStatusRepository
import com.developer.ev2z.response.DeviceStatusResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.util.getAddressFromLatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

class DeviceStatusViewModel(
    private val repo: DeviceStatusRepository,
    private val tokenProvider: () -> String
) : ViewModel() {

    private val _devices = MutableStateFlow<MyDevicesResponse?>(null)
    val devices = _devices.asStateFlow()



    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _deviceStatus =
        MutableStateFlow<DeviceStatusResponse?>(null)
    val deviceStatus = _deviceStatus.asStateFlow()


    private val _statusFilter = MutableStateFlow("all")
    val statusFilter = _statusFilter.asStateFlow()
    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    private val _addressMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val addressMap = _addressMap.asStateFlow()


    val filteredSessions = combine(
        deviceStatus,
        statusFilter
    ) { statusResponse, filter ->
        val sessions = statusResponse?.data?.sessions ?: emptyList()
        if (filter == "all") sessions
        else sessions.filter { it.status == filter }
    }

    fun setFilter(filter: String) {
        _statusFilter.value = filter
    }


    fun loadDevices() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _devices.value = repo.getMyDevices(
                    token = tokenProvider()
                )
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadDeviceStatus(deviceId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _deviceStatus.value = repo.getDeviceStatus(
                    token = tokenProvider(),
                    deviceId = deviceId
                )
            } finally {
                _loading.value = false
            }
        }
    }

    fun selectDevice(deviceId: String) {
        _selectedDeviceId.value = deviceId
        loadDeviceStatus(deviceId)
    }


    fun resolveAddress(lat: Double, lng: Double) {
        val key = "$lat,$lng"

        // already fetched → skip
        if (_addressMap.value.containsKey(key)) return

        viewModelScope.launch {
            try {
                val address = getAddressFromLatLng(lat, lng)
                _addressMap.value = _addressMap.value + (key to address)
            } catch (e: Exception) {
                _addressMap.value =
                    _addressMap.value + (key to "Location unavailable")
            }
        }
    }


}
