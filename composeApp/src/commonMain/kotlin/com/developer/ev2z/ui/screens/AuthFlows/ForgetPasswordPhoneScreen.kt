package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.ui.components.CountryPickerDialog
import com.developer.ev2z.ui.components.PhoneInputField
import com.developer.ev2z.ui.components.defaultCountry
import com.developer.ev2z.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun ForgetPasswordPhoneScreen(
    viewModel: AuthViewModel,
    preFilledPhone: String = "",
    onBack: () -> Unit,
    onOtpSent: (String) -> Unit,
    onNotRegistered: (String) -> Unit = {}
) {
    var phoneNumber     by remember(preFilledPhone) { mutableStateOf(preFilledPhone) }
    var selectedCountry by remember { mutableStateOf(defaultCountry()) }
    var showPicker      by remember { mutableStateOf(false) }

    var hasHandledResult by remember { mutableStateOf(false) }
    var manuallyEdited   by remember { mutableStateOf(false) }

    val isLoading by viewModel.loading.collectAsState()
    val error     by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    var phoneError      by remember { mutableStateOf<String?>(null) }
    var previousLoading by remember { mutableStateOf(false) }

    val phoneValid = phoneNumber.length >= 5

    // Auto-submit if pre-filled with a valid number (Indian default country)
    LaunchedEffect(preFilledPhone) {
        if (preFilledPhone.length >= 5 && !manuallyEdited) {
            hasHandledResult = false
            delay(500)
            val fullPhone = "${selectedCountry.dialCode}$preFilledPhone"
            viewModel.forgotPassword(fullPhone)
        }
    }

    // Handle success / error when loading finishes
    LaunchedEffect(isLoading) {
        if (previousLoading && !isLoading && !hasHandledResult) {
            hasHandledResult = true
            val currentError = error
            when {
                currentError != null &&
                currentError.contains("not registered", ignoreCase = true) -> {
                    phoneError = "This phone number is not registered"
                    viewModel.clearError()
                }
                currentError != null -> {
                    phoneError = currentError
                    viewModel.clearError()
                }
                currentError == null && phoneValid -> {
                    val fullPhone = "${selectedCountry.dialCode}$phoneNumber"
                    onOtpSent(fullPhone)
                }
            }
        }
        previousLoading = isLoading
    }

    if (showPicker) {
        CountryPickerDialog(
            onDismiss = { showPicker = false },
            onCountrySelected = { country ->
                selectedCountry = country
                showPicker = false
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth  = maxWidth
        val isCompact    = screenHeight < 600.dp

        val headerFontSize = if (isCompact) 18.sp else 20.sp
        val titleSize      = if (isCompact) 24.sp else 28.sp
        val subtitleSize   = if (isCompact) 13.sp else 14.sp
        val labelSize      = if (isCompact) 12.sp else 14.sp
        val fieldHeight    = if (isCompact) 50.dp  else 56.dp
        val buttonHeight   = if (isCompact) 46.dp  else 52.dp
        val buttonTextSize = if (isCompact) 15.sp  else 16.sp
        val horizontalPad  = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPad)
        ) {
            Spacer(Modifier.height(if (isCompact) 12.dp else 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF333333))
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text       = "Forgot Password",
                    fontSize   = headerFontSize,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF222222)
                )
            }

            Spacer(Modifier.height(if (isCompact) 24.dp else 36.dp))

            Text(
                text       = "Reset Your Password",
                fontSize   = titleSize,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF222222),
                modifier   = Modifier.fillMaxWidth(),
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = "Enter your registered phone number and we'll send you an OTP to reset your password",
                fontSize  = subtitleSize,
                color     = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = (subtitleSize.value * 1.5f).sp,
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(if (isCompact) 28.dp else 40.dp))

            Text(
                text       = "Phone Number*",
                fontSize   = labelSize,
                fontWeight = FontWeight.Medium,
                color      = Color(0xFF555555),
                modifier   = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
            PhoneInputField(
                phoneNumber     = phoneNumber,
                onPhoneChange   = { input ->
                    phoneNumber    = input
                    manuallyEdited = true
                    phoneError     = null
                },
                selectedCountry = selectedCountry,
                onCountryClick  = { showPicker = true },
                fieldHeight     = fieldHeight,
                isError         = (phoneNumber.isNotEmpty() && !phoneValid) || phoneError != null,
                labelSize       = labelSize,
                placeholder     = if (isCompact) "Phone number" else "Enter Your Registered Number"
            )
            if (phoneNumber.isNotEmpty() && !phoneValid) {
                Text(
                    text  = "Enter a valid phone number",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
            phoneError?.let {
                Text(
                    text  = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
            }

            Spacer(Modifier.height(if (isCompact) 24.dp else 32.dp))

            Button(
                onClick = {
                    hasHandledResult = false
                    phoneError = null
                    val fullPhone = "${selectedCountry.dialCode}$phoneNumber"
                    viewModel.forgotPassword(fullPhone)
                },
                modifier = Modifier.fillMaxWidth().height(buttonHeight),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFF6A9F50),
                    contentColor           = Color.White,
                    disabledContainerColor = Color(0xFFB5D4A5),
                    disabledContentColor   = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
                enabled = !isLoading && phoneValid
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text       = "Send OTP",
                        fontSize   = buttonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}