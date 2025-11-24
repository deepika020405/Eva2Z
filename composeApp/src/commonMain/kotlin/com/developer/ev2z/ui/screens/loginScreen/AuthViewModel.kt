package com.developer.ev2z.viewmodel

import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.repository.AuthRepository
import com.developer.ev2z.data.local.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    private val storage: StorageManager
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess   // 🔥 FIXED: val not var

    private val _signupSuccess = MutableStateFlow(false)
    val signupSuccess: StateFlow<Boolean> = _signupSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error


    fun login(request: LoginRequest) {
        println("🔥 Login started with phone: ${request.phone}")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                println("🔥 Making API call...")
                val result = repository.login(request)

                println("🔥 Login successful! Token: ${result.token}")

                storage.saveString("token", result.token)
                storage.saveString("name", result.user.name)
                storage.saveString("phone", result.user.phone)

                _loginSuccess.value = true      // Login success
                println("🔥 Login success set to true")

            } catch (e: Exception) {
                println("❌ Login error: ${e.message}")
                e.printStackTrace()
                _error.value = e.message ?: "Login failed"
            } finally {
                _loading.value = false
                println("🔥 Loading set to false")
            }
        }
    }

    fun setLoginSuccess(value: Boolean) {
        _loginSuccess.value = value
    }

    fun resetLoginSuccess() {
        _loginSuccess.value = false
    }

    fun resetSignupSuccess() {
        _signupSuccess.value = false
    }

    fun setError(message: String) {
        _error.value = message
    }

    fun clearError() {
        _error.value = null
    }
}