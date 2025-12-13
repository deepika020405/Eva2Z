package com.developer.ev2z.ui.screens.OTP

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OTPScreen(
    title: String = "Enter OTP",
    subtitle: String = "Enter the OTP code we just sent\nyou on your registered Email/Phone number",
    isLoading: Boolean = false,
    error: String? = null,
    onOTPComplete: (String) -> Unit = {},
    onResendOTP: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var otp1 by remember { mutableStateOf("") }
    var otp2 by remember { mutableStateOf("") }
    var otp3 by remember { mutableStateOf("") }
    var otp4 by remember { mutableStateOf("") }
    var otp5 by remember { mutableStateOf("") }
    var otp6 by remember { mutableStateOf("") }

    // ✅ Focus requesters for auto-focus
    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }
    val focusRequester5 = remember { FocusRequester() }
    val focusRequester6 = remember { FocusRequester() }

    val otpList = listOf(otp1, otp2, otp3, otp4, otp5, otp6)
    val isOTPComplete = otpList.all { it.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF252A23),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ✅ 6 OTP boxes with proper spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OTPBox(
                value = otp1,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() && otp1.isNotEmpty()) {
                        otp1 = ""
                    } else if (newValue.isNotEmpty()) {
                        otp1 = newValue
                        focusRequester2.requestFocus()
                    }
                },
                focusRequester = focusRequester1
            )
            OTPBox(
                value = otp2,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() && otp2.isNotEmpty()) {
                        otp2 = ""
                        focusRequester1.requestFocus()
                    } else if (newValue.isNotEmpty()) {
                        otp2 = newValue
                        focusRequester3.requestFocus()
                    }
                },
                focusRequester = focusRequester2
            )
            OTPBox(
                value = otp3,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() && otp3.isNotEmpty()) {
                        otp3 = ""
                        focusRequester2.requestFocus()
                    } else if (newValue.isNotEmpty()) {
                        otp3 = newValue
                        focusRequester4.requestFocus()
                    }
                },
                focusRequester = focusRequester3
            )
            OTPBox(
                value = otp4,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() && otp4.isNotEmpty()) {
                        otp4 = ""
                        focusRequester3.requestFocus()
                    } else if (newValue.isNotEmpty()) {
                        otp4 = newValue
                        focusRequester5.requestFocus()
                    }
                },
                focusRequester = focusRequester4
            )
            OTPBox(
                value = otp5,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() && otp5.isNotEmpty()) {
                        otp5 = ""
                        focusRequester4.requestFocus()
                    } else if (newValue.isNotEmpty()) {
                        otp5 = newValue
                        focusRequester6.requestFocus()
                    }
                },
                focusRequester = focusRequester5
            )
            OTPBox(
                value = otp6,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() && otp6.isNotEmpty()) {
                        otp6 = ""
                        focusRequester5.requestFocus()
                    } else {
                        otp6 = newValue
                    }
                },
                focusRequester = focusRequester6
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isOTPComplete) {
                    val fullOTP = "$otp1$otp2$otp3$otp4$otp5$otp6"
                    onOTPComplete(fullOTP)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6A9F50),
                contentColor = Color.White
            ),
            enabled = !isLoading && isOTPComplete
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Continue",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Didn't get OTP? ",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            TextButton(onClick = onResendOTP) {
                Text(
                    text = "Resend OTP",
                    fontSize = 14.sp,
                    color = Color(0xFF6A9F50),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        error?.let { errorMessage ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }

    // ✅ Auto-focus first box
    LaunchedEffect(Unit) {
        focusRequester1.requestFocus()
    }
}

@Composable
fun OTPBox(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    BasicTextField(
        value = value,
        onValueChange = {
            if (it.length <= 1 && (it.isEmpty() || it.all { char -> char.isDigit() })) {
                onValueChange(it)
            }
        },
        modifier = Modifier
            .width(48.dp)
            .height(58.dp)
            .focusRequester(focusRequester)
            .border(
                width = 1.5.dp,
                color = if (value.isNotEmpty()) Color(0xFF6A9F50) else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            )
            .background(Color.White, RoundedCornerShape(12.dp)),
        textStyle = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                innerTextField()
            }
        }
    )
}