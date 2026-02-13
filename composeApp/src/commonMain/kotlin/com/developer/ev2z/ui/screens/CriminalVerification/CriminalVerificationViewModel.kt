package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.developer.ev2z.network.ApiService

data class CriminalForm(
    val name: String,
    val email: String,
    val phone: String,
    val employeeId: String,
    val consent: Boolean
)

data class CandidateResult(
    val candidateId: String? = null,
    val isDuplicate: Boolean = false,
    val error: String? = null
)

data class PaymentResult(
    val success: Boolean,
    val transactionId: String? = null
)

enum class VerificationStep {
    FORM,
    PAYMENT,
    STATUS
}

class CriminalVerificationViewModel(
    private val api: ApiService
) : ViewModel() {

    var step by mutableStateOf(VerificationStep.FORM)
        private set

    var loading by mutableStateOf(false)
        private set

    var candidateId by mutableStateOf<String?>(null)
        private set

    var showDuplicateDialog by mutableStateOf(false)
        private set

    fun dismissDuplicateDialog() {
        showDuplicateDialog = false
    }

    fun goToForm() {
        step = VerificationStep.FORM
    }

    fun goToPayment() {
        step = VerificationStep.PAYMENT
    }

    fun goToStatus() {
        step = VerificationStep.STATUS
    }

    suspend fun submitForm(form: CriminalForm) {
        loading = true
        println("🌐 Creating candidate...")

        val result = api.createCandidateWithPackage(form)

        loading = false

        when {
            result.candidateId != null -> {
                candidateId = result.candidateId
                goToPayment()
            }

            result.isDuplicate -> {
                showDuplicateDialog = true
            }

            else -> println("❌ Error: ${result.error}")
        }
    }

    fun continueWithDuplicate() {
        showDuplicateDialog = false
        goToPayment()
    }

    fun paymentSuccess() {
        goToStatus()
    }
}