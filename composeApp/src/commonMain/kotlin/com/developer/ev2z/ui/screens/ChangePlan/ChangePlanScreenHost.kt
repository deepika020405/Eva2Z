package com.developer.ev2z.ui.screens.ChangePlan


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.phonepe.openPhonePeSdk
import com.developer.ev2z.ui.screens.market_screen.PaymentResultScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlanScreenHost(
    onBack: () -> Unit
) {
    val planViewModel = remember { ChangePlanViewModel() }
    val planState by planViewModel.state.collectAsState()

    val renewViewModel = remember { RenewPlanViewModel(ApiService()) }
    val paymentState by renewViewModel.paymentState.collectAsState()
    val phonePeToken by renewViewModel.phonePeToken.collectAsState()

    // 🔥 PHONEPE SDK TRIGGER
    LaunchedEffect(phonePeToken) {
        phonePeToken?.let { token ->
            openPhonePeSdk(
                paymentUrl = token,
                onResult = { result ->
                    renewViewModel.onPaymentResult(
                        success = result.success,
                        phonePeOrderId = result.transactionId ?: "",
                        message = result.message
                    )
                }
            )
        }
    }

    // ✅ SINGLE UI CONTROLLER
    when {
        paymentState is PaymentState.Processing -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Redirecting to PhonePe…")
                }
            }
        }

        paymentState is PaymentState.Success -> {
            val ps = paymentState as PaymentState.Success
            PaymentResultScreen(
                isSuccess = true,
                transactionId = ps.txnId ?: "N/A",
                amount = ps.amount.toString()
            )
        }

        paymentState is PaymentState.Failed -> {
            val pf = paymentState as PaymentState.Failed
            PaymentResultScreen(
                isSuccess = false,
                reason = pf.reason,
                amount = pf.amount.toString()
            )
        }

        planState.isLoading -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        planState.error != null -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(planState.error ?: "Something went wrong")
            }
        }

        else -> {
            ChangePlanScreen(
                devices = planState.devices,
                onBack = {
                    renewViewModel.reset()
                    onBack()
                },
                onRenewConfirm = { deviceId, duration ->
                    renewViewModel.startRenewPlan(deviceId, duration)
                }
            )
        }
    }




    /* ===============================
       UI STATE HANDLING
    =============================== */
    when {
        planState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        planState.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(planState.error ?: "Something went wrong")
            }
        }

        else -> {
            ChangePlanScreen(
                devices = planState.devices,
                onBack = onBack,
                onRenewConfirm = { deviceId, duration ->
                    // 🔥 REAL RENEW FLOW
                    renewViewModel.startRenewPlan(
                        deviceId = deviceId,
                        duration = duration
                    )
                }
            )
        }
    }
}