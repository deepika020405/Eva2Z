package com.developer.ev2z.ui.screens.ChangePlan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.request.RenewPlanRequest
import com.developer.ev2z.response.RenewPlanResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class RenewPlanViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _paymentState =
        MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState = _paymentState.asStateFlow()

    private val _phonePeToken = MutableStateFlow<String?>(null)
    val phonePeToken = _phonePeToken.asStateFlow()

    private var currentAmount: Int = 0

    // ✅ ONLY FUNCTION DEFINITION HERE
    fun startRenewPlan(
        deviceId: String,   // 🔥 THIS MUST BE IMEI
        duration: String
    ) {
        if (_paymentState.value == PaymentState.Processing) {
            println("⚠️ Payment already in Processing state")
            return
        }

        viewModelScope.launch {
            try {
                println("🔥 startRenewPlan → deviceId=$deviceId duration=$duration")

                _paymentState.value = PaymentState.Processing

                val planDays = when (duration) {
                    "3 Months" -> 90
                    "6 Months" -> 180
                    else -> 365
                }

                currentAmount = when (planDays) {
                    90 -> 399
                    180 -> 699
                    else -> 1
                }

                val request = RenewPlanRequest(
                    device_id = deviceId,   // ✅ IMEI
                    plan_days = planDays,
                    amount = currentAmount
                )

                println("🌐 Calling renewDevicePlan API → $request")

                val response = apiService.renewDevicePlan(request)

                println("✅ orderToken = ${response.orderToken}")

                _phonePeToken.value = response.orderToken

            } catch (e: Exception) {
                println("❌ Renew API failed → ${e.message}")

                _paymentState.value = PaymentState.Failed(
                    reason = e.message ?: "Renew failed",
                    amount = currentAmount
                )
            }
        }
    }

    fun onPaymentResult(
        success: Boolean,
        txnId: String,
        message: String?
    ) {
        println("📲 PhonePe callback → success=$success txnId=$txnId message=$message")

        _paymentState.value =
            if (success)
                PaymentState.Success(txnId, currentAmount)
            else
                PaymentState.Failed(
                    message ?: "Payment cancelled",
                    currentAmount
                )

        _phonePeToken.value = null
    }

    fun reset() {
        println("🔄 Resetting payment state")
        _paymentState.value = PaymentState.Idle
        _phonePeToken.value = null
    }
}