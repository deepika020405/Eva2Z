package com.developer.ev2z.ui.screens.Profile

import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val userPhone: String = "",
    val userImage: String? = null,
    val error: String? = null,
    val logoutSuccess: Boolean = false
)

class ProfileViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserData()
        fetchProfilePicture()
    }

    private fun loadUserData() {
        val name = storageManager.getString("name", "User")
        val phone = storageManager.getString("phone", "")
        val image = storageManager.getString("profile_image", "")

        _state.value = _state.value.copy(
            userName = name,
            userPhone = phone,
            userImage = image.ifEmpty { null }
        )
    }

    fun fetchProfilePicture() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val token = storageManager.getString("token", "")
                if (token.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Token not found"
                    )
                    return@launch
                }

                val response = apiService.getProfilePicture("Bearer $token")

                // Parse profile picture URL
                val profilePicUrl = response["profile_picture_url"]?.toString()?.trim('"')
                val newName = response["name"]?.toString()?.trim('"')

                // Build full URL if relative path
                val fullUrl = if (!profilePicUrl.isNullOrEmpty()) {
                    if (profilePicUrl.startsWith("http")) {
                        profilePicUrl
                    } else {
                        "https://gps.eva2z.in$profilePicUrl"
                    }
                } else null

                // Save to storage
                if (fullUrl != null) {
                    storageManager.saveString("profile_image", fullUrl)
                }

                if (!newName.isNullOrEmpty()) {
                    storageManager.saveString("name", newName)
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    userName = newName ?: _state.value.userName,
                    userImage = fullUrl,
                    error = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to fetch profile picture"
                )
                println("❌ Profile fetch error: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Clear all user data
                storageManager.clear()

                _state.value = _state.value.copy(
                    logoutSuccess = true,
                    userName = "",
                    userPhone = "",
                    userImage = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Logout failed"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}