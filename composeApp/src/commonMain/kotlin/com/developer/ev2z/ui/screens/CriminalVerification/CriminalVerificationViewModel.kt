package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.runtime.*
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.util.AppTokenProvider
import kotlinx.serialization.json.jsonPrimitive

// ================= DATA MODELS =================

data class CriminalForm(
    val name: String,
    val email: String,
    val phone: String,
    val employeeId: String,
    val consent: Boolean,
    val houseNo: String,
    val colony: String,
    val city: String,
    val state: String,
    val pincode: String,
    val landmark: String,
    val stayingPeriod: String
)


data class CandidateResult(
    val candidateId: String? = null,
    val isDuplicate: Boolean = false,
    val error: String? = null
)

enum class VerificationStep {
    FORM,
    CREATING_CANDIDATE,
    PAYMENT,
    SUBMITTING_BGV,
    SUCCESS,
    ERROR
}

// ================= VIEWMODEL =================

class CriminalVerificationViewModel(
    private val api: ApiService
) {

    var step by mutableStateOf(VerificationStep.FORM)
    var loading by mutableStateOf(false)

    var merchantOrderId by mutableStateOf<String?>(null)
    var paymentToken by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf("")
    var amount = 299

    private var cachedForm: CriminalForm? = null

    // =========================
    // STEP 1 → USER SUBMIT FORM
    // =========================
    suspend fun submitForm(form: CriminalForm) {
        cachedForm = form
        startPayment()
    }

    // =========================
    // STEP 2 → CREATE PAYMENT ORDER
    // =========================
    private suspend fun startPayment() {
        loading = true
        step = VerificationStep.CREATING_CANDIDATE // show loader

        try {
            val order = api.createCriminalOrder(amount)

            merchantOrderId = order["merchantOrderId"]?.jsonPrimitive?.content
            paymentToken = order["token"]?.jsonPrimitive?.content

            if (paymentToken.isNullOrEmpty()) {
                throw Exception("Payment token missing")
            }

            step = VerificationStep.PAYMENT

        } catch (e: Exception) {
            errorMessage = e.message ?: "Payment init failed"
            step = VerificationStep.ERROR
        } finally {
            loading = false
        }
    }

    // =========================
    // STEP 3 → PAYMENT SUCCESS
    // =========================
    suspend fun onPaymentSuccess(phonepeOrderId: String) {
        loading = true
        step = VerificationStep.SUBMITTING_BGV

        try {
            val merchantId = merchantOrderId
                ?: throw Exception("Missing merchantOrderId")

            // ✅ 1. CALLBACK BACKEND
            api.sendCriminalPaymentCallback(
                merchantOrderId = merchantId,
                phonepeOrderId = phonepeOrderId,
                status = "SUCCESS"
            )

            // ✅ 2. CREATE CANDIDATE AFTER PAYMENT
            val candidateResult = api.createCandidateWithPackage(cachedForm!!)

            val candidateId = candidateResult.candidateId?.toIntOrNull()
                ?: throw Exception("Candidate creation failed")

            // ✅ 3. SUBMIT BGV
            api.submitBgvVerification(candidateId, cachedForm!!)

            step = VerificationStep.SUCCESS

        } catch (e: Exception) {
            errorMessage = e.message ?: "Verification failed"
            step = VerificationStep.ERROR
        } finally {
            loading = false
        }
    }

    fun reset() {
        step = VerificationStep.FORM
        merchantOrderId = null
        paymentToken = null
        errorMessage = ""
        cachedForm = null
    }
}