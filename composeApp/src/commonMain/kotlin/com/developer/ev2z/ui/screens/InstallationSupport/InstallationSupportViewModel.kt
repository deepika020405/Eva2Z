package com.developer.ev2z.ui.screens.InstallationSupport

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.request.SupportRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SupportFormState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class InstallationSupportViewModel(
    private val apiService: ApiService = ApiService()
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(SupportFormState())
    val state: StateFlow<SupportFormState> = _state.asStateFlow()

    fun submitSupport(
        name: String,
        phone: String,
        email: String,
        product: String,
        issueType: String,
        comment: String,
        appointmentDate: String,   // "DD/MM/YYYY" or blank
        appointmentTime: String    // "HH:MM AM/PM" or blank
    ) {
        _state.value = SupportFormState(isLoading = true)

        scope.launch {
            try {
                val request = SupportRequest(
                    name = name,
                    phone = phone,
                    email = email,
                    product = product,
                    subject = issueType,
                    message = comment,
                    issueType = issueType,
                    comment = comment,
                    appointmentDate = appointmentDate.toApiDate(),
                    appointmentTime = appointmentTime.toApiTime()
                )

                val response = apiService.submitSupportRequest(request)

                if (response.status == "success") {
                    _state.value = SupportFormState(isSuccess = true)
                } else {
                    _state.value = SupportFormState(error = response.message ?: "Submission failed. Please try again.")
                }

            } catch (e: Exception) {
                _state.value = SupportFormState(error = "Network error. Please check your connection.")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

// "DD/MM/YYYY" → "YYYY-MM-DD", blank/invalid → null
private fun String.toApiDate(): String? {
    if (isBlank()) return null
    val parts = split("/")
    if (parts.size != 3) return null
    return "${parts[2]}-${parts[1]}-${parts[0]}"
}

// "HH:MM AM/PM" → "HH:MM:00" (24h), blank/invalid → null
private fun String.toApiTime(): String? {
    if (isBlank()) return null
    return try {
        val parts = split(" ")
        val (h, m) = parts[0].split(":").map { it.toInt() }
        val period = parts.getOrNull(1) ?: "AM"
        val hour24 = when {
            period == "AM" && h == 12 -> 0
            period == "PM" && h != 12 -> h + 12
            else -> h
        }
        "${hour24.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:00"
    } catch (e: Exception) {
        null
    }
}