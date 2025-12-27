package com.developer.ev2z.util

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

actual class OTPReader {
    actual fun startListening(onOTPReceived: (String) -> Unit) {
        println("✅ Android: SMS User Consent API (No SMS permission needed)")
    }

    actual fun stopListening() {
        println("❌ Android: Stopping SMS User Consent")
    }
}

@Composable
actual fun OTPAutoReader(onOTPReceived: (String) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    // ✅ Launcher for user consent dialog
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            println("📱 SMS Message: $message")

            message?.let {
                val otp = extractOTP(it)
                otp?.let { code ->
                    println("✅ OTP Extracted: $code")
                    onOTPReceived(code)
                }
            }
        } else {
            println("❌ User denied SMS consent")
        }
    }

    DisposableEffect(Unit) {
        if (activity == null) {
            println("❌ Not an Activity context")
            return@DisposableEffect onDispose { }
        }

        println("🔔 Setting up SMS User Consent API...")

        // ✅ BroadcastReceiver for SMS detection
        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    val extras = intent.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

                    when (status?.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            println("📩 SMS detected! Showing consent dialog...")

                            // ✅ Get consent intent (handle both API levels)
                            val consentIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                extras.getParcelable(
                                    SmsRetriever.EXTRA_CONSENT_INTENT,
                                    Intent::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
                            }

                            // ✅ Launch consent dialog if intent is not null
                            consentIntent?.let { intent ->
                                try {
                                    consentLauncher.launch(intent)
                                } catch (e: Exception) {
                                    println("❌ Error launching consent: ${e.message}")
                                }
                            } ?: run {
                                println("❌ Consent intent is null")
                            }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            println("⏱️ SMS Retriever timeout (5 minutes)")
                        }
                        else -> {
                            println("❌ SMS Retriever error: ${status?.statusCode}")
                        }
                    }
                }
            }
        }

        // ✅ Register receiver
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    smsReceiver,
                    intentFilter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(smsReceiver, intentFilter)
            }
            println("✅ SMS Receiver registered")
        } catch (e: Exception) {
            println("❌ Failed to register receiver: ${e.message}")
        }

        // ✅ Start SMS User Consent API
        val client = SmsRetriever.getClient(activity)
        val task = client.startSmsUserConsent(null) // null = any sender

        task.addOnSuccessListener {
            println("✅ SMS User Consent API started successfully")
            println("📱 Waiting for SMS... (5 min timeout)")
        }

        task.addOnFailureListener { e ->
            println("❌ Failed to start SMS User Consent: ${e.message}")
        }

        // ✅ Cleanup
        onDispose {
            println("🧹 Cleaning up SMS User Consent...")
            try {
                context.unregisterReceiver(smsReceiver)
                println("✅ Receiver unregistered")
            } catch (e: Exception) {
                println("⚠️ Receiver already unregistered")
            }
        }
    }
}

/**
 * Extract 6-digit OTP from SMS message
 * Format: "Your OTP for registration is 667295\nEVA2Z GPS"
 */
private fun extractOTP(message: String): String? {
    println("🔍 Extracting OTP from: $message")

    // ✅ Match 6 consecutive digits
    val otpPattern = "\\b\\d{6}\\b".toRegex()
    val result = otpPattern.find(message)?.value

    println("🎯 Extracted OTP: $result")
    return result
}