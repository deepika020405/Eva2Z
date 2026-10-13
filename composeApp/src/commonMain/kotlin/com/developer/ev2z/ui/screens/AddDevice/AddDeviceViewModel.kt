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

    /** The vehicle catalogue behind the "Vehicle Type Icon" picker. */
    private val _vehicleTypes = MutableStateFlow<List<VehicleType>>(emptyList())
    val vehicleTypes: StateFlow<List<VehicleType>> = _vehicleTypes.asStateFlow()

    private val _isLoadingVehicleTypes = MutableStateFlow(false)
    val isLoadingVehicleTypes: StateFlow<Boolean> = _isLoadingVehicleTypes.asStateFlow()

    /**
     * Loads the vehicle catalogue. Separate from [errorMessage] so a catalogue that
     * fails to load does not read as an add-device failure; the picker says so itself
     * and offers a retry.
     */
    private val _vehicleTypesError = MutableStateFlow<String?>(null)
    val vehicleTypesError: StateFlow<String?> = _vehicleTypesError.asStateFlow()

    fun loadVehicleTypes(token: String) {
        if (_isLoadingVehicleTypes.value || _vehicleTypes.value.isNotEmpty()) return

        _isLoadingVehicleTypes.value = true
        _vehicleTypesError.value = null

        viewModelScope.launch {
            try {
                val response = apiService.getVehicleTypes(token)
                println("🚗 Vehicle types: ${response.data.size} of ${response.total}")
                _vehicleTypes.value = response.data
                if (response.data.isEmpty()) {
                    _vehicleTypesError.value = "No vehicle types available"
                }
            } catch (e: Exception) {
                println("❌ Vehicle types failed: ${e.message}")
                _vehicleTypesError.value = "Couldn't load vehicle types"
            } finally {
                _isLoadingVehicleTypes.value = false
            }
        }
    }

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
        deviceType: String, // ✅ New parameter
        /**
         * The picked [VehicleType]'s id. Required by the server — an add-device with
         * no `vehicle_id` is rejected 422 "The vehicle id field is required" — and
         * sent as a **string**: the same validator rejects the number with "The
         * vehicle id field must be a string".
         */
        vehicleId: Int
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

                // The SIM label prints 19 or 20 digits with a trailing "U", and the
                // "U" is always sent — 20 or 21 characters on the wire.
                //
                // This REQUIRES `sim_number` to be validated max:21 server-side. While
                // the rule is max:20, a 20-digit SIM is rejected with
                // "The sim number field must not be greater than 20 characters"; that
                // 422 is a backend limit, not a scanning or input bug.
                val simDigits = simNumber.trim().removeSuffix("U")
                val simNumSave = "${simDigits}U"

                val body = buildJsonObject {
                    put("device_id", deviceIdSave)
                    put("device_name", deviceName)
                    put("sim_number", simNumSave)
                    put("device_type", deviceType) // ✅ Send device type
                    put("vehicle_id", vehicleId.toString())
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