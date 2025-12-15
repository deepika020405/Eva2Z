package com.developer.ev2z.util

import android.app.Activity
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.developer.ev2z.receiver.OTPBroadcastReceiver
import com.google.android.gms.auth.api.phone.SmsRetriever

actual class OTPReader {
    private var receiver: OTPBroadcastReceiver? = null

    actual fun startListening(onOTPReceived: (String) -> Unit) {
        println("✅ Android: Starting SMS Retriever")
    }

    actual fun stopListening() {
        println("❌ Android: Stopping SMS Retriever")
    }
}

@Composable
actual fun OTPAutoReader(onOTPReceived: (String) -> Unit) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val receiver = OTPBroadcastReceiver().apply {
            this.onOTPReceived = onOTPReceived
        }

        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        context.registerReceiver(receiver, filter)

        // Start SMS Retriever
        if (context is Activity) {
            val client = SmsRetriever.getClient(context)
            client.startSmsRetriever().addOnSuccessListener {
                println("✅ Android SMS Retriever started")
            }.addOnFailureListener {
                println("❌ SMS Retriever failed: ${it.message}")
            }
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                println("⚠️ Receiver already unregistered")
            }
        }
    }
}