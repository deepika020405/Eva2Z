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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

    var showBackDialog by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var lastSubmittedOtp by remember { mutableStateOf<String?>(null) }

    BackPressHandler { showBackDialog = true }

    // ✅ Auto read OTP
    OTPAutoReader { received ->
        if (received.length == 6) {
            otp1 = received[0].toString()
            otp2 = received[1].toString()
            otp3 = received[2].toString()
            otp4 = received[3].toString()
            otp5 = received[4].toString()
            otp6 = received[5].toString()
        }
    }

    val isOtpComplete = listOf(otp1, otp2, otp3, otp4, otp5, otp6).all { it.isNotEmpty() }

    // ✅ AUTO SUBMIT (SAFE)
    LaunchedEffect(isOtpComplete, isLoading) {
        if (!isOtpComplete || isLoading) return@LaunchedEffect

        val currentOtp = "$otp1$otp2$otp3$otp4$otp5$otp6"

        if (currentOtp == lastSubmittedOtp) return@LaunchedEffect

        lastSubmittedOtp = currentOtp
        hasSubmitted = true

        delay(300)
        onOTPComplete(currentOtp)
    }

    // ✅ Allow retry only if user changes OTP
    LaunchedEffect(otp1, otp2, otp3, otp4, otp5, otp6) {
        hasSubmitted = false
    }

    // ✅ Reset submit flag on error (but keep OTP)
    LaunchedEffect(error) {
        if (error != null) {
            hasSubmitted = false
        }
    }

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = { Text("Confirm") },
            text = { Text("Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = { showBackDialog = false; onBack() }) {
                    Text("Yes", color = Color(0xFF6A9F50))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showBackDialog = true }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Enter OTP",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(Modifier.height(40.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            OTPBox(otp1, focus1, null, error) {
                otp1 = it
                if (it.isNotEmpty()) focus2.requestFocus()
            }

            OTPBox(otp2, focus2, focus1, error) {
                otp2 = it
                if (it.isNotEmpty()) focus3.requestFocus()
            }

            OTPBox(otp3, focus3, focus2, error) {
                otp3 = it
                if (it.isNotEmpty()) focus4.requestFocus()
            }

            OTPBox(otp4, focus4, focus3, error) {
                otp4 = it
                if (it.isNotEmpty()) focus5.requestFocus()
            }

            OTPBox(otp5, focus5, focus4, error) {
                otp5 = it
                if (it.isNotEmpty()) focus6.requestFocus()
            }

            OTPBox(otp6, focus6, focus5, error) {
                otp6 = it
            }
        }

        // ✅ ERROR BELOW OTP BOXES
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                val otp = "$otp1$otp2$otp3$otp4$otp5$otp6"
                if (isOtpComplete && otp != lastSubmittedOtp) {
                    lastSubmittedOtp = otp
                    hasSubmitted = true
                    onOTPComplete(otp)
                }
            },
            enabled = isOtpComplete && !isLoading,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(30.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text("Continue")
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Didn't get OTP?",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )

            Spacer(modifier = Modifier.width(4.dp))

            TextButton(
                onClick = onResendOTP,
                contentPadding = PaddingValues(0.dp) // 🔥 no extra padding
            ) {
                Text(
                    text = "Resend OTP",
                    fontSize = 14.sp,
                    color = Color(0xFF6A9F50),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    LaunchedEffect(Unit) { focus1.requestFocus() }
}

@Composable
private fun OTPBox(
    value: String,
    focusRequester: FocusRequester,
    previousFocusRequester: FocusRequester?,
    error: String?,
    onChange: (String) -> Unit
){
    BasicTextField(
        value = value,
        onValueChange = {
            when {
                it.length == 1 && it.all { c -> c.isDigit() } -> {
                    onChange(it)
                }

                it.isEmpty() -> {
                    onChange("")
                    previousFocusRequester?.requestFocus()
                }
            }
        },
        modifier = Modifier
            .width(52.dp)
            .height(60.dp)
            .focusRequester(focusRequester)
            .border(
                1.5.dp,
                if (error != null) Color.Red else Color(0xFFE0E0E0),
                RoundedCornerShape(12.dp)
            )
            .background(Color.White, RoundedCornerShape(12.dp)),
        textStyle = TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        decorationBox = { Box(Modifier.fillMaxSize(), Alignment.Center) { it() } }
    )
}