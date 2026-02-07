package com.developer.ev2z.ui.screens.ChangePlan

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
    val activated_at: String? = null,
    val expires_at: String? = null,
    val days_left: Int? = null,      // ✅ ADD THIS
    val status: String? = null
)

/* ================= UI MODELS ================= */

data class PlansUiState(
    val isLoading: Boolean = false,
    val devices: List<DevicePlanUi> = emptyList(),
    val error: String? = null
)

data class DevicePlanUi(
    val deviceId: String,
    val deviceName: String,
    val planType: String,
    val expiryDate: String,
    val daysLeft: Int
)

/* ================= VIEWMODEL ================= */

class ChangePlanViewModel(
    private val apiService: ApiService = ApiService()
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(PlansUiState(isLoading = true))
    val state: StateFlow<PlansUiState> = _state.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        scope.launch {
            try {
                _state.value = PlansUiState(isLoading = true)

                println("🔁 ChangePlan: loading plans")

                // ✅ token automatically picked inside ApiService
                val response = apiService.getUserDevices()

                println("📦 RAW RESPONSE = $response")
                println("📦 DATA SIZE = ${response.data.size}")

                if (response.success) {
                    _state.value = PlansUiState(
                        isLoading = false,
                        devices = response.data.map { it.toUiModel() }
                    )
                } else {
                    _state.value = PlansUiState(
                        isLoading = false,
                        devices = emptyList(),
                        error = "No plans found"
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = PlansUiState(
                    isLoading = false,
                    error = e.message ?: "Failed to load plans"
                )
            }
        }
    }

    fun renewPlan(deviceId: String) {
        println("🔄 Renew plan for deviceId=$deviceId")
    }

    fun changePlan(deviceId: String) {
        println("🔁 Change plan for deviceId=$deviceId")
    }
}

/* ================= MAPPER ================= */

private fun UserDeviceDto.toUiModel(): DevicePlanUi {

    return DevicePlanUi(
        deviceId = device_id.toString(),

        // Backend me device ka naam nahi aa raha
        // isliye device_id se derive ho raha (hardcoded nahi, derived)
        deviceName = "Device #$device_id",

        // Backend status se plan type
        planType = status ?: "unknown",

        // Backend expiry date (safe parsing)
        expiryDate = expires_at
            ?.substringBefore(" ")
            ?: "N/A",

        // ✅ PURE backend value
        daysLeft = days_left ?: 0
    )
}

