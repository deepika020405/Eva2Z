package com.developer.ev2z.ui.screens.Auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.util.OTPAutoReader
import kotlinx.coroutines.delay

@Composable
fun OTPScreen(
    title: String = "Verify OTP",
    subtitle: String = "Enter the OTP sent to your mobile number",
    isLoading: Boolean = false,
    error: String? = null,
    onOTPComplete: (String) -> Unit,
    onResendOTP: () -> Unit,
    onBack: () -> Unit
) {

    var otp1 by remember { mutableStateOf("") }
    var otp2 by remember { mutableStateOf("") }
    var otp3 by remember { mutableStateOf("") }
    var otp4 by remember { mutableStateOf("") }
    var otp5 by remember { mutableStateOf("") }
    var otp6 by remember { mutableStateOf("") }

    val focus1 = remember { FocusRequester() }
    val focus2 = remember { FocusRequester() }
    val focus3 = remember { FocusRequester() }
    val focus4 = remember { FocusRequester() }
    val focus5 = remember { FocusRequester() }
    val focus6 = remember { FocusRequester() }

    var resendTimeLeft by remember { mutableStateOf(60) }
    var resendEnabled by remember { mutableStateOf(false) }

    var showBackDialog by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var lastSubmittedOtp by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    BackPressHandler { showBackDialog = true }

    OTPAutoReader { received ->
        if (received.length == 6) {
            otp1 = received[0].toString()
            otp2 = received[1].toString()
            otp3 = received[2].toString()
            otp4 = received[3].toString()
            otp5 = received[4].toString()
            otp6 = received[5].toString()
            focusManager.clearFocus()
        }
    }

    val isOtpComplete = listOf(otp1, otp2, otp3, otp4, otp5, otp6).all { it.isNotEmpty() }

    LaunchedEffect(isOtpComplete, isLoading) {
        if (!isOtpComplete || isLoading) return@LaunchedEffect
        val currentOtp = "$otp1$otp2$otp3$otp4$otp5$otp6"
        if (currentOtp == lastSubmittedOtp) return@LaunchedEffect
        lastSubmittedOtp = currentOtp
        delay(300)
        onOTPComplete(currentOtp)
    }

    LaunchedEffect(otp1, otp2, otp3, otp4, otp5, otp6) {
        hasSubmitted = false
    }

    LaunchedEffect(error) {
        if (error != null) {
            otp1 = ""
            otp2 = ""
            otp3 = ""
            otp4 = ""
            otp5 = ""
            otp6 = ""
            lastSubmittedOtp = null
            hasSubmitted = false
            delay(100)
            focus1.requestFocus()
        }
    }

    LaunchedEffect(resendTimeLeft) {
        if (resendTimeLeft > 0) {
            delay(1000)
            resendTimeLeft--
        } else {
            resendEnabled = true
        }
    }

    fun onOtpChanged() {
        if (error != null) {
            lastSubmittedOtp = null
        }
    }

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = {
                Text(
                    "Confirm",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text("Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = { showBackDialog = false; onBack() }) {
                    Text("Yes", color = Color(0xFF6A9F50), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) {
                    Text("No", color = Color(0xFF888888))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val isCompact = screenHeight < 600.dp

        // Responsive sizes
        val titleSize = if (isCompact) 22.sp else 26.sp
        val subtitleSize = if (isCompact) 13.sp else 14.sp
        val buttonHeight = if (isCompact) 46.dp else 52.dp
        val horizontalPad = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)

        // OTP box sizing — responsive to screen width
        val otpBoxSize = ((screenWidth - horizontalPad * 2 - 50.dp) / 6).coerceIn(40.dp, 56.dp)
        val otpBoxHeight = (otpBoxSize * 1.15f).coerceIn(48.dp, 64.dp)
        val otpFontSize = if (isCompact) 20.sp else 24.sp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPad, vertical = if (isCompact) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back + Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showBackDialog = true }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Back",
                        tint = Color(0xFF333333)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Enter OTP",
                    fontSize = if (isCompact) 18.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            }

            Spacer(Modifier.height(if (isCompact) 16.dp else 24.dp))

            // Title
            Text(
                text = title,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = subtitle,
                fontSize = subtitleSize,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(if (isCompact) 28.dp else 40.dp))

            // OTP Boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                OTPBox(otp1, focus1, null, error, otpBoxSize, otpBoxHeight, otpFontSize) {
                    otp1 = it; onOtpChanged()
                    if (it.isNotEmpty()) focus2.requestFocus()
                }
                OTPBox(otp2, focus2, focus1, error, otpBoxSize, otpBoxHeight, otpFontSize) {
                    otp2 = it; onOtpChanged()
                    if (it.isNotEmpty()) focus3.requestFocus()
                }
                OTPBox(otp3, focus3, focus2, error, otpBoxSize, otpBoxHeight, otpFontSize) {
                    otp3 = it; onOtpChanged()
                    if (it.isNotEmpty()) focus4.requestFocus()
                }
                OTPBox(otp4, focus4, focus3, error, otpBoxSize, otpBoxHeight, otpFontSize) {
                    otp4 = it; onOtpChanged()
                    if (it.isNotEmpty()) focus5.requestFocus()
                }
                OTPBox(otp5, focus5, focus4, error, otpBoxSize, otpBoxHeight, otpFontSize) {
                    otp5 = it; onOtpChanged()
                    if (it.isNotEmpty()) focus6.requestFocus()
                }
                OTPBox(otp6, focus6, focus5, error, otpBoxSize, otpBoxHeight, otpFontSize) {
                    otp6 = it; onOtpChanged()
                }
                Spacer(Modifier.weight(1f))
            }

            // Error
            error?.let {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(if (isCompact) 20.dp else 28.dp))

            // Continue Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val otp = "$otp1$otp2$otp3$otp4$otp5$otp6"
                    if (isOtpComplete && otp != lastSubmittedOtp) {
                        lastSubmittedOtp = otp
                        hasSubmitted = true
                        onOTPComplete(otp)
                    }
                },
                enabled = isOtpComplete && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A9F50),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFB5D4A5),
                    disabledContentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        "Continue",
                        fontSize = if (isCompact) 15.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Resend OTP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!resendEnabled) {
                    val minutes = resendTimeLeft / 60
                    val seconds = resendTimeLeft % 60

                    Text(
                        text = "Resend OTP in $minutes:${seconds.toString().padStart(2, '0')}",
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        color = Color(0xFFE57373),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Didn't get OTP?",
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        color = Color(0xFF888888)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = {
                            resendTimeLeft = 60
                            resendEnabled = false
                            onResendOTP()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Resend OTP",
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            color = Color(0xFF6A9F50),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        LaunchedEffect(Unit) { focus1.requestFocus() }
    }
}

@Composable
private fun OTPBox(
    value: String,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester?,
    error: String?,
    boxWidth: Dp,
    boxHeight: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onChange: (String) -> Unit
) {
    val borderColor = when {
        error != null -> Color(0xFFE57373)
        value.isNotEmpty() -> Color(0xFF6A9F50)
        else -> Color(0xFFE0E0E0)
    }

    val bgColor = if (value.isNotEmpty()) Color(0xFFF8FAF7) else Color(0xFFFAFAFA)

    BasicTextField(
        value = value,
        onValueChange = {
            when {
                it.length == 1 && it.all { c -> c.isDigit() } -> onChange(it)
                it.isEmpty() -> {
                    onChange("")
                    previousFocusRequester?.requestFocus()
                }
            }
        },
        modifier = Modifier
            .width(boxWidth)
            .height(boxHeight)
            .focusRequester(focusRequester)
            .border(
                width = if (value.isNotEmpty() || error != null) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(bgColor, RoundedCornerShape(14.dp)),
        textStyle = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF222222)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}