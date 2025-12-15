package com.developer.ev2z.util

import androidx.compose.runtime.Composable

expect class OTPReader {
    fun startListening(onOTPReceived: (String) -> Unit)
    fun stopListening()
}

@Composable
expect fun OTPAutoReader(onOTPReceived: (String) -> Unit)