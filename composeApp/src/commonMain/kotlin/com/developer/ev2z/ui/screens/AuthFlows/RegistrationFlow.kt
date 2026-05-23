package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.ui.screens.Auth.OTPScreen
import com.developer.ev2z.ui.screens.Auth.PasswordScreen
import com.developer.ev2z.viewmodel.AuthViewModel
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import org.jetbrains.compose.resources.painterResource

enum class RegistrationStep {
    VERIFY_PHONE_OTP,
    VERIFY_EMAIL_OTP,
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
    var currentStep by remember { mutableStateOf(RegistrationStep.VERIFY_PHONE_OTP) }
    var showExitDialog by remember { mutableStateOf(false) }

    val isLoading by authViewModel.loading.collectAsState()
    val otpVerified by authViewModel.otpVerified.collectAsState()
    val emailOtpVerified by authViewModel.emailOtpVerified.collectAsState()
    val loginSuccess by authViewModel.loginSuccess.collectAsState()
    val error by authViewModel.error.collectAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Cross-platform back button — only on first screen
    if (currentStep == RegistrationStep.VERIFY_PHONE_OTP) {
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

//    LaunchedEffect(otpVerified) {
//        if (otpVerified) {
//            currentStep = RegistrationStep.CREATE_PASSWORD
//            authViewModel.resetOtpVerified()
//        }
//    }

    // Phone OTP verified → go to email OTP step
    LaunchedEffect(otpVerified) {
        if (otpVerified) {
            kotlinx.coroutines.delay(500)
            currentStep = RegistrationStep.VERIFY_EMAIL_OTP
            authViewModel.resetOtpVerified()
        }
    }

    // Email OTP verified → go to create password
    LaunchedEffect(emailOtpVerified) {
        if (emailOtpVerified) {
            kotlinx.coroutines.delay(500)
            currentStep = RegistrationStep.CREATE_PASSWORD
            authViewModel.resetEmailOtpVerified()
        }
    }

    // Registration complete → show success dialog
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            println("✅ Registration complete! Auto-login successful")
            authViewModel.resetLoginSuccess()
            showSuccessDialog = true
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF6A9F50),
                    modifier = Modifier.size(52.dp)
                )
            },
            title = {
                Text(
                    text = "Welcome to Eva2z!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Your account has been created successfully. You're all set to start tracking your vehicles.",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF555555),
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A9F50)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Get Started",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.20f),
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
                    .padding(top = screenHeight * 0.20f)
                    .background(
                        color = Color(0xFFFFFFFF),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                when (currentStep) {

                    // ── Step 1: Phone OTP ─────────────────────────────────
                    RegistrationStep.VERIFY_PHONE_OTP -> {
                        OTPScreen(
                            title = "Verify Phone Number",
                            subtitle = "Enter the 6-digit OTP sent to\n$phone",
                            isLoading = isLoading,
                            error = error,
                            onOTPComplete = { otp ->
                                authViewModel.verifyOtp(phone, otp)
                            },
                            onResendOTP = {
                                authViewModel.signUp(name, phone, "", email)
                            },
                            onBack = onBack
                        )
                    }

                    // ── Step 2: Email OTP ─────────────────────────────────
                    RegistrationStep.VERIFY_EMAIL_OTP -> {
                        OTPScreen(
                            title = "Verify Email Address",
                            subtitle = "Enter the 6-digit OTP sent to\n$email",
                            isLoading = isLoading,
                            error = error,
                            onOTPComplete = { otp ->
                                authViewModel.verifyEmailOtp(phone, email, otp)
                            },
                            onResendOTP = {
                                authViewModel.signUp(name, phone, "", email)
                            },
                            onBack = {
                                currentStep = RegistrationStep.VERIFY_PHONE_OTP
                                authViewModel.clearError()
                            }
                        )
                    }

                    // ── Step 3: Create Password ───────────────────────────
                    RegistrationStep.CREATE_PASSWORD -> {
                        PasswordScreen(
                            title = "Create Password",
                            subtitle = "Secure your account with a strong password",
                            buttonText = "Create Account",
                            isLoading = isLoading,
                            error = error,
                            onPasswordSubmit = { password, _ ->
                                println("🔐 Calling savePasswordAndLogin...")
                                authViewModel.savePasswordAndLogin(name, phone, password, email)
                            },
                            onBack = { currentStep = RegistrationStep.VERIFY_EMAIL_OTP }
                        )
                    }
                }
            }
        }
    }
}