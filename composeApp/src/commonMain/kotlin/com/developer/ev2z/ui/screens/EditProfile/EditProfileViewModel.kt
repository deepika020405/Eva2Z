package com.developer.ev2z.ui.screens.EditProfile

import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.util.compressImageBytes
import com.developer.ev2z.util.fixAndCompressImage
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
    val userImageBytes: ByteArray? = null,
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
//    fun updateProfileWithImage(imageBytes: ByteArray) {
//        selectedImageBytes = imageBytes
//        println("✅ Image selected, size: ${imageBytes.size} bytes (will upload on button click)")
//
//        // ✅ Just show preview, don't upload yet
//        // Convert bytes to temporary URL for preview (optional)
//        // For now, just store it
//    }

    fun updateProfileWithImage(imageBytes: ByteArray) {
        // 1️⃣ Fix rotation + compress
        val fixedBytes = fixAndCompressImage(imageBytes, maxSizeKB = 500)

        // 2️⃣ Store for upload
        selectedImageBytes = fixedBytes

        // 3️⃣ Update UI immediately
        _state.value = _state.value.copy(
            userImageBytes = fixedBytes   // ✅ raw bytes for UI
        )

        println("✅ Image selected, fixed rotation, preview updated (size: ${fixedBytes.size} bytes)")
    }



    // ✅ FIX 2: Email is optional now

    fun updateProfile() {
        viewModelScope.launch {

            val name = _state.value.userName.trim()
            val email = _state.value.userEmail.trim()

            // 🔴 VALIDATION: Name required
            if (name.isEmpty()) {
                _state.value = _state.value.copy(
                    error = "Name is required"
                )
                return@launch
            }

            // 🔴 VALIDATION: Email required
            if (email.isEmpty()) {
                _state.value = _state.value.copy(
                    error = "Email is required"
                )
                return@launch
            }

            // 🔴 VALIDATION: Email format
            if (!isValidEmail(email)) {
                _state.value = _state.value.copy(
                    error = "Please enter a valid email address"
                )
                return@launch
            }

            // ✅ All validations passed
            _state.value = _state.value.copy(
                isLoading = true,
                error = null
            )

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
                println("   Name: $name")
                println("   Email: $email")
                println("   Has Image: ${selectedImageBytes != null}")
                println("   Image size: ${selectedImageBytes?.size ?: 0} bytes")

                val compressedImage = selectedImageBytes?.let {
                    compressImageBytes(it, maxSizeKB = 500)
                }

                val response = apiService.updateProfile(
                    token = "Bearer $token",
                    name = name,
                    email = email, // 🔥 Email now required
                    photo = compressedImage
                )

                val userObject = response["user"] as? Map<*, *>
                val photoUrl = response["photo_url"]?.toString()?.trim('"')

                if (userObject == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid response from server"
                    )
                    return@launch
                }

                val newName = userObject["name"]?.toString()?.trim('"') ?: name
                val newEmail = userObject["email"]?.toString()?.trim('"') ?: email
                val newPhone = userObject["phone"]?.toString()?.trim('"') ?: _state.value.userPhone
                val newPhoto = userObject["photo"]?.toString()?.trim('"')

                val fullPhotoUrl = when {
                    photoUrl != null && photoUrl.startsWith("http") -> photoUrl
                    photoUrl != null -> "https://gps.eva2z.in$photoUrl"
                    newPhoto != null && newPhoto.startsWith("http") -> newPhoto
                    newPhoto != null -> "https://gps.eva2z.in/storage/$newPhoto"
                    else -> _state.value.userImage
                }

                // ✅ Save locally
                storageManager.saveString("name", newName)
                storageManager.saveString("email", newEmail)
                storageManager.saveString("phone", newPhone)
                fullPhotoUrl?.let { storageManager.saveString("photo", it) }

                // ✅ Update UI state
                _state.value = _state.value.copy(
                    isLoading = false,
                    userName = newName,
                    userEmail = newEmail,
                    userPhone = newPhone,
                    userImage = fullPhotoUrl,
                    error = null,
                    updateSuccess = true
                )

                // Clear image after success
                selectedImageBytes = null

                println("✅ Profile updated successfully")

            } catch (e: Exception) {
                println("❌ Profile update error: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to update profile"
                )
            }
        }
    }



    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        return emailRegex.matches(email)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }
}