package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.ui.screens.Auth.OTPScreen
import com.developer.ev2z.ui.screens.Auth.PasswordScreen
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
    var showExitDialog by remember { mutableStateOf(false) }

    val isLoading by authViewModel.loading.collectAsState()
    val otpVerified by authViewModel.otpVerified.collectAsState()
    val error by authViewModel.error.collectAsState()

    // ✅ Cross-platform back button - ONLY on first screen
    if (currentStep == RegistrationStep.VERIFY_OTP) {
        BackPressHandler {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Registration?", fontWeight = FontWeight.Bold) },
            text = { Text("Cancel registration and return to login?") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    onBack()
                }) {
                    Text("Yes", color = Color(0xFF6A9F50), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("No", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

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
                                authViewModel.signUp(name, fullPhone, "", email)
                            },
                            onBack = onBack
                        )
                    }

                    RegistrationStep.CREATE_PASSWORD -> {
                        val loginSuccess by authViewModel.loginSuccess.collectAsState()

                        LaunchedEffect(loginSuccess) {
                            if (loginSuccess) {
                                println("✅ Registration complete! Auto-login successful")
                                onSuccess()
                            }
                        }

                        PasswordScreen(
                            title = "Create Password",
                            subtitle = "Secure your account with a strong password",
                            buttonText = "Create Account",
                            isLoading = isLoading,
                            error = error,
                            onPasswordSubmit = { password, confirmPassword ->
                                val fullPhone = "+91$phone"
                                println("🔐 Calling savePasswordAndLogin...")
                                authViewModel.savePasswordAndLogin(name, fullPhone, password, email)
                            },
                            onBack = { currentStep = RegistrationStep.VERIFY_OTP }
                        )
                    }
                }
            }
        }
    }
}