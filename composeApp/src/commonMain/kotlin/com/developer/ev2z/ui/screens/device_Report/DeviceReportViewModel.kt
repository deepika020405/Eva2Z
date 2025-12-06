package com.developer.ev2z.ui.screens.device_Report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.repository.DeviceReportRepository
import com.developer.ev2z.response.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceReportViewModel(
    private val repository: DeviceReportRepository,
    private val platformAddressProvider: PlatformAddressProvider   // 👈 NEW
) : ViewModel() {

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

    // 👇 NEW: address state
    private val _address = MutableStateFlow<String?>(null)
    val address: StateFlow<String?> = _address.asStateFlow()

    fun fetchMyDevices(token: String) {
        viewModelScope.launch {
            _uiState.value = DeviceReportUiState.Loading
            try {
                val response = repository.getMyDevices(token)
                if (response.success) {
                    _myDevices.value = response.devices
                    _totalDevices.value = response.total_devices

                    if (response.devices.isNotEmpty()) {
                        _selectedDevice.value = response.devices.first()
                        fetchDeviceLiveLocation(token, response.devices.first().device_id)
                    } else {
                        _uiState.value = DeviceReportUiState.Success
                    }
                } else {
                    _uiState.value = DeviceReportUiState.Error("Failed to fetch devices")
                }
            } catch (e: Exception) {
                _uiState.value = DeviceReportUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun fetchDeviceLiveLocation(token: String, deviceId: String) {
        viewModelScope.launch {
            try {
                val response = repository.getDeviceLiveLocation(token, deviceId)
                if (response.success) {
                    _liveLocation.value = response
                    _uiState.value = DeviceReportUiState.Success

                    // jaise hi live location aaye, address nikaal lo
                    response.data?.latestLocation?.let {
                        getAddressFromLatLng(it.latitude.toDouble(), it.longitude.toDouble())
                    }
                } else {
                    _uiState.value = DeviceReportUiState.Error("Failed to fetch live location")
                }
            } catch (e: Exception) {
                _uiState.value = DeviceReportUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun selectDevice(device: Device, token: String) {
        _selectedDevice.value = device
        fetchDeviceLiveLocation(token, device.device_id)
    }

    fun refreshData(token: String) {
        fetchMyDevices(token)
    }

    // 👇 NEW: reverse geocode helper
    fun getAddressFromLatLng(lat: Double, lng: Double) {
        viewModelScope.launch {
            platformAddressProvider.getAddress(lat, lng) { address ->
                _address.value = address ?: "Address not found"
            }
        }
    }
}

sealed class DeviceReportUiState {
    object Loading : DeviceReportUiState()
    object Success : DeviceReportUiState()
    data class Error(val message: String) : DeviceReportUiState()
}