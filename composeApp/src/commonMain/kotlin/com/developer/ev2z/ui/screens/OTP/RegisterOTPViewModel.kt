// ========== 4. OTP/RegisterOTPViewModel.kt ==========
package com.developer.ev2z.ui.screens.OTP

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterOTPViewModel {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun verifyOTP(otp: String, onSuccess: () -> Unit) {
        _isLoading.value = true
        _error.value = null

        // TODO: Call API to verify OTP
        // Simulate API call
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            kotlinx.coroutines.delay(1500)
            _isLoading.value = false
            onSuccess()
        }
    }

    fun resendOTP() {
        _isLoading.value = true
        // TODO: Call API to resend OTP
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            kotlinx.coroutines.delay(1000)
            _isLoading.value = false
        }
    }
}