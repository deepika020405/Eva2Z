package com.developer.ev2z.ui.screens.Auth

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
import com.developer.ev2z.util.OTPAutoReader

@Composable
fun OTPScreen(
    title: String = "Enter OTP",
    subtitle: String = "Enter the OTP code we just sent\nyou on your registered phone number",
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

    val focusRequester1 = remember { FocusRequester() }
    val focusRequester2 = remember { FocusRequester() }
    val focusRequester3 = remember { FocusRequester() }
    val focusRequester4 = remember { FocusRequester() }
    val focusRequester5 = remember { FocusRequester() }
    val focusRequester6 = remember { FocusRequester() }

    // ✅ Cross-platform OTP Auto-read
    OTPAutoReader { receivedOTP ->
        println("📱 Auto-filling OTP: $receivedOTP")
        if (receivedOTP.length == 6) {
            otp1 = receivedOTP[0].toString()
            otp2 = receivedOTP[1].toString()
            otp3 = receivedOTP[2].toString()
            otp4 = receivedOTP[3].toString()
            otp5 = receivedOTP[4].toString()
            otp6 = receivedOTP[5].toString()
        }
    }

    val otpList = listOf(otp1, otp2, otp3, otp4, otp5, otp6)
    val isOTPComplete = otpList.all { it.isNotEmpty() }

    LaunchedEffect(isOTPComplete) {
        if (isOTPComplete && !isLoading) {
            val fullOTP = "$otp1$otp2$otp3$otp4$otp5$otp6"
            println("🔐 Auto-submitting OTP: $fullOTP")
            kotlinx.coroutines.delay(300)
            onOTPComplete(fullOTP)
        }
    }

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

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OTPBox(value = otp1, onValueChange = { if (it.isEmpty() && otp1.isNotEmpty()) otp1 = "" else if (it.isNotEmpty()) { otp1 = it; focusRequester2.requestFocus() } }, focusRequester = focusRequester1)
            OTPBox(value = otp2, onValueChange = { if (it.isEmpty() && otp2.isNotEmpty()) { otp2 = ""; focusRequester1.requestFocus() } else if (it.isNotEmpty()) { otp2 = it; focusRequester3.requestFocus() } }, focusRequester = focusRequester2)
            OTPBox(value = otp3, onValueChange = { if (it.isEmpty() && otp3.isNotEmpty()) { otp3 = ""; focusRequester2.requestFocus() } else if (it.isNotEmpty()) { otp3 = it; focusRequester4.requestFocus() } }, focusRequester = focusRequester3)
            OTPBox(value = otp4, onValueChange = { if (it.isEmpty() && otp4.isNotEmpty()) { otp4 = ""; focusRequester3.requestFocus() } else if (it.isNotEmpty()) { otp4 = it; focusRequester5.requestFocus() } }, focusRequester = focusRequester4)
            OTPBox(value = otp5, onValueChange = { if (it.isEmpty() && otp5.isNotEmpty()) { otp5 = ""; focusRequester4.requestFocus() } else if (it.isNotEmpty()) { otp5 = it; focusRequester6.requestFocus() } }, focusRequester = focusRequester5)
            OTPBox(value = otp6, onValueChange = { if (it.isEmpty() && otp6.isNotEmpty()) { otp6 = ""; focusRequester5.requestFocus() } else otp6 = it }, focusRequester = focusRequester6)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isOTPComplete) {
                    onOTPComplete("$otp1$otp2$otp3$otp4$otp5$otp6")
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6A9F50),
                contentColor = Color.White
            ),
            enabled = !isLoading && isOTPComplete
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Didn't get OTP? ", fontSize = 14.sp, color = Color(0xFF666666))
            TextButton(onClick = onResendOTP) {
                Text("Resend OTP", fontSize = 14.sp, color = Color(0xFF6A9F50), fontWeight = FontWeight.SemiBold)
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester1.requestFocus()
    }
}

@Composable
fun OTPBox(value: String, onValueChange: (String) -> Unit, focusRequester: FocusRequester) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= 1 && (it.isEmpty() || it.all { c -> c.isDigit() })) onValueChange(it) },
        modifier = Modifier.width(48.dp).height(58.dp).focusRequester(focusRequester)
            .border(1.5.dp, if (value.isNotEmpty()) Color(0xFF6A9F50) else Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp)),
        textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        decorationBox = { innerTextField -> Box(Alignment.Center as Modifier,
            Modifier.fillMaxSize() as Alignment
        ) { innerTextField() } }
    )
}