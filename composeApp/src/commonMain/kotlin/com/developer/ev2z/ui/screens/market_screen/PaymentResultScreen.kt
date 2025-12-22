// commonMain/ui/screens/market_screen/PaymentResultScreen.kt
package com.developer.ev2z.ui.screens.market_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay

class PaymentResultScreen(
    private val isSuccess: Boolean,
    private val transactionId: String? = null,
    private val amount: String? = null,
    private val reason: String? = null
) : Screen {

    @Composable
    override fun Content() {
        PaymentResultContent(
            isSuccess = isSuccess,
            transactionId = transactionId,
            amount = amount,
            reason = reason
        )
    }
}

@Composable
fun PaymentResultContent(
    isSuccess: Boolean,
    transactionId: String?,
    amount: String?,
    reason: String?
) {
    val navigator = LocalNavigator.currentOrThrow
    val viewModel = ShopViewModelProvider.viewModel

    // ✅ Auto-navigate back after 3 seconds ONLY on success
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(3000)
            // ✅ Reset payment state
            viewModel.resetPaymentState()
            // ✅ Pop to root (shop screen)
            navigator.popUntilRoot()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSuccess) Color(0xFFF0F8F4) else Color(0xFFFFF5F5)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ✅ Success/Failed Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = if (isSuccess) Color(0xFF1B9F4A) else Color(0xFFE53935),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSuccess) "✓" else "✕",
                        fontSize = 60.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ✅ Title
                Text(
                    text = if (isSuccess) "Payment Successful!" else "Payment Failed",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Color(0xFF1B9F4A) else Color(0xFFE53935)
                )

                Spacer(Modifier.height(12.dp))

                // ✅ Message
                Text(
                    text = if (isSuccess) {
                        "Your order has been placed successfully"
                    } else {
                        reason ?: "Something went wrong. Please try again."
                    },
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // ✅ Details Card
                if (isSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(Color(0xFFF8F8F8))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            if (amount != null) {
                                DetailRow("Amount Paid", "₹$amount")
                                Spacer(Modifier.height(8.dp))
                            }

                            if (transactionId != null) {
                                DetailRow("Transaction ID", transactionId)
                            }
                        }
                    }
                } else {
                    // ✅ Failed Details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(Color(0xFFFFEBEE))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "❌ ${reason ?: "Payment cancelled or failed"}",
                                color = Color(0xFFE53935),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ✅ Action Buttons
                if (isSuccess) {
                    Button(
                        onClick = {
                            // ✅ Reset payment state before navigating
                            viewModel.resetPaymentState()
                            navigator.popUntilRoot()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(Color(0xFF1B9F4A))
                    ) {
                        Text("Continue Shopping", fontSize = 16.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                println("🔄 Retry Payment clicked")
                                // ✅ CRITICAL: Reset payment state before retry
                                viewModel.resetPaymentState()
                                // ✅ Pop back to PaymentScreen (removes result screen)
                                navigator.pop()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(Color(0xFFE53935))
                        ) {
                            Text("Retry Payment", fontSize = 16.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        TextButton(
                            onClick = {
                                // ✅ Reset payment state
                                viewModel.resetPaymentState()
                                // ✅ Go back to shop
                                navigator.popUntilRoot()
                            }
                        ) {
                            Text("Back to Shop", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}