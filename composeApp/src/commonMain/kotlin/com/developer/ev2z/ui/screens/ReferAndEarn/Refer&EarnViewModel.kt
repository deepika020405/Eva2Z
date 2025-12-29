package com.developer.ev2z.ui.screens.ReferAndEarn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.ReferralCodeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReferralUiState {
    object Loading : ReferralUiState()
    data class Success(
        val referralCode: String,
        val hasDevice: Boolean = true
    ) : ReferralUiState()
    data class Error(
        val message: String,
        val hasDevice: Boolean = false
    ) : ReferralUiState()
}

class ReferAndEarnViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReferralUiState>(ReferralUiState.Loading)
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    private val _showCopySnackbar = MutableStateFlow(false)
    val showCopySnackbar: StateFlow<Boolean> = _showCopySnackbar.asStateFlow()

    fun loadReferralCode(token: String) {
        viewModelScope.launch {
            _uiState.value = ReferralUiState.Loading

            try {
                val response = apiService.getReferralCode(token)

                if (response.success && response.referral_code != null) {
                    _uiState.value = ReferralUiState.Success(
                        referralCode = response.referral_code,
                        hasDevice = true
                    )
                    println("✅ Referral code loaded: ${response.referral_code}")
                } else {
                    // User doesn't have device
                    _uiState.value = ReferralUiState.Error(
                        message = response.message ?: "You need to add at least one device first",
                        hasDevice = false
                    )
                    println("⚠️ No device found for referral")
                }
            } catch (e: Exception) {
                _uiState.value = ReferralUiState.Error(
                    message = "Failed to load referral code: ${e.message}",
                    hasDevice = false
                )
                println("❌ Error loading referral code: ${e.message}")
            }
        }
    }

    fun showCopySuccess() {
        _showCopySnackbar.value = true
    }

    fun resetCopySnackbar() {
        _showCopySnackbar.value = false
    }

    fun getReferralMessage(referralCode: String): String {
        return """
            🎁 Join me on EVA2Z Connect and get ₹150 OFF!
            
            EVA2Z Connect is the best GPS tracking app for your vehicles.
            
            Use my referral code: $referralCode
            
            Download now: https://eva2z.in/download
            
            Both of us will get ₹150 reward when you place your first order! 🎉
        """.trimIndent()
    }

    fun getWhatsAppMessage(referralCode: String): String {
        return "🎁 *Join me on EVA2Z Connect!*\n\n" +
                "Get *₹150 OFF* on your first order!\n\n" +
                "Use my referral code: *$referralCode*\n\n" +
                "Download: https://eva2z.in/download\n\n" +
                "Both of us will get ₹150 reward! 🎉"
    }
}