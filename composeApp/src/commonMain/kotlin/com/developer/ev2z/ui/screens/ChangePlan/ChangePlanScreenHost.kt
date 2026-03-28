package com.developer.ev2z.ui.screens.ChangePlan


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.phonepe.openPhonePeSdk
import com.developer.ev2z.ui.screens.market_screen.PaymentResultScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlanScreenHost(
    onBack: () -> Unit
) {
    PlatformBackHandler(enabled = true) { onBack() }

    val planViewModel = remember { ChangePlanViewModel() }
    val planState by planViewModel.state.collectAsState()

    val renewViewModel = remember { RenewPlanViewModel(ApiService()) }
    val paymentState by renewViewModel.paymentState.collectAsState()
    val phonePeToken by renewViewModel.phonePeToken.collectAsState()

    var sdkLaunched by remember { mutableStateOf(false) }

    // 🔥 SAFE SDK TRIGGER
    LaunchedEffect(phonePeToken) {
        if (phonePeToken != null && !sdkLaunched) {
            sdkLaunched = true
            openPhonePeSdk(
                paymentUrl = phonePeToken!!,
                onResult = { result ->
                    renewViewModel.onPaymentResult(
                        success = result.success,
                        phonePeOrderId = result.transactionId ?: "",
                        message = result.message
                    )
                    sdkLaunched = false
                }
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {

        when (paymentState) {

            // 🔄 PROCESSING
            is PaymentState.Processing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Redirecting to PhonePe…")
                    }
                }
            }

            // ✅ SUCCESS
            is PaymentState.Success -> {
                val ps = paymentState as PaymentState.Success
                PaymentPlanScreen(
                    state = PaymentUiState.Success,
                    amount = ps.amount.toString(),
                    transactionId = ps.txnId,
                    onPrimaryAction = {
                        renewViewModel.reset()
                        onBack()
                    }
                )
            }

            // ⚪ CANCELLED
            is PaymentState.Cancelled -> {
                PaymentPlanScreen(
                    state = PaymentUiState.Cancelled,
                    onPrimaryAction = { renewViewModel.reset() }
                )
            }

            // ❌ FAILED
            is PaymentState.Failed -> {
                PaymentPlanScreen(
                    state = PaymentUiState.Failed,
                    onPrimaryAction = { renewViewModel.reset() }
                )
            }

            // 🟢 NORMAL FLOW
            else -> {
                when {
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
            }
        }
    }
}