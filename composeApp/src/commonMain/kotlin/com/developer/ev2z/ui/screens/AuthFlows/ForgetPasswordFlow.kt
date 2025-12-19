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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ForgetPasswordStep {
    ENTER_PHONE,
    VERIFY_OTP,
    RESET_PASSWORD
}

@Composable
fun ForgetPasswordFlow(
    authViewModel: AuthViewModel,
    preFilledPhone: String = "",
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {},
    onNotRegistered: (String) -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(ForgetPasswordStep.ENTER_PHONE) }
    var showExitDialog by remember { mutableStateOf(false) }

    val initialPhone = remember(preFilledPhone) {
        when {
            preFilledPhone.startsWith("+91") -> preFilledPhone.removePrefix("+91")
            preFilledPhone.length == 10 -> preFilledPhone
            else -> ""
        }
    }

    var phoneNumber by remember { mutableStateOf("") }

    val isLoading by authViewModel.loading.collectAsState()
    val otpVerified by authViewModel.otpVerified.collectAsState()
    val loginSuccess by authViewModel.loginSuccess.collectAsState()
    val error by authViewModel.error.collectAsState()

    // ✅ Cross-platform back button - ONLY on first screen
    if (currentStep == ForgetPasswordStep.ENTER_PHONE) {
        BackPressHandler {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit?", fontWeight = FontWeight.Bold) },
            text = { Text("Return to login screen?") },
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
            println("✅ OTP Verified! Moving to reset password")
            currentStep = ForgetPasswordStep.RESET_PASSWORD
            authViewModel.resetOtpVerified()
        }
    }

    LaunchedEffect(loginSuccess) {
        if (loginSuccess && currentStep == ForgetPasswordStep.RESET_PASSWORD) {
            println("✅ Password reset & auto-login successful!")
            authViewModel.resetLoginSuccess()
            onSuccess()
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
                    ForgetPasswordStep.ENTER_PHONE -> {
                        ForgetPasswordPhoneScreen(
                            viewModel = authViewModel,
                            preFilledPhone = initialPhone,
                            onBack = onBack,
                            onOtpSent = { phone ->
                                println("✅ OTP sent! Phone: $phone")
                                phoneNumber = "+91$phone"

                                GlobalScope.launch(Dispatchers.Main) {
                                    delay(300)
                                    currentStep = ForgetPasswordStep.VERIFY_OTP
                                }
                            },
                            onNotRegistered = { phone ->
                                println("📞 Not registered: $phone")
                                onNotRegistered(phone)
                            }
                        )
                    }

                    ForgetPasswordStep.VERIFY_OTP -> {
                        OTPScreen(
                            title = "Verify OTP",
                            subtitle = "Enter the OTP sent to $phoneNumber",
                            isLoading = isLoading,
                            error = error,
                            onOTPComplete = { otp ->
                                authViewModel.verifyForgotOtp(phoneNumber, otp)
                            },
                            onResendOTP = {
                                authViewModel.forgotPassword(phoneNumber)
                            },
                            onBack = {
                                currentStep = ForgetPasswordStep.ENTER_PHONE
                            }
                        )
                    }

                    ForgetPasswordStep.RESET_PASSWORD -> {
                        PasswordScreen(
                            title = "Reset Password",
                            subtitle = "Create a new password for your account",
                            buttonText = "Reset Password",
                            isLoading = isLoading,
                            error = error,
                            onPasswordSubmit = { password, confirmPassword ->
                                println("🔐 Resetting password for: $phoneNumber")
                                authViewModel.resetPassword(phoneNumber, password, confirmPassword)
                            },
                            onBack = {
                                println("🔙 Back to OTP screen")
                                currentStep = ForgetPasswordStep.VERIFY_OTP
                            }
                        )
                    }
                }
            }
        }
    }
}