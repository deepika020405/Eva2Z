package com.developer.ev2z.ui.screens.ChangePlan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class PaymentUiState {
    Success,
    Failed,
    Cancelled
}

@Composable
fun PaymentPlanScreen(
    state: PaymentUiState,
    amount: String? = null,
    transactionId: String? = null,
    onPrimaryAction: () -> Unit
) {

    val (title, message, primaryColor, icon) = when (state) {

        PaymentUiState.Success -> Quad(
            "Payment Successful",
            "Your plan has been renewed successfully.",
            Color(0xFF16A34A), // modern green
            Icons.Default.CheckCircle
        )

        PaymentUiState.Failed -> Quad(
            "Payment Failed",
            "We couldn't process your payment. Please try again.",
            Color(0xFFDC2626), // premium red
            Icons.Default.Error
        )

        PaymentUiState.Cancelled -> Quad(
            "Payment Cancelled",
            "You cancelled the payment process.",
            Color(0xFFDC2626), // 🔴 red theme
            Icons.Default.Cancel
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 36.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // 🔵 Premium Icon Badge
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(primaryColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1F2937)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                // 💰 Amount
                if (amount != null && state == PaymentUiState.Success) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Amount Paid",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        "₹$amount",
                        style = MaterialTheme.typography.headlineMedium,
                        color = primaryColor
                    )
                }

                // 🧾 Transaction ID
                transactionId?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Transaction ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF374151)
                    )
                }

                Spacer(Modifier.height(30.dp))

                // 🔘 Primary Button
                Button(
                    onClick = onPrimaryAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        when (state) {
                            PaymentUiState.Success -> "Done"
                            PaymentUiState.Failed -> "Retry Payment"
                            PaymentUiState.Cancelled -> "Go Back"
                        }
                    )
                }
            }
        }
    }
}

/* Helper */
data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)