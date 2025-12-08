package com.developer.ev2z.ui.screens.EditProfile

import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditProfileState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val userImage: String? = null,
    val error: String? = null,
    val updateSuccess: Boolean = false
)

class EditProfileViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    // ✅ Store selected image bytes temporarily
    private var selectedImageBytes: ByteArray? = null

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val name = storageManager.getString("name", "")
        val email = storageManager.getString("email", "")
        val phone = storageManager.getString("phone", "")
        val image = storageManager.getString("profile_image", "")

        _state.value = _state.value.copy(
            userName = name,
            userEmail = email,
            userPhone = phone,
            userImage = image.ifEmpty { null }
        )
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(userName = name)
    }

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(userEmail = email)
    }

    // ✅ New: Handle image selection
    fun updateProfileWithImage(imageBytes: ByteArray) {
        selectedImageBytes = imageBytes
        println("✅ Image selected, size: ${imageBytes.size} bytes")

        // Update immediately to show image
        updateProfile()
    }

    // ✅ Update profile (with or without image)
    fun updateProfile() {
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

                println("🌐 Updating profile...")
                println("   Name: ${_state.value.userName}")
                println("   Email: ${_state.value.userEmail}")
                println("   Has Image: ${selectedImageBytes != null}")

                val response = apiService.updateProfile(
                    token = "Bearer $token",
                    name = _state.value.userName,
                    email = _state.value.userEmail,
                    photo = selectedImageBytes  // ✅ Pass image bytes
                )

                // Parse response
                val newName = response["name"]?.toString()?.trim('"')
                val newEmail = response["email"]?.toString()?.trim('"')
                val newPhoto = response["photo"]?.toString()?.trim('"')

                println("✅ Profile update response:")
                println("   Name: $newName")
                println("   Email: $newEmail")
                println("   Photo: $newPhoto")

                // Build full URL if relative path
                val fullPhotoUrl = if (!newPhoto.isNullOrEmpty()) {
                    if (newPhoto.startsWith("http")) {
                        newPhoto
                    } else {
                        "https://gps.eva2z.in$newPhoto"
                    }
                } else null

                // Save to storage
                if (!newName.isNullOrEmpty()) {
                    storageManager.saveString("name", newName)
                }

                if (!newEmail.isNullOrEmpty()) {
                    storageManager.saveString("email", newEmail)
                }

                if (fullPhotoUrl != null) {
                    storageManager.saveString("profile_image", fullPhotoUrl)
                    println("✅ Saved profile image: $fullPhotoUrl")
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    userName = newName ?: _state.value.userName,
                    userEmail = newEmail ?: _state.value.userEmail,
                    userImage = fullPhotoUrl,
                    updateSuccess = true,
                    error = null
                )

                // ✅ Clear image bytes after successful upload
                selectedImageBytes = null

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update profile"
                )
                println("❌ Profile update error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }
}