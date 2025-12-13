package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developer.ev2z.ui.screens.ForgetPassword.ForgetPasswordPhoneScreen
import com.developer.ev2z.ui.screens.OTP.OTPScreen
import com.developer.ev2z.ui.screens.Password.PasswordScreen
import com.developer.ev2z.viewmodel.AuthViewModel

enum class ForgetPasswordStep {
    ENTER_PHONE,
    VERIFY_OTP,
    RESET_PASSWORD
}

@Composable
fun ForgetPasswordFlow(
    authViewModel: AuthViewModel,
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    var currentStep by remember { mutableStateOf(ForgetPasswordStep.ENTER_PHONE) }
    var phoneNumber by remember { mutableStateOf("") }

    val isLoading by authViewModel.loading.collectAsState()
    val otpVerified by authViewModel.otpVerified.collectAsState()
    val error by authViewModel.error.collectAsState()

    // ✅ Auto-navigate to password screen when OTP is verified
    LaunchedEffect(otpVerified) {
        if (otpVerified) {
            println("✅ OTP Verified! Moving to reset password")
            currentStep = ForgetPasswordStep.RESET_PASSWORD
            authViewModel.resetOtpVerified()
        }
    }

    // ✅ Listen for password reset success and auto-login
    val loginSuccess by authViewModel.loginSuccess.collectAsState()
    LaunchedEffect(loginSuccess) {
        if (loginSuccess && currentStep == ForgetPasswordStep.RESET_PASSWORD) {
            println("✅ Password reset & auto-login successful!")
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
                            authViewModel = authViewModel,
                            onContinue = { phone ->
                                println("📞 Phone: $phone")
                                phoneNumber = phone
                                currentStep = ForgetPasswordStep.VERIFY_OTP
                            },
                            onBack = onBack
                        )
                    }

                    ForgetPasswordStep.VERIFY_OTP -> {
                        var otp1 by remember { mutableStateOf("") }
                        var otp2 by remember { mutableStateOf("") }
                        var otp3 by remember { mutableStateOf("") }
                        var otp4 by remember { mutableStateOf("") }
                        var otp5 by remember { mutableStateOf("") }
                        var otp6 by remember { mutableStateOf("") }

                        val otpList = listOf(otp1, otp2, otp3, otp4, otp5, otp6)
                        val isOTPComplete = otpList.all { it.isNotEmpty() }

                        // ✅ Auto-submit OTP when all 6 digits filled
                        LaunchedEffect(isOTPComplete) {
                            if (isOTPComplete && !isLoading) {
                                val fullOTP = "$otp1$otp2$otp3$otp4$otp5$otp6"
                                println("🔐 Auto-submitting OTP: $fullOTP")
                                authViewModel.verifyForgotOtp(phoneNumber, fullOTP)
                            }
                        }

                        OTPScreen(
                            title = "Verify OTP",
                            subtitle = "Enter the OTP sent to $phoneNumber",
                            isLoading = isLoading,
                            error = error,
                            onOTPComplete = { otp ->
                                authViewModel.verifyForgotOtp(phoneNumber, otp)
                            },
                            onResendOTP = {
                                authViewModel.resendOtp(phoneNumber)
                            },
                            onBack = { currentStep = ForgetPasswordStep.ENTER_PHONE }
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
                                println("🔐 Resetting password")
                                authViewModel.resetPassword(phoneNumber, password, confirmPassword)
                            },
                            onBack = { currentStep = ForgetPasswordStep.VERIFY_OTP }
                        )
                    }
                }
            }
        }
    }
}