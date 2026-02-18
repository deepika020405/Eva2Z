package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.util.AppTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.text.get

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
    val stayingPeriod: String,

    // 🔥 NEW OPTIONAL FIELDS
    val uanNumber: String = "",
    val uanNotAvailable: Boolean = true,
    val rented: Boolean = false
)


data class CandidateResult(
    val candidateId: String? = null,
    val isDuplicate: Boolean = false,
    val error: String? = null
)

enum class VerificationStep {
    FORM,
    CREATING_ORDER,
    PAYMENT,
    VERIFYING_PAYMENT,
    CREATING_CANDIDATE,
    SUBMITTING_BGV,
    SUCCESS,
    ERROR
}


enum class BgvStatus {
    FORM_SUBMITTED,
    PAYMENT_DONE,
    CANDIDATE_CREATED,
    VERIFICATION_STARTED,
    COMPLETED,
    FAILED
}

// ================= VIEWMODEL =================

class CriminalVerificationViewModel(
    private val api: ApiService
) {

    var step by mutableStateOf(VerificationStep.FORM)
    var loading by mutableStateOf(false)
    var createdCandidateId by mutableStateOf<Int?>(null)

    var bgvStatus by mutableStateOf(BgvStatus.FORM_SUBMITTED)

    var merchantOrderId by mutableStateOf<String?>(null)
    var paymentToken by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf("")
    var amount = 1

    var cachedForm by mutableStateOf<CriminalForm?>(null)
        private set
    // =========================
    // STEP 1 → USER SUBMIT FORM
    // =========================
    suspend fun submitForm(form: CriminalForm) {
        cachedForm = form
        bgvStatus = BgvStatus.FORM_SUBMITTED
        createOrder()
    }




    private suspend fun createOrder() {
        loading = true
        step = VerificationStep.CREATING_ORDER

        try {
            val order = api.createCriminalOrder(amount)

            merchantOrderId = order["merchantOrderId"]?.jsonPrimitive?.content
            paymentToken = order["token"]?.jsonPrimitive?.content

            if (paymentToken.isNullOrEmpty()) {
                throw Exception("Payment token missing")
            }

            step = VerificationStep.PAYMENT

        } catch (e: Exception) {
            errorMessage = e.message ?: "Order creation failed"
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
        step = VerificationStep.VERIFYING_PAYMENT
        bgvStatus = BgvStatus.PAYMENT_DONE


        try {
            val merchantId = merchantOrderId
                ?: throw Exception("Missing merchantOrderId")

            // 1️⃣ Verify payment on backend
            api.sendCriminalPaymentCallback(
                merchantOrderId = merchantId,
                phonepeOrderId = phonepeOrderId,
                status = "SUCCESS"
            )

            createCandidate()

        } catch (e: Exception) {
            errorMessage = e.message ?: "Payment verification failed"
            step = VerificationStep.ERROR
        } finally {
            loading = false
        }
    }


    private suspend fun createCandidate() {
        step = VerificationStep.CREATING_CANDIDATE
        bgvStatus = BgvStatus.CANDIDATE_CREATED


        val result = api.createCandidateWithPackage(cachedForm!!)

        when {
            result.candidateId != null -> {
                val id = result.candidateId.toInt()

                createdCandidateId = id  // ✅ STORE ID

                submitBGV(id)
            }

            result.isDuplicate -> {
                println("ℹ️ Candidate already exists — continuing BGV")

                // optional: show message
                errorMessage = "Candidate already exists. Verification already initiated."
                step = VerificationStep.SUCCESS
            }

            else -> {
                throw Exception(result.error ?: "Candidate creation failed")
            }
        }
    }

    suspend fun checkVerificationStatus() {
        val id = createdCandidateId ?: return

        try {
            println("🌐 Checking status for candidateId=$id")

            val response = api.getCandidateStatus(id)

            println("📥 RAW STATUS RESPONSE = $response")

            val data = response["data"]
                ?.jsonObject?.get("data")
                ?.jsonObject

            val statusCode = data
                ?.get("overall_status_code")
                ?.jsonPrimitive?.content?.toInt()

            val statusText = data
                ?.get("overall_status")
                ?.jsonPrimitive?.content

            println("📊 STATUS CODE = $statusCode")
            println("📊 STATUS TEXT = $statusText")

            bgvStatus = when (statusCode) {
                20 -> BgvStatus.COMPLETED
                30 -> BgvStatus.FAILED
                else -> BgvStatus.VERIFICATION_STARTED
            }

        } catch (e: Exception) {
            println("❌ Status check failed: ${e.message}")
        }
    }

    fun startStatusPolling(scope: CoroutineScope) {
        scope.launch {
            while (bgvStatus != BgvStatus.COMPLETED && bgvStatus != BgvStatus.FAILED) {
                checkVerificationStatus()
                delay(5000)
            }
        }
    }

    private suspend fun submitBGV(candidateId: Int) {
        step = VerificationStep.SUBMITTING_BGV
        bgvStatus = BgvStatus.VERIFICATION_STARTED


        api.submitBgvVerification(candidateId, cachedForm!!)

        step = VerificationStep.SUCCESS
    }

    fun reset() {
        step = VerificationStep.FORM
        merchantOrderId = null
        paymentToken = null
        errorMessage = ""
        cachedForm = null
        createdCandidateId = null   // ✅ add
    }
}


