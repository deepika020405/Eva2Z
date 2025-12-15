package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    onNotRegistered: (String) -> Unit = {} // ✅ NEW: Callback for not registered
) {
    var currentStep by remember { mutableStateOf(ForgetPasswordStep.ENTER_PHONE) }

    // ✅ Extract digits from phone number (remove +91 if present)
    val initialPhone = remember(preFilledPhone) {
        when {
            preFilledPhone.startsWith("+91") -> preFilledPhone.removePrefix("+91")
            preFilledPhone.length == 10 -> preFilledPhone
            else -> ""
        }
    }

    // ✅ Store full phone with country code for API calls
    var phoneNumber by remember { mutableStateOf("") }

    val isLoading by authViewModel.loading.collectAsState()
    val otpVerified by authViewModel.otpVerified.collectAsState()
    val loginSuccess by authViewModel.loginSuccess.collectAsState()
    val error by authViewModel.error.collectAsState()

    // ✅ Auto-navigate when OTP verified
    LaunchedEffect(otpVerified) {
        if (otpVerified) {
            println("✅ OTP Verified! Moving to reset password")
            currentStep = ForgetPasswordStep.RESET_PASSWORD
            authViewModel.resetOtpVerified()
        }
    }

    // ✅ Auto-login after password reset
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
                            onOtpSent = { phone -> // ✅ Receive actual phone
                                println("✅ OTP sent! Phone: $phone")
                                phoneNumber = "+91$phone" // ✅ Store with country code

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