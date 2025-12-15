package com.developer.ev2z.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class OTPBroadcastReceiver : BroadcastReceiver() {

    var onOTPReceived: ((String) -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == SmsRetriever.SMS_RETRIEVED_ACTION) {
            val extras = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    val message = extras.get(SmsRetriever.EXTRA_SMS_MESSAGE) as? String
                    message?.let {
                        val otp = extractOTP(it)
                        otp?.let { code ->
                            println("📱 Android OTP Auto-read: $code")
                            onOTPReceived?.invoke(code)
                        }
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    println("⏱️ SMS Retriever timeout")
                }
            }
        }
    }

    private fun extractOTP(message: String): String? {
        // Extract 6-digit OTP: "Your OTP for registration is 516636"
        val otpPattern = "\\b\\d{6}\\b".toRegex()
        return otpPattern.find(message)?.value
    }
}