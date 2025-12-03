// ui/screens/homeScreen/HomeViewModel.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val deviceResponse: DeviceResponse) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
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
    var isRefreshing = MutableStateFlow(false)

    private val _showDeviceDropdown = MutableStateFlow(false)
    val showDeviceDropdown: StateFlow<Boolean> = _showDeviceDropdown.asStateFlow()

    fun loadDevices(token: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            repository.getDevicesLatestLocations(token).fold(
                onSuccess = { response ->
                    _uiState.value = HomeUiState.Success(response)
                    println("✅ Devices loaded: ${response.totalDevices}")
                    println("✅ Response Body: ${response.toString()}")
                },
                onFailure = { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                    println("❌ Error loading devices: ${error.message}")
                }
            )
        }
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
        loadDevices(token)
    }
}