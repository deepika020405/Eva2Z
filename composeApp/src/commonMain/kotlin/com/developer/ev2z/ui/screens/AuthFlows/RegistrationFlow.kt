package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developer.ev2z.ui.screens.OTP.OTPScreen
import com.developer.ev2z.ui.screens.Password.PasswordScreen
import com.developer.ev2z.viewmodel.AuthViewModel

enum class RegistrationStep {
    VERIFY_OTP,
    CREATE_PASSWORD
}

@Composable
fun RegistrationFlow(
    authViewModel: AuthViewModel,
    name: String,
    phone: String,
    email: String,
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(RegistrationStep.VERIFY_OTP) }

    val isLoading by authViewModel.loading.collectAsState()
    val otpVerified by authViewModel.otpVerified.collectAsState()
    val error by authViewModel.error.collectAsState()

    // ✅ Listen for OTP verification success
    LaunchedEffect(otpVerified) {
        if (otpVerified) {
            currentStep = RegistrationStep.CREATE_PASSWORD
            authViewModel.resetOtpVerified()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFE6EDE3)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight * 0.20f)
                    .background(
                        color = Color(0xFFFFFFFF),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                when (currentStep) {
                    RegistrationStep.VERIFY_OTP -> {
                        OTPScreen(
                            title = "Verify OTP",
                            subtitle = "Enter the OTP sent to +91$phone",
                            isLoading = isLoading,
                            error = error,
                            onOTPComplete = { otp ->
                                val fullPhone = "+91$phone"
                                authViewModel.verifyOtp(fullPhone, otp)
                            },
                            onResendOTP = {
                                val fullPhone = "+91$phone"
                                authViewModel.resendOtp(fullPhone)
                            },
                            onBack = onBack
                        )
                    }

                    RegistrationStep.CREATE_PASSWORD -> {
                        PasswordScreen(
                            title = "Create Password",
                            subtitle = "Secure your account with a strong password",
                            buttonText = "Creating Account...",
                            isLoading = isLoading,
                            error = error,
                            onPasswordSubmit = { password, confirmPassword ->
                                val fullPhone = "+91$phone"
                                // Now call signup with password
                                authViewModel.signUp(name, fullPhone, password, email)
                                onSuccess()
                            },
                            onBack = { currentStep = RegistrationStep.VERIFY_OTP }
                        )
                    }
                }
            }
        }
    }
}