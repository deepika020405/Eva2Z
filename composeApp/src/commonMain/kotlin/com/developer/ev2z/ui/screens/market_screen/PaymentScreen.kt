// commonMain/ui/screens/market_screen/PaymentScreen.kt
package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.developer.ev2z.phonepe.PaymentState
import com.developer.ev2z.util.Analytics

class PaymentScreen : Screen {
    @Composable
    override fun Content() {
        PaymentScreenContent()
    }
}

@Composable
fun PaymentScreenContent() {
    val viewModel = ShopViewModelProvider.viewModel
    val state by viewModel.state.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val navigator = LocalNavigator.currentOrThrow

    // ✅ Handle payment state changes
    LaunchedEffect(paymentState) {
        when (val ps = paymentState) {
            is PaymentState.Success -> {
                println("✅ Payment Success - Navigating to success screen")
                navigator.replace(
                    PaymentResultScreen(
                        isSuccess = true,
                        transactionId = ps.txnId,
                        amount = state.finalAmount ?: state.totalPrice
                    )
                )
            }
            is PaymentState.Failed -> {
                println("❌ Payment Failed - Navigating to failed screen")
                navigator.replace(
                    PaymentResultScreen(
                        isSuccess = false,
                        reason = ps.reason,
                        amount = state.finalAmount ?: state.totalPrice
                    )
                )
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Payment Method",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "✕",
                fontSize = 24.sp,
                modifier = Modifier.clickable {
                    // ✅ Reset payment state before going back
                    viewModel.resetPaymentState()
                    navigator.pop()
                }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Amount Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(P_GreenSurface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Total Amount", color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text(
                    "₹${state.finalAmount ?: state.totalPrice ?: "0"}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = P_Green
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Select Payment Gateway",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(16.dp))

        // 💳 PhonePe Option
        PaymentMethodCard(
            icon = "📱",
            name = "PhonePe",
            description = "UPI, Cards, Wallets & More",
            onClick = {
                println("📱 PhonePe button clicked")
                // ✅ Reset state before initiating new payment
                Analytics.logEvent(
                    name = "add_payment_info",
                    params = mapOf(
                        "currency" to "INR",
                        "value" to (state.finalAmount ?: state.totalPrice ?: 0),
                        "items_count" to state.cart.size
                    )
                )
                viewModel.resetPaymentState()
                viewModel.initiatePhonePePayment()
            },
            enabled = paymentState !is PaymentState.Processing
        )

        Spacer(Modifier.height(12.dp))

        // 💳 Razorpay Option (Future)
//        PaymentMethodCard(
//            icon = "💰",
//            name = "Razorpay",
//            description = "Coming Soon",
//            onClick = { /* TODO */ },
//            enabled = false
//        )

        Spacer(Modifier.weight(1f))

        // Loading Indicator
        if (paymentState is PaymentState.Processing) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Creating order...", color = Color.Gray)
                }
            }
        }

        // Error Display (only for immediate errors, not payment failures)
        if (paymentState is PaymentState.Failed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "❌ ${(paymentState as PaymentState.Failed).reason}",
                        color = Color.Red,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    // ✅ Retry button for immediate errors
                    Button(
                        onClick = {
                            println("🔄 Retry button clicked")
                            viewModel.resetPaymentState()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(P_Green)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    icon: String,
    name: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            if (enabled) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 32.sp)

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) Color.Black else Color.Gray
                )
                Text(
                    description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            if (enabled) {
                Text("→", fontSize = 24.sp, color = P_Green)
            }
        }
    }
}