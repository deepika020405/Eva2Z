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

    // ✅ Store selected image bytes temporarily - DON'T auto-upload
    private var selectedImageBytes: ByteArray? = null

    init {
        loadUserData()
    }

    fun loadUserData() {
        val name = storageManager.getString("name", "")
        val email = storageManager.getString("email", "")
        val phone = storageManager.getString("phone", "")
        val image = storageManager.getString("photo", "")

        println("📱 Loading user data from storage:")
        println("   Name: $name")
        println("   Email: $email")
        println("   Phone: $phone")
        println("   Photo: $image")

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

    // ✅ FIX 1: Just store image bytes, DON'T upload automatically
    fun updateProfileWithImage(imageBytes: ByteArray) {
        selectedImageBytes = imageBytes
        println("✅ Image selected, size: ${imageBytes.size} bytes (will upload on button click)")

        // ✅ Just show preview, don't upload yet
        // Convert bytes to temporary URL for preview (optional)
        // For now, just store it
    }

    // ✅ FIX 2: Email is optional now
    fun updateProfile() {
        viewModelScope.launch {
            // Validate: Name is required
            if (_state.value.userName.trim().isEmpty()) {
                _state.value = _state.value.copy(
                    error = "Name is required"
                )
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val token = storageManager.getString("token", "")
                if (token.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Token not found. Please login again."
                    )
                    return@launch
                }

                println("🌐 Updating profile...")
                println("   Name: ${_state.value.userName}")
                println("   Email: ${_state.value.userEmail}")
                println("   Has Image: ${selectedImageBytes != null}")
                println("   Image size: ${selectedImageBytes?.size ?: 0} bytes")

                val response = apiService.updateProfile(
                    token = "Bearer $token",
                    name = _state.value.userName,
                    email = _state.value.userEmail.ifEmpty { "" }, // ✅ Email optional
                    photo = selectedImageBytes
                )

                println("✅ API Response received:")
                println("   Response: $response")

                // Parse response
                val userObject = response["user"] as? Map<*, *>
                val photoUrl = response["photo_url"]?.toString()?.trim('"')

                if (userObject != null) {
                    val newName = userObject["name"]?.toString()?.trim('"') ?: _state.value.userName
                    val newEmail = userObject["email"]?.toString()?.trim('"') ?: ""
                    val newPhone = userObject["phone"]?.toString()?.trim('"') ?: _state.value.userPhone
                    val newPhoto = userObject["photo"]?.toString()?.trim('"')

                    println("✅ Parsed user data:")
                    println("   Name: $newName")
                    println("   Email: $newEmail")
                    println("   Phone: $newPhone")
                    println("   Photo: $newPhoto")
                    println("   Photo URL: $photoUrl")

                    // Build full photo URL
                    val fullPhotoUrl = when {
                        photoUrl != null && photoUrl.startsWith("http") -> photoUrl
                        photoUrl != null -> "https://gps.eva2z.in$photoUrl"
                        newPhoto != null && newPhoto.startsWith("http") -> newPhoto
                        newPhoto != null -> "https://gps.eva2z.in/storage/$newPhoto"
                        else -> _state.value.userImage // Keep existing if no new photo
                    }

                    println("✅ Final photo URL: $fullPhotoUrl")

                    // ✅ Save to storage
                    storageManager.saveString("name", newName)
                    storageManager.saveString("email", newEmail)
                    storageManager.saveString("phone", newPhone)
                    if (fullPhotoUrl != null) {
                        storageManager.saveString("photo", fullPhotoUrl)
                    }

                    println("✅ Saved to local storage")

                    // ✅ FIX 3: Update state BEFORE marking success
                    _state.value = _state.value.copy(
                        isLoading = false,
                        userName = newName,
                        userEmail = newEmail,
                        userPhone = newPhone,
                        userImage = fullPhotoUrl,
                        error = null
                    )

                    // Clear selected image after successful upload
                    selectedImageBytes = null

                    // ✅ Set success flag AFTER state is updated
                    _state.value = _state.value.copy(updateSuccess = true)

                    println("✅ Profile update completed successfully!")
                } else {
                    println("❌ Invalid response structure")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid response from server"
                    )
                }

            } catch (e: Exception) {
                println("❌ Profile update error: ${e.message}")
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update profile"
                )
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