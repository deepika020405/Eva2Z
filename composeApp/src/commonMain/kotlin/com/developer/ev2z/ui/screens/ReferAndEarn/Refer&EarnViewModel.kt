package com.developer.ev2z.ui.screens.ReferAndEarn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReferralUiState {
    object Loading : ReferralUiState()
    data class Success(
        val referralCode: String,
        val referralAmount: Int,
        val referralBalance: String,
        val hasDevice: Boolean = true
    ) : ReferralUiState()
    data class NoDevice(
        val message: String
    ) : ReferralUiState()
    data class Error(
        val message: String
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
                    // ✅ Ready for API update - uncomment when API is updated
                    /*
                    _uiState.value = ReferralUiState.Success(
                        referralCode = response.referral_code,
                        referralAmount = response.referral_amount ?: 150,
                        referralBalance = response.referral_balance ?: "0",
                        hasDevice = true
                    )
                    */

                    // ✅ Current implementation - comment out when API is updated
                    _uiState.value = ReferralUiState.Success(
                        referralCode = response.referral_code,
                        referralAmount = 150, // Hardcoded for now
                        referralBalance = "450", // Hardcoded for now
                        hasDevice = true
                    )

                    println("✅ Referral code loaded: ${response.referral_code}")
                } else {
                    // ✅ User doesn't have device - show NoDevice state
                    _uiState.value = ReferralUiState.NoDevice(
                        message = response.message ?: "You need to add at least one device before sharing referral code."
                    )
                    println("⚠️ No device found for referral")
                }
            } catch (e: Exception) {
                _uiState.value = ReferralUiState.Error(
                    message = "Failed to load referral code: ${e.message}"
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

    // ✅ Dynamic referral amount in messages
    fun getReferralMessage(referralCode: String, referralAmount: Int): String {
        return """
            🎁 Join me on EVA2Z Connect and get ₹$referralAmount OFF!
            
            EVA2Z Connect is the best GPS tracking app for your vehicles.
            
            Use my referral code: $referralCode
            
            Download Apk now: https://play.google.com/store/apps/details?id=com.developer.ev2z
            
            Both of us will get ₹$referralAmount reward when you place your first order! 🎉
        """.trimIndent()
    }

    fun getWhatsAppMessage(referralCode: String, referralAmount: Int): String {
        return """
            🎁 Join me on EVA2Z Connect and get ₹$referralAmount OFF!
            
            EVA2Z Connect is the best GPS tracking app for your vehicles.
            
            Use my referral code: $referralCode
            
            Download Apk now: https://play.google.com/store/apps/details?id=com.developer.ev2z
            
            Both of us will get ₹$referralAmount reward when you place your first order! 🎉
        """.trimIndent()
    }
}