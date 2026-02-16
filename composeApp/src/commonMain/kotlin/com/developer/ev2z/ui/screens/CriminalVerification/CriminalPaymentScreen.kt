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
    paymentUrl: String,
    amount: Int,
    onSuccess: (String?) -> Unit,
    onFailure: (String?) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {

        Text("Pay ₹$amount for Criminal Verification")

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                openPhonePeSdk(paymentUrl) { result ->
                    if (result.success) {
                        onSuccess(result.transactionId)
                    } else {
                        onFailure(result.message)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Pay with PhonePe")
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onBack) {
            Text("Cancel")
        }
    }
}