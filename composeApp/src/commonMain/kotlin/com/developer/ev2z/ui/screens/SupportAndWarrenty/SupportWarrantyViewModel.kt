package com.developer.ev2z.ui.screens.SupportAndWarrenty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.ui.screens.ChangePlan.UserDeviceDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupportWarrantyUi(
    val productName: String,
    val deviceId: String,          // IMEI / Serial
    val warrantyStatus: WarrantyStatus,
    val warrantyExpiryDate: String
)

enum class WarrantyStatus {
    ACTIVE,
    EXPIRED
}

class WarrantyViewModel(
    private val apiService: ApiService = ApiService()
) : ViewModel() {

    private val _state = MutableStateFlow<SupportWarrantyUi?>(null)
    val state: StateFlow<SupportWarrantyUi?> = _state.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)  // 👈 add this
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    fun loadWarranty(deviceId: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getUserDevices()

                if (response.success) {
                    val device = response.data
                        .firstOrNull { it.device_id.toString() == deviceId }
                    device?.let {
                        _state.value = it.toWarrantyUiModel()
                    }
                }

            } catch (e: Exception) {
                println("❌ Warranty load error: ${e.message}")
            } finally {
                _isLoaded.value = true  // 👈 always mark as done
            }
        }
    }
}

private fun UserDeviceDto.toWarrantyUiModel(): SupportWarrantyUi {
    return SupportWarrantyUi(
        productName = "Device #$device_id",
        deviceId = device_number, // ✅ show IMEI

        warrantyStatus =
            if (warranty_status == "active")
                WarrantyStatus.ACTIVE
            else
                WarrantyStatus.EXPIRED,

        warrantyExpiryDate =
            warranty_expiry ?: "Not Purchased"
    )
}