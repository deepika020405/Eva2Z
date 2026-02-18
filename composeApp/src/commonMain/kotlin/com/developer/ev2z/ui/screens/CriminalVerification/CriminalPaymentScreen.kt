package com.developer.ev2z.ui.screens.CriminalVerification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.phonepe.openPhonePeSdk
import com.developer.ev2z.ui.screens.social_media.openUrl
import io.ktor.websocket.Frame

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔹 Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Secure Payment",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Complete your verification payment",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 🔹 Payment Card
        androidx.compose.material3.ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Amount to Pay",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "₹$amount",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "You will be redirected to PhonePe to complete the payment securely.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 🔹 Buttons
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
                shape = MaterialTheme.shapes.large
            ) {
                Text("Pay with PhonePe")
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Payment")
            }
        }
    }
}