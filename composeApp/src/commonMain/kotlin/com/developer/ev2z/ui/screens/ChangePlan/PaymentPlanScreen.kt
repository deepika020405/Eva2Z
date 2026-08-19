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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.theme.AppPageBg

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

    val (title, message, primaryColor, secondaryBg, icon) = when (state) {
        PaymentUiState.Success -> Quint(
            "Payment Successful",
            "Your plan has been renewed successfully.",
            Color(0xFF6A9F50),
            Color(0xFFF0F7EC),
            Icons.Default.CheckCircle
        )
        PaymentUiState.Failed -> Quint(
            "Payment Failed",
            "We couldn't process your payment. Please try again.",
            Color(0xFFDC2626),
            Color(0xFFFEF2F2),
            Icons.Default.Error
        )
        PaymentUiState.Cancelled -> Quint(
            "Payment Cancelled",
            "You cancelled the payment process.",
            Color(0xFFE65100),
            Color(0xFFFFF3E0),
            Icons.Default.Cancel
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppPageBg)
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val isCompact = screenHeight < 600.dp

        val iconBadgeSize = if (isCompact) 72.dp else 88.dp
        val iconInnerSize = if (isCompact) 40.dp else 48.dp
        val titleSize = if (isCompact) 20.sp else 24.sp
        val bodySize = if (isCompact) 13.sp else 14.sp
        val amountSize = if (isCompact) 28.sp else 32.sp
        val buttonHeight = if (isCompact) 48.dp else 52.dp
        val buttonTextSize = if (isCompact) 15.sp else 16.sp
        val cardHorizontalPad = (screenWidth * 0.05f).coerceIn(16.dp, 24.dp)
        val cardInnerPad = if (isCompact) 24.dp else 32.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = cardHorizontalPad),
            verticalArrangement = if (isCompact) Arrangement.Top else Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isCompact) Spacer(Modifier.height(32.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = cardInnerPad, horizontal = 20.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icon Badge
                    Box(
                        modifier = Modifier
                            .size(iconBadgeSize)
                            .background(secondaryBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(iconInnerSize)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Title
                    Text(
                        text = title,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Message
                    Text(
                        text = message,
                        fontSize = bodySize,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center,
                        lineHeight = (bodySize.value * 1.5f).sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Amount
                    if (amount != null && state == PaymentUiState.Success) {
                        Spacer(Modifier.height(20.dp))

                        Text(
                            "Amount Paid",
                            fontSize = 12.sp,
                            color = Color(0xFF999999),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "₹$amount",
                            fontSize = amountSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A9F50)
                        )
                    }

                    // Transaction ID
                    transactionId?.let {
                        Spacer(Modifier.height(16.dp))

                        Surface(
                            color = Color(0xFFF8F8F8),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Transaction ID",
                                    fontSize = 11.sp,
                                    color = Color(0xFF999999),
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    it,
                                    fontSize = 13.sp,
                                    color = Color(0xFF333333),
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Action Button
                    Button(
                        onClick = onPrimaryAction,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state == PaymentUiState.Success)
                                Color(0xFF6A9F50)
                            else
                                primaryColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                    ) {
                        Text(
                            when (state) {
                                PaymentUiState.Success -> "Done"
                                PaymentUiState.Failed -> "Retry Payment"
                                PaymentUiState.Cancelled -> "Go Back"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = buttonTextSize
                        )
                    }
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

data class Quint<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)