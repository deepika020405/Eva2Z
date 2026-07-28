package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.ui.components.isKeyboardVisible
import com.developer.ev2z.ui.screens.Auth.OTPScreen
import com.developer.ev2z.ui.screens.Auth.PasswordScreen
import com.developer.ev2z.viewmodel.AuthViewModel
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

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
        // Pass digits only; ForgetPasswordPhoneScreen owns the country code selection
        preFilledPhone.filter { it.isDigit() }
    }

    var phoneNumber by remember { mutableStateOf("") }

    val isLoading by authViewModel.loading.collectAsState()
    val otpVerified by authViewModel.otpVerified.collectAsState()
    val loginSuccess by authViewModel.loginSuccess.collectAsState()
    val error by authViewModel.error.collectAsState()
    val keyboardVisible = isKeyboardVisible()

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
                    // Cancel backend session so normal login is never blocked
                    if (phoneNumber.isNotBlank()) {
                        authViewModel.cancelForgotPasswordSession(phoneNumber)
                    }
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
            // Keeps the logo clear of the notch / Dynamic Island — the iOS host view
            // ignores the safe area, so nothing else insets this screen.
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val screenHeight = maxHeight
            // Never smaller than the logo block, so a short window shrinks the empty space
            // instead of cropping the logo. Collapses while the keyboard is up so the step
            // below keeps room to scroll.
            val headerHeight by animateDpAsState(
                targetValue = if (keyboardVisible) 0.dp
                              else (screenHeight * 0.20f).coerceAtLeast(AuthHeaderMinHeight),
                label = "forgetPasswordHeaderHeight"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Eva2z Connect",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight)
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
                            onBack = {
                                // Cancel if OTP was already sent and user is abandoning
                                if (phoneNumber.isNotBlank()) {
                                    authViewModel.cancelForgotPasswordSession(phoneNumber)
                                }
                                onBack()
                            },
                            onOtpSent = { phone ->
                                // phone already includes country code (e.g. "+916789546789")
                                println("✅ OTP sent! Phone: $phone")
                                phoneNumber = phone

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