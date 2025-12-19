package com.developer.ev2z.ui.screens.device_Report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.repository.DeviceReportRepository
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.DeviceAnalyticsData
import com.developer.ev2z.response.TotalMilagesData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AnalyticsRange(val apiValue: String) {
    TODAY("today"),
    WEEK("week"),
    MONTH("month")
}

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

    // 👉 total milage state
    private val _totalMilages = MutableStateFlow<TotalMilagesData?>(null)
    val totalMilages: StateFlow<TotalMilagesData?> = _totalMilages.asStateFlow()

    // 👉 engine cut/restore command state
    private val _mobilizeMessage = MutableStateFlow<String?>(null)
    val mobilizeMessage: StateFlow<String?> = _mobilizeMessage.asStateFlow()

    private val _showConfirmationDialog = MutableStateFlow<MobilizeDialogData?>(null)
    val showConfirmationDialog = _showConfirmationDialog.asStateFlow()



    private val _selectedAnalyticsRange =
        MutableStateFlow(AnalyticsRange.TODAY)
    val selectedAnalyticsRange: StateFlow<AnalyticsRange> =
        _selectedAnalyticsRange.asStateFlow()

    private val _deviceAnalytics =
        MutableStateFlow<DeviceAnalyticsData?>(null)
    val deviceAnalytics: StateFlow<DeviceAnalyticsData?> =
        _deviceAnalytics.asStateFlow()

    private val _isAnalyticsLoading = MutableStateFlow(false)
    val isAnalyticsLoading: StateFlow<Boolean> =
        _isAnalyticsLoading.asStateFlow()

    private val _analyticsError = MutableStateFlow<String?>(null)
    val analyticsError: StateFlow<String?> = _analyticsError.asStateFlow()


    data class MobilizeDialogData(
        val command: String,   // "cut" or "restore"
        val terminalId: String
    )

    fun fetchMyDevices(token: String) {
        viewModelScope.launch {
            _uiState.value = DeviceReportUiState.Loading
            try {
                val response = repository.getMyDevices(token)
                if (response.success) {
                    _myDevices.value = response.devices
                    _totalDevices.value = response.total_devices
                    println(response.toString())

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
                    range = range.apiValue   // "today" / "week" / "month"
                )

                if (response.success && response.data != null) {
                    _deviceAnalytics.value = response.data
                    _selectedAnalyticsRange.value = range
                } else {
                    _analyticsError.value =
                        response.message ?: "Failed to fetch analytics"
                }
            } catch (e: Exception) {
                _analyticsError.value =
                    e.message ?: "Unknown error while fetching analytics"
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

}

sealed class DeviceReportUiState {
    object Loading : DeviceReportUiState()
    object Success : DeviceReportUiState()
    data class Error(val message: String) : DeviceReportUiState()
}