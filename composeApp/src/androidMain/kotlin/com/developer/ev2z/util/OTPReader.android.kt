package com.developer.ev2z.util

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

actual class OTPReader {
    actual fun startListening(onOTPReceived: (String) -> Unit) {
        println("✅ Android: SMS User Consent API")
    }

    actual fun stopListening() {
        println("❌ Android: Stopping SMS User Consent")
    }
}

@Composable
actual fun OTPAutoReader(onOTPReceived: (String) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        println("📱 Consent Result Code: ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
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
            println("❌ User denied (code: ${result.resultCode})")
        }
    }

    DisposableEffect(Unit) {
        if (activity == null) {
            println("❌ Not an Activity")
            return@DisposableEffect onDispose { }
        }

        println("🔔 Setting up SMS User Consent API...")

        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                println("📩 onReceive called")

                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    println("📩 SMS_RETRIEVED_ACTION detected")

                    val extras = intent.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

                    println("📩 Status: ${status?.statusCode}")

                    when (status?.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            println("📩 SMS detected!")

                            try {
                                // ✅ CORRECT: Extract PendingIntent directly from extras
                                val consentIntent: PendingIntent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    extras.getParcelable(
                                        SmsRetriever.EXTRA_CONSENT_INTENT,
                                        PendingIntent::class.java
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
                                }

                                println("📩 PendingIntent: $consentIntent")

                                consentIntent?.let { pendingIntent ->
                                    // ✅ Extract IntentSender from PendingIntent
                                    val intentSender: IntentSender = pendingIntent.intentSender
                                    println("📩 IntentSender: $intentSender")

                                    // ✅ Create IntentSenderRequest
                                    val request = IntentSenderRequest.Builder(intentSender).build()

                                    println("📩 Launching consent dialog...")
                                    consentLauncher.launch(request)
                                } ?: run {
                                    println("❌ PendingIntent is null")
                                }

                            } catch (e: Exception) {
                                println("❌ Error: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            println("⏱️ Timeout (5 min)")
                        }
                        else -> {
                            println("❌ Error: ${status?.statusCode}")
                        }
                    }
                }
            }
        }

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
            println("✅ Receiver registered")
        } catch (e: Exception) {
            println("❌ Register failed: ${e.message}")
        }

        val client = SmsRetriever.getClient(activity)
        val task = client.startSmsUserConsent(null)

        task.addOnSuccessListener {
            println("✅ SMS User Consent started")
        }

        task.addOnFailureListener { e ->
            println("❌ Start failed: ${e.message}")
        }

        onDispose {
            println("🧹 Cleanup...")
            try {
                context.unregisterReceiver(smsReceiver)
                println("✅ Unregistered")
            } catch (e: Exception) {
                println("⚠️ Already unregistered")
            }
        }
    }
}

private fun extractOTP(message: String): String? {
    println("🔍 Extracting OTP from: $message")
    val otpPattern = "\\b\\d{6}\\b".toRegex()
    val result = otpPattern.find(message)?.value
    println("🎯 Result: $result")
    return result
}