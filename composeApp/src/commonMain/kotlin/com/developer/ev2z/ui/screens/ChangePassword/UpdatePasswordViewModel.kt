package com.developer.ev2z.ui.screens.ChangePassword

import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class UpdatePasswordViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    // Loading state
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    // Success state
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ============================================
    // CHANGE PASSWORD
    // ============================================
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmNewPassword: String
    ) {
        println("🔐 Change Password started")

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _updateSuccess.value = false

            try {
                val token = storageManager.getString("token", "")

                if (token.isEmpty()) {
                    _error.value = "Session expired. Please login again."
                    _loading.value = false
                    return@launch
                }

                println("🔐 Calling API with token...")
                val result = apiService.changePassword(
                    token = token,
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    confirmNewPassword = confirmNewPassword
                )

                // ✅ Check for success message
                val message = result["message"]?.jsonPrimitive?.content

                when {
                    // SUCCESS CASE
                    message?.contains("successfully", ignoreCase = true) == true -> {
                        println("✅ Password changed successfully!")
                        _updateSuccess.value = true
                        _error.value = null
                    }

                    // ERROR: Current password incorrect
                    result["error"]?.jsonPrimitive?.content != null -> {
                        val errorMsg = result["error"]?.jsonPrimitive?.content
                        println("❌ API Error: $errorMsg")
                        _error.value = errorMsg ?: "Failed to change password"
                    }

                    // ERROR: Validation errors (multiple fields)
                    result["errors"]?.jsonObject != null -> {
                        val errors = result["errors"]?.jsonObject
                        val errorMessages = mutableListOf<String>()

                        errors?.forEach { (field, errorArray) ->
                            val messages = errorArray.jsonArray.mapNotNull {
                                it.jsonPrimitive?.content
                            }
                            errorMessages.addAll(messages)
                        }

                        val combinedError = errorMessages.joinToString("\n")
                        println("❌ Validation Errors: $combinedError")
                        _error.value = combinedError
                    }

                    // UNKNOWN ERROR
                    else -> {
                        println("❌ Unknown response format: $result")
                        _error.value = "Failed to change password. Please try again."
                    }
                }

            } catch (e: Exception) {
                println("❌ Change Password Exception: ${e.message}")
                e.printStackTrace()
                _error.value = e.message ?: "Network error. Please try again."
            } finally {
                _loading.value = false
            }
        }
    }

    // ============================================
    // VALIDATE PASSWORDS
    // ============================================
    fun validatePasswords(
        current: String,
        new: String,
        confirm: String
    ): ValidationResult {
        return when {
            current.isEmpty() -> ValidationResult(
                isValid = false,
                currentPasswordError = "Current password is required"
            )

            current.length < 6 -> ValidationResult(
                isValid = false,
                currentPasswordError = "Current password must be at least 6 characters"
            )

            new.isEmpty() -> ValidationResult(
                isValid = false,
                newPasswordError = "New password is required"
            )

            new.length < 6 -> ValidationResult(
                isValid = false,
                newPasswordError = "New password must be at least 6 characters"
            )

            new == current -> ValidationResult(
                isValid = false,
                newPasswordError = "New password must be different from current password"
            )

            confirm.isEmpty() -> ValidationResult(
                isValid = false,
                confirmPasswordError = "Please confirm your new password"
            )

            new != confirm -> ValidationResult(
                isValid = false,
                confirmPasswordError = "Passwords do not match"
            )

            else -> ValidationResult(isValid = true)
        }
    }

    // ============================================
    // STATE RESET FUNCTIONS
    // ============================================
    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun resetState() {
        _loading.value = false
        _updateSuccess.value = false
        _error.value = null
    }
}

// ============================================
// VALIDATION RESULT DATA CLASS
// ============================================
data class ValidationResult(
    val isValid: Boolean,
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null
)