// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/AddDevice/AddDeviceViewModel.kt
package com.developer.ev2z.ui.screens.AddDevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AddDeviceViewModel : ViewModel() {

    private val apiService = ApiService()

    private val _scannedDeviceId = MutableStateFlow("")
    val scannedDeviceId: StateFlow<String> = _scannedDeviceId.asStateFlow()

    private val _scannedSimNumber = MutableStateFlow("")
    val scannedSimNumber: StateFlow<String> = _scannedSimNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _addDeviceResult = MutableStateFlow<Result<String>?>(null)
    val addDeviceResult: StateFlow<Result<String>?> = _addDeviceResult.asStateFlow()

    fun setDeviceId(id: String) {
        _scannedDeviceId.value = id
    }

    fun setSimNumber(sim: String) {
        _scannedSimNumber.value = sim
    }

    fun clearData() {
        _scannedDeviceId.value = ""
        _scannedSimNumber.value = ""
        _addDeviceResult.value = null
    }

    fun addDevice(
        token: String,
        deviceName: String,
        deviceId: String,
        simNumber: String
    ) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val deviceIdSave = "0$deviceId"
                val simNumSave = if (simNumber.endsWith("U")) simNumber else "${simNumber}U"

                val body = buildJsonObject {
                    put("device_id", deviceIdSave)
                    put("device_name", deviceName)
                    put("sim_number", simNumSave)
                }

                val result = apiService.addDevice(token, body)
                _isLoading.value = false
                _addDeviceResult.value = Result.success("Device Added Successfully")

            } catch (e: Exception) {
                _isLoading.value = false
                _addDeviceResult.value = Result.failure(e)
            }
        }
    }
}