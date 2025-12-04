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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean

class AddDeviceViewModel : ViewModel() {

    private val apiService = ApiService()

    private val _scannedDeviceId = MutableStateFlow("")
    val scannedDeviceId: StateFlow<String> = _scannedDeviceId.asStateFlow()

    private val _scannedSimNumber = MutableStateFlow("")
    val scannedSimNumber: StateFlow<String> = _scannedSimNumber.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _addDeviceResult = MutableStateFlow<AddDeviceResult?>(null)
    val addDeviceResult: StateFlow<AddDeviceResult?> = _addDeviceResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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
        _errorMessage.value = null
    }

    // ✅ Added deviceType parameter
    fun addDevice(
        token: String,
        deviceName: String,
        deviceId: String,
        simNumber: String,
        deviceType: String // ✅ New parameter
    ) {
        if (token.isEmpty() || token == "YOUR_TOKEN_HERE") {
            _errorMessage.value = "Authentication token not found. Please login again."
            println("❌ No valid token found")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val deviceIdSave = if (deviceId.startsWith("0")) deviceId else "0$deviceId"
                val simNumSave = if (simNumber.endsWith("U")) simNumber else "${simNumber}U"

                val body = buildJsonObject {
                    put("device_id", deviceIdSave)
                    put("device_name", deviceName)
                    put("sim_number", simNumSave)
                    put("device_type", deviceType) // ✅ Send device type
                }

                println("🔹 AddDevice Request:")
                println("   Token: Bearer $token")
                println("   Body: $body")

                val response = apiService.addDevice("Bearer $token", body)

                println("🔹 AddDevice Response: $response")

                _isLoading.value = false

                val success = response["success"]?.jsonPrimitive?.boolean ?: false
                val message = response["message"]?.jsonPrimitive?.content ?: ""

                if (success) {
                    println("✅ API Success: $message")
                    _addDeviceResult.value = AddDeviceResult.Success(
                        deviceName = deviceName,
                        deviceId = deviceIdSave,
                        simNumber = simNumSave,
                        deviceType = deviceType // ✅ Include in result
                    )
                } else {
                    println("❌ API Failed: $message")
                    _errorMessage.value = message
                    _addDeviceResult.value = AddDeviceResult.Error(message)
                }

            } catch (e: Exception) {
                _isLoading.value = false
                val errorMsg = e.message ?: "Unknown error occurred"
                _errorMessage.value = errorMsg
                _addDeviceResult.value = AddDeviceResult.Error(errorMsg)
                println("❌ AddDevice Exception: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}

// ✅ Updated sealed class
sealed class AddDeviceResult {
    data class Success(
        val deviceName: String,
        val deviceId: String,
        val simNumber: String,
        val deviceType: String // ✅ Added
    ) : AddDeviceResult()

    data class Error(val message: String) : AddDeviceResult()
}