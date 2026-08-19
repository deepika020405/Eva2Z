package com.developer.ev2z.ui.screens.ChangePlan


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.phonepe.openPhonePeSdk
import com.developer.ev2z.ui.screens.market_screen.PaymentResultScreen
import com.developer.ev2z.theme.AppPageBg


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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppPageBg
    ) {
        when (paymentState) {

            is PaymentState.Processing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF6A9F50),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Redirecting to PhonePe…",
                                fontSize = 15.sp,
                                color = Color(0xFF555555),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

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

            is PaymentState.Cancelled -> {
                PaymentPlanScreen(
                    state = PaymentUiState.Cancelled,
                    onPrimaryAction = { renewViewModel.reset() }
                )
            }

            is PaymentState.Failed -> {
                PaymentPlanScreen(
                    state = PaymentUiState.Failed,
                    onPrimaryAction = { renewViewModel.reset() }
                )
            }

            else -> {
                when {
                    planState.isLoading -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(
                                color = Color(0xFF6A9F50),
                                strokeWidth = 3.dp
                            )
                        }
                    }

                    planState.error != null -> {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = planState.error ?: "Something went wrong",
                                    fontSize = 15.sp,
                                    color = Color(0xFF555555),
                                    textAlign = TextAlign.Center
                                )
                            }
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