package com.developer.ev2z.ui.screens.Password

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ForgetPasswordViewModel {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun resetPassword(password: String, confirmPassword: String, onSuccess: () -> Unit) {
        if (password != confirmPassword) {
            _error.value = "Passwords do not match"
            return
        }

        _isLoading.value = true
        _error.value = null

        // TODO: Call API to reset password
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            kotlinx.coroutines.delay(2000)
            _isLoading.value = false
            onSuccess()
        }
    }
}