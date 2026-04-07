package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.phonepe.openPhonePeSdk

@Composable
fun CriminalPaymentScreen(
    paymentToken: String,
    amount: Int,
    onSuccess: (String?) -> Unit,
    onFailure: (String?) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CV_Background)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))

            Surface(
                shape = CircleShape,
                color = CV_PrimaryBg,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = CV_Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Secure Payment",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CV_TextPrimary
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Complete your verification payment",
                fontSize = 14.sp,
                color = CV_TextSec
            )
        }

        // Payment Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CV_Surface),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Amount to Pay",
                    fontSize = 13.sp,
                    color = CV_TextHint
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "\u20B9$amount",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = CV_Primary
                )

                Spacer(Modifier.height(16.dp))

                HorizontalDivider(color = CV_Border, thickness = 0.5.dp)

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "You will be redirected to PhonePe to complete the payment securely.",
                    fontSize = 13.sp,
                    color = CV_TextSec,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        // Buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    openPhonePeSdk(paymentToken) { result ->
                        if (result.success) onSuccess(result.transactionId)
                        else onFailure(result.message)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CV_Primary)
            ) {
                Icon(
                    Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Pay with PhonePe",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cancel Payment",
                    color = CV_TextSec,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}