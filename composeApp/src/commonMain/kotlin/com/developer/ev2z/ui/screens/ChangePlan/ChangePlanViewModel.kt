package com.developer.ev2z.ui.screens.ChangePlan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.util.AppTokenProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

/* ================= BACKEND MODELS ================= */

@Serializable
data class UserDevicesResponse(
    val success: Boolean,
    val data: List<UserDeviceDto>
)

@Serializable
data class UserDeviceDto(
    val device_id: Int,
    val device_number: String,
    val sim_number: String? = null,

    val activated_at: String? = null,
    val plan_days_left: Int? = null,
    val expires_at: String? = null,
    val plan_status: String? = null,

    val warranty_start: String? = null,
    val warranty_expiry: String? = null,
    val warranty_status: String? = null
)

/* ================= UI MODELS ================= */

data class PlansUiState(
    val isLoading: Boolean = false,
    val devices: List<DevicePlanUi> = emptyList(),
    val error: String? = null
)

data class DevicePlanUi(
    val deviceId: String,    // DB id (247)
    val imei: String,        // ✅ REAL IMEI
    val deviceName: String,
    val simNumber: String?,   // ✅ ADD THIS
    val planType: String,
    val expiryDate: String,
    val daysLeft: Int
)

/* ================= VIEWMODEL ================= */

class ChangePlanViewModel(
    private val apiService: ApiService = ApiService()
) : ViewModel() {

    private val _state = MutableStateFlow(PlansUiState(isLoading = true))
    val state: StateFlow<PlansUiState> = _state.asStateFlow()
    private val _phonePeToken = MutableStateFlow<String?>(null)
    val phonePeToken = _phonePeToken.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        viewModelScope.launch {
            try {
                _state.value = PlansUiState(isLoading = true)

                val response = apiService.getUserDevices()

                if (response.success) {
                    _state.value = PlansUiState(
                        isLoading = false,
                        devices = response.data.map { it.toPlanUiModel() }                    )
                } else {
                    _state.value = PlansUiState(
                        isLoading = false,
                        error = "No devices found"
                    )
                }

            } catch (e: Exception) {
                _state.value = PlansUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load plans"
                )
            }
        }
    }

    fun renewPlan(deviceId: String, duration: String) {
        println("🔄 Renew device=$deviceId duration=$duration")
        // 👉 yahin baad me /device/renew call hoga
    }
}
/* ================= MAPPER ================= */

private fun UserDeviceDto.toPlanUiModel(): DevicePlanUi {
    return DevicePlanUi(
        deviceId = device_id.toString(),
        imei = device_number, // ✅ real IMEI
        simNumber = sim_number,   // ✅ PASS FROM API
        deviceName = "Device #$device_id",
        planType = plan_status ?: "unknown",
        expiryDate = expires_at?.substringBefore(" ") ?: "N/A",
        daysLeft = plan_days_left ?: 0
    )
}