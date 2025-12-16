package com.developer.ev2z.util

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            println("📱 SMS Message (User Approved): $message")

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
            println("❌ Context is not an Activity")
            return@DisposableEffect onDispose { }
        }

        println("🔔 Setting up SMS User Consent API...")

        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                    val extras = intent.extras
                    val smsRetrieverStatus = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

                    when (smsRetrieverStatus?.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            println("📩 SMS detected! Requesting user consent...")

                            val consentIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
                            }

                            try {
                                // ✅ CORRECT: Use activity.startIntentSenderForResult pattern
                                consentIntent?.let { intent ->
                                    activity.startIntentSenderForResult(
                                        intent.getParcelableExtra(SmsRetriever.EXTRA_CONSENT_INTENT),
                                        SMS_CONSENT_REQUEST,
                                        null,
                                        0,
                                        0,
                                        0
                                    )
                                } ?: run {
                                    println("❌ Consent intent is null")
                                }

                            } catch (e: Exception) {
                                println("❌ Exception launching consent: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                        CommonStatusCodes.TIMEOUT -> {
                            println("⏱️ SMS User Consent timeout")
                        }
                        else -> {
                            println("❌ SMS User Consent error: ${smsRetrieverStatus?.statusCode}")
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
            println("✅ SMS BroadcastReceiver registered")
        } catch (e: Exception) {
            println("❌ Failed to register receiver: ${e.message}")
        }

        val client = SmsRetriever.getClient(activity)
        val task = client.startSmsUserConsent(null)

        task.addOnSuccessListener {
            println("✅ SMS User Consent API started")
        }

        task.addOnFailureListener { exception ->
            println("❌ Failed to start SMS User Consent: ${exception.message}")
        }

        onDispose {
            println("🧹 Cleaning up SMS User Consent...")
            try {
                context.unregisterReceiver(smsReceiver)
                println("✅ Receiver unregistered")
            } catch (e: Exception) {
                println("⚠️ Receiver already unregistered: ${e.message}")
            }
        }
    }
}

private const val SMS_CONSENT_REQUEST = 2 // Request code

private fun extractOTP(message: String): String? {
    println("🔍 Extracting OTP from: $message")
    val otpPattern = "\\b\\d{6}\\b".toRegex()
    val result = otpPattern.find(message)?.value
    println("🎯 Extraction result: $result")
    return result
}