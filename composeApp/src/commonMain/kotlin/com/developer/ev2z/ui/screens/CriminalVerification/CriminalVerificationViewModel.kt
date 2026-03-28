package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.runtime.*
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val KEY_CANDIDATE_ID   = "bgv_candidate_id"
private const val KEY_BGV_STATUS     = "bgv_status"
private const val KEY_MERCHANT_ORDER = "bgv_merchant_order_id"
private const val KEY_REPORT_URL     = "bgv_report_url"

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
    FORM, CREATING_ORDER, PAYMENT, VERIFYING_PAYMENT,
    CREATING_CANDIDATE, SUBMITTING_BGV, SUCCESS, ERROR
}

enum class BgvStatus {
    FORM_SUBMITTED, PAYMENT_DONE, CANDIDATE_CREATED,
    VERIFICATION_STARTED, COMPLETED, FAILED
}

data class CourtCheckResult(
    val checkStatus: String,
    val checkStatusCode: Int,
    val city: String,
    val state: String,
    val address: String,
    val completionDate: String
)

class CriminalVerificationViewModel(
    private val api: ApiService,
    private val prefs: UserPrefs
) {
    var step               by mutableStateOf(VerificationStep.FORM)
    var loading            by mutableStateOf(false)
    var createdCandidateId by mutableStateOf<Int?>(null)
    var bgvStatus          by mutableStateOf(BgvStatus.FORM_SUBMITTED)
    var merchantOrderId    by mutableStateOf<String?>(null)
    var paymentToken       by mutableStateOf<String?>(null)
    var errorMessage       by mutableStateOf("")
    var amount             by mutableStateOf(1)
    var reportUrl          by mutableStateOf<String?>(null)
    var searchedCandidateName by mutableStateOf<String?>(null)
    var courtCheckResult   by mutableStateOf<CourtCheckResult?>(null)
        private set

    var cachedForm by mutableStateOf<CriminalForm?>(null)
        private set

    // ─────────────────────────────────────────────
    // Helper — court array parse karo (reusable)
    // ─────────────────────────────────────────────
    private fun parseCourtResult(data: kotlinx.serialization.json.JsonObject?) {
        val courtArray = data
            ?.get("check_details")
            ?.jsonObject
            ?.get("court")
            ?.let { kotlinx.serialization.json.Json.parseToJsonElement(it.toString()) }
            ?.jsonArray

        val firstCourt = courtArray?.firstOrNull()?.jsonObject

        if (firstCourt != null) {
            courtCheckResult = CourtCheckResult(
                checkStatus     = firstCourt["check_status"]?.jsonPrimitive?.content ?: "Pending",
                checkStatusCode = firstCourt["check_status_code"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                city            = firstCourt["city"]?.jsonPrimitive?.content ?: "",
                state           = firstCourt["state"]?.jsonPrimitive?.content ?: "",
                address         = buildString {
                    firstCourt["address_1"]?.jsonPrimitive?.content?.let { append(it) }
                    firstCourt["address_2"]?.jsonPrimitive?.content?.let { append(", $it") }
                },
                completionDate  = firstCourt["completion_date"]?.jsonPrimitive?.content?.take(10) ?: ""
            )
            println("⚖️ Court Result = $courtCheckResult")
        } else {
            println("⚖️ No court records found")
            courtCheckResult = null
        }
    }

    // ─────────────────────────────────────────────
    // Load persisted state — screen open hone par
    // ─────────────────────────────────────────────
    fun loadPersistedState() {
        val savedId = prefs.getString(KEY_CANDIDATE_ID, "")
            .toIntOrNull() ?: return

        createdCandidateId = savedId
        merchantOrderId    = prefs.getString(KEY_MERCHANT_ORDER, "").ifBlank { null }
        reportUrl          = prefs.getString(KEY_REPORT_URL, "").ifBlank { null }
        bgvStatus          = parseBgvStatus(prefs.getString(KEY_BGV_STATUS, ""))
        step               = VerificationStep.SUCCESS
    }

    // ─────────────────────────────────────────────
    // Search by Candidate ID
    // ─────────────────────────────────────────────
    suspend fun searchByCandidate(candidateIdStr: String, name: String) {
        loading      = true
        errorMessage = ""

        try {
            val id = candidateIdStr.trim().toIntOrNull()
                ?: throw Exception("Invalid Candidate ID — only numbers allowed")

            val response = api.getCandidateStatus(id)
            val data = response["data"]?.jsonObject?.get("data")?.jsonObject
                ?: throw Exception("Invalid response from server")

            val statusCode    = data["overall_status_code"]?.jsonPrimitive?.content?.toIntOrNull()
            val candidateName = data["name"]?.jsonPrimitive?.content
            val url           = data["report_url"]?.jsonPrimitive?.content

            createdCandidateId    = id
            searchedCandidateName = candidateName
            reportUrl             = url

            bgvStatus = when (statusCode) {
                1    -> BgvStatus.COMPLETED
                2    -> BgvStatus.FAILED
                else -> BgvStatus.VERIFICATION_STARTED
            }

            // ✅ Court result parse karo
            parseCourtResult(data)

            prefs.saveString(KEY_CANDIDATE_ID, id.toString())
            prefs.saveString(KEY_BGV_STATUS, bgvStatus.name)
            if (!url.isNullOrBlank()) prefs.saveString(KEY_REPORT_URL, url)

            step = VerificationStep.SUCCESS

        } catch (e: Exception) {
            errorMessage = e.message ?: "Search failed"
            step = VerificationStep.ERROR
        } finally {
            loading = false
        }
    }

    // ─────────────────────────────────────────────
    // Existing flow
    // ─────────────────────────────────────────────
    suspend fun submitForm(form: CriminalForm) {
        cachedForm = form
        bgvStatus  = BgvStatus.FORM_SUBMITTED
        createOrder()
    }

    private suspend fun createOrder() {
        loading = true
        step    = VerificationStep.CREATING_ORDER
        try {
            val order = api.createCriminalOrder(amount)
            merchantOrderId = order["merchantOrderId"]?.jsonPrimitive?.content
            paymentToken    = order["token"]?.jsonPrimitive?.content

            merchantOrderId?.let { prefs.saveString(KEY_MERCHANT_ORDER, it) }

            if (paymentToken.isNullOrEmpty()) throw Exception("Payment token missing")
            step = VerificationStep.PAYMENT
        } catch (e: Exception) {
            errorMessage = e.message ?: "Order creation failed"
            step = VerificationStep.ERROR
        } finally {
            loading = false
        }
    }

    suspend fun onPaymentSuccess(phonepeOrderId: String) {
        loading   = true
        step      = VerificationStep.VERIFYING_PAYMENT
        bgvStatus = BgvStatus.PAYMENT_DONE
        prefs.saveString(KEY_BGV_STATUS, BgvStatus.PAYMENT_DONE.name)

        try {
            val merchantId = merchantOrderId ?: throw Exception("Missing merchantOrderId")
            val txnId      = phonepeOrderId.ifBlank { merchantId }
            api.sendCriminalPaymentCallback(merchantId, txnId, "SUCCESS")
            createCandidate()
        } catch (e: Exception) {
            errorMessage = e.message ?: "Payment verification failed"
            step = VerificationStep.ERROR
        } finally {
            loading = false
        }
    }

    private suspend fun createCandidate() {
        step      = VerificationStep.CREATING_CANDIDATE
        bgvStatus = BgvStatus.CANDIDATE_CREATED
        prefs.saveString(KEY_BGV_STATUS, BgvStatus.CANDIDATE_CREATED.name)

        val result = api.createCandidateWithPackage(cachedForm!!)
        when {
            result.candidateId != null -> {
                val id = result.candidateId.toInt()
                createdCandidateId = id
                prefs.saveString(KEY_CANDIDATE_ID, id.toString())
                submitBGV(id)
            }
            result.isDuplicate -> {
                println("ℹ️ Candidate already exists")
                errorMessage = "Candidate already exists. Verification already initiated."
                step = VerificationStep.SUCCESS
            }
            else -> throw Exception(result.error ?: "Candidate creation failed")
        }
    }

    suspend fun checkVerificationStatus() {
        val id = createdCandidateId ?: return
        try {
            println("🌐 Checking status for candidateId=$id")
            val response = api.getCandidateStatus(id)
            println("📥 RAW STATUS RESPONSE = $response")

            val data       = response["data"]?.jsonObject?.get("data")?.jsonObject
            val statusCode = data?.get("overall_status_code")?.jsonPrimitive?.content?.toIntOrNull()
            val statusText = data?.get("overall_status")?.jsonPrimitive?.content
            val url        = data?.get("report_url")?.jsonPrimitive?.content

            println("📊 STATUS CODE = $statusCode")
            println("📊 STATUS TEXT = $statusText")

            if (!url.isNullOrBlank()) {
                reportUrl = url
                prefs.saveString(KEY_REPORT_URL, url)
            }

            val newStatus = when (statusCode) {
                1    -> BgvStatus.COMPLETED
                2    -> BgvStatus.FAILED
                else -> BgvStatus.VERIFICATION_STARTED
            }

            if (newStatus != bgvStatus) {
                bgvStatus = newStatus
                prefs.saveString(KEY_BGV_STATUS, newStatus.name)
            }

            // ✅ Court result parse karo
            parseCourtResult(data)

        } catch (e: Exception) {
            println("❌ Status check failed: ${e.message}")
        }
    }

    fun startStatusPolling(scope: CoroutineScope) {
        scope.launch {
            var attempts = 0
            while (
                isActive &&
                bgvStatus != BgvStatus.COMPLETED &&
                bgvStatus != BgvStatus.FAILED &&
                attempts < 60
            ) {
                checkVerificationStatus()
                attempts++
                delay(5000)
            }
        }
    }

    private suspend fun submitBGV(candidateId: Int) {
        step      = VerificationStep.SUBMITTING_BGV
        bgvStatus = BgvStatus.VERIFICATION_STARTED
        prefs.saveString(KEY_BGV_STATUS, BgvStatus.VERIFICATION_STARTED.name)

        try { api.submitBgvVerification(candidateId, cachedForm!!) }
        catch (e: Exception) { println("⚠️ BGV error (non-fatal): ${e.message}") }

        step = VerificationStep.SUCCESS
    }

    fun reset() {
        prefs.saveString(KEY_CANDIDATE_ID, "")
        prefs.saveString(KEY_BGV_STATUS, "")
        prefs.saveString(KEY_MERCHANT_ORDER, "")
        prefs.saveString(KEY_REPORT_URL, "")

        step                  = VerificationStep.FORM
        merchantOrderId       = null
        paymentToken          = null
        errorMessage          = ""
        cachedForm            = null
        createdCandidateId    = null
        bgvStatus             = BgvStatus.FORM_SUBMITTED
        reportUrl             = null
        searchedCandidateName = null
        courtCheckResult      = null   // ✅ reset
        amount                = 1
    }

    private fun parseBgvStatus(s: String) = when (s) {
        "COMPLETED"            -> BgvStatus.COMPLETED
        "FAILED"               -> BgvStatus.FAILED
        "VERIFICATION_STARTED" -> BgvStatus.VERIFICATION_STARTED
        "CANDIDATE_CREATED"    -> BgvStatus.CANDIDATE_CREATED
        "PAYMENT_DONE"         -> BgvStatus.PAYMENT_DONE
        else                   -> BgvStatus.VERIFICATION_STARTED
    }
}