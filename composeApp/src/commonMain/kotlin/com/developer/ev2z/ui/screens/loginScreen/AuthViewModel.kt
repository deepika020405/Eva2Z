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
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val result = repository.login(request)

                storage.saveString("token", result.token)
                storage.saveString("name", result.user.name)
                storage.saveString("phone", result.user.phone)

                _loginSuccess.value = true

            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun resetLoginSuccess() {
        _loginSuccess.value = false
    }

    fun clearError() {
        _error.value = null
    }
}