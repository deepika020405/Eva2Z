package com.developer.ev2z.ui.screens.ManageDevices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManageDeviceUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleteSuccess: Boolean = false,
    val mobilizeSuccess: Boolean = false,
    val successMessage: String? = null
)

class ManageDeviceViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageDeviceUiState())
    val uiState: StateFlow<ManageDeviceUiState> = _uiState.asStateFlow()

    fun loadDevices(token: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val response = apiService.getMyDevices("Bearer $token")

                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        devices = response.devices,
                        isLoading = false,
                        error = null
                    )
                    println("Devices loaded: ${response.devices.size}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to fetch devices"
                    )
                }
            } catch (e: Exception) {
                println("❌ ViewModel Error: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun deleteDevice(token: String, deviceId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val response = apiService.deleteDevice(deviceId, "Bearer $token")

                println("Device deleted successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    deleteSuccess = true,
                    successMessage = "Device deleted successfully"
                )

                // Refresh the device list
                loadDevices(token)

            } catch (e: Exception) {
                println("❌ Delete Error: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete device: ${e.message}"
                )
            }
        }
    }

    fun mobilizeDevice(token: String, deviceId: String, command: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val response = apiService.sendMobilizeCommand(
//                val response = apiService.mobilizeDevice(
                    token = "Bearer $token",
                    terminalId = deviceId,
                    command = command
                )

                val action = if (command == "cut") "turned off" else "turned on"
                println("Device $action successfully")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mobilizeSuccess = true,
                    successMessage = "Device $action successfully"
                )

                // Refresh the device list
                loadDevices(token)

            } catch (e: Exception) {
                println("Mobilize Error: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to execute command: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearDeleteSuccess() {
        _uiState.value = _uiState.value.copy(deleteSuccess = false, successMessage = null)
    }

    fun clearMobilizeSuccess() {
        _uiState.value = _uiState.value.copy(mobilizeSuccess = false, successMessage = null)
    }
}