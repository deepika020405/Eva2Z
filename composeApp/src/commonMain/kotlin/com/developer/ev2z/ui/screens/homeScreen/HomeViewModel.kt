// ui/screens/homeScreen/HomeViewModel.kt
package com.developer.ev2z.ui.screens.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.repository.HomeRepository
import com.developer.ev2z.util.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _showOverview = MutableStateFlow(false)
    val showOverview: StateFlow<Boolean> = _showOverview.asStateFlow()
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()


    private val _showDeviceDropdown = MutableStateFlow(false)
    val showDeviceDropdown: StateFlow<Boolean> = _showDeviceDropdown.asStateFlow()

    fun loadDevices(token: String) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            repository.getDevicesLatestLocations(token).fold(
                onSuccess = { response ->
                    _uiState.value = HomeUiState.Success(response)
                    // First proof this run that the stored token is still good. FCM registration
                    // waits on this so a revoked session can never re-register the device.
                    SessionState.markVerified()
                    println("✅ Devices loaded: ${response.totalDevices}")
                    println("✅ Response Body: ${response.toString()}")
                },
                onFailure = { error ->
                    if (error is UnauthorizedException) {
                        println("🔒 [Home] 401 Unauthorized — redirecting to login")
                        SessionState.invalidate()
                        _uiState.value = HomeUiState.Unauthorized
                    } else {
                        _uiState.value = HomeUiState.Error(error.message ?: "Unknown error")
                        println("❌ Error loading devices: ${error.message}")
                    }
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
        if (_isRefreshing.value) return // 👈 double click block

        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadDevices(token) // tumhari existing API call
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}