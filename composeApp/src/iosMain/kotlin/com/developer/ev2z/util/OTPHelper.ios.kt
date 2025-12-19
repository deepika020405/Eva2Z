package com.developer.ev2z.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UserNotifications.*
import kotlinx.cinterop.*

actual class OTPReader {
    actual fun startListening(onOTPReceived: (String) -> Unit) {
        println("✅ iOS: Starting OTP listener")
        // iOS uses system OTP autofill (no manual implementation needed)
    }

    actual fun stopListening() {
        println("❌ iOS: Stopping OTP listener")
    }
}

@Composable
actual fun OTPAutoReader(onOTPReceived: (String) -> Unit) {
    // ✅ iOS automatically handles OTP autofill via system
    // No manual implementation needed - iOS shows OTP suggestion above keyboard
    DisposableEffect(Unit) {
        println("📱 iOS OTP AutoFill enabled (system handles it)")

        onDispose {
            println("🍎 iOS OTP cleanup")
        }
    }
}
