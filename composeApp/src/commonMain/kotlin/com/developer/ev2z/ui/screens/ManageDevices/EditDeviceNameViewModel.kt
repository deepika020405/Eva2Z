// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/ManageDevices/EditDeviceNameViewModel.kt
package com.developer.ev2z.ui.screens.ManageDevices

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

data class EditDeviceNameUiState(
    val isLoading: Boolean = false,
    val updateSuccess: Boolean = false,
    val error: String? = null
)

class EditDeviceNameViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditDeviceNameUiState())
    val uiState: StateFlow<EditDeviceNameUiState> = _uiState.asStateFlow()

    fun updateDeviceName(token: String, deviceId: String, newName: String) {
        if (token.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Authentication token not found"
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val body = buildJsonObject {
                    put("device_id", deviceId)
                    put("device_name", capitalizeWords(newName))
                }

                println("🔹 UpdateDevice Request:")
                println("   Token: Bearer $token")
                println("   Body: $body")

                val response = apiService.updateDevice("Bearer $token", body)

                println("🔹 UpdateDevice Response: $response")

                val success = response["success"]?.jsonPrimitive?.boolean ?: false
                val message = response["message"]?.jsonPrimitive?.content ?: ""

                if (success) {
                    println("✅ Update Success: $message")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        updateSuccess = true,
                        error = null
                    )
                } else {
                    println("❌ Update Failed: $message")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = message
                    )
                }

            } catch (e: Exception) {
                println("❌ UpdateDevice Exception: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update device"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun capitalizeWords(input: String): String {
        return input.trim()
            .lowercase()
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
}