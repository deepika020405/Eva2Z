package com.developer.ev2z.ui.screens.AccountActivity

import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

data class AccountActivityState(
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val errorMessage: String? = null
)

class AccountActivityViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(AccountActivityState())
    val state: StateFlow<AccountActivityState> = _state.asStateFlow()

    fun deleteAccount(password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isDeleting = true,
                errorMessage = null
            )

            try {
                val token = storageManager.getString("token", "") ?: ""

                if (token.isEmpty()) {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        errorMessage = "Authentication error. Please login again."
                    )
                    return@launch
                }

                val body = JsonObject(
                    mapOf("password" to JsonPrimitive(password))
                )

                println("🗑️ Attempting to delete account...")
                val response = apiService.deleteAccount(token, body)
                println("🗑️ Delete Account Response: $response")

//                val success = response["success"]?.jsonPrimitive?.content?.toBoolean() ?: false
                val success = response["status"]?.jsonPrimitive?.content?.toBoolean() ?: false
                val message = response["message"]?.jsonPrimitive?.content
                    ?: "Account deleted successfully"

                if (success) {
                    println("✅ Account deleted successfully")

                    // ✅ Call logout to clear everything
                    logout()

                } else {
                    println("❌ Delete failed: $message")
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        errorMessage = message
                    )
                }

            } catch (e: Exception) {
                println("❌ Delete Account Exception: ${e.message}")
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isDeleting = false,
                    errorMessage = e.message ?: "Failed to delete account"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                println("🚪 AccountActivityViewModel: Logging out...")
                val token = storageManager.getString("token", "")
                val fcmToken = storageManager.getString("last_fcm_token", "")
                if (token.isNotBlank() && fcmToken.isNotBlank()) {
                    try {
                        apiService.logout("Bearer $token", fcmToken)
                        println("✅ Logout API called successfully")
                    } catch (e: Exception) {
                        println("⚠️ Logout API failed (continuing local logout): ${e.message}")
                    }
                }
                storageManager.clear()
                println("✅ Storage cleared")
                _state.value = _state.value.copy(
                    isDeleting = false,
                    deleteSuccess = true,
                    errorMessage = null
                )
                println("✅ Logout complete")
            } catch (e: Exception) {
                println("❌ Logout failed: ${e.message}")
                _state.value = _state.value.copy(
                    isDeleting = false,
                    errorMessage = e.message ?: "Logout failed"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun resetState() {
        _state.value = AccountActivityState()
    }
}