package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgetPasswordPhoneScreen(
    viewModel: AuthViewModel,
    preFilledPhone: String = "",
    onBack: () -> Unit,
    onOtpSent: (String) -> Unit,
    onNotRegistered: (String) -> Unit = {}
) {
    var phoneNumber by remember(preFilledPhone) { mutableStateOf(preFilledPhone) }

    var hasHandledResult by remember { mutableStateOf(false) }
    var wasAutoFilled by remember(preFilledPhone) {
        mutableStateOf(preFilledPhone.isNotEmpty())
    }
    var manuallyEdited by remember { mutableStateOf(false) }

    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var phoneError by remember { mutableStateOf<String?>(null) }
    var previousLoading by remember { mutableStateOf(false) }

    // Auto-submit if pre-filled and 10 digits
    LaunchedEffect(preFilledPhone) {
        if (preFilledPhone.length == 10 && wasAutoFilled && !manuallyEdited) {
            hasHandledResult = false
            delay(500)
            val fullPhone = "+91$preFilledPhone"
            viewModel.forgotPassword(fullPhone)
        }
    }

    // Handle BOTH success and error when loading finishes
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

                currentError == null && phoneNumber.length == 10 -> {
                    onOtpSent(phoneNumber)
                }
            }
        }
        previousLoading = isLoading
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val isCompact = screenHeight < 600.dp

        // Responsive sizes
        val headerFontSize = if (isCompact) 18.sp else 20.sp
        val titleSize = if (isCompact) 24.sp else 28.sp
        val subtitleSize = if (isCompact) 13.sp else 14.sp
        val labelSize = if (isCompact) 12.sp else 14.sp
        val fieldHeight = if (isCompact) 50.dp else 56.dp
        val buttonHeight = if (isCompact) 46.dp else 52.dp
        val buttonTextSize = if (isCompact) 15.sp else 16.sp
        val horizontalPad = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPad)
        ) {
            // Header
            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Back",
                        tint = Color(0xFF333333)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Forgot Password",
                    fontSize = headerFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 36.dp))

            // Title
            Text(
                text = "Reset Your Password",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Enter your registered phone number and we'll send you an OTP to reset your password",
                fontSize = subtitleSize,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                lineHeight = (subtitleSize.value * 1.5f).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(if (isCompact) 28.dp else 40.dp))

            // Phone Number Label
            Text(
                text = "Phone Number*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                        phoneNumber = it
                        manuallyEdited = true
                        phoneError = null
                    }
                },
                prefix = {
                    Text(
                        text = "🇮🇳 +91 ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333)
                    )
                },
                placeholder = {
                    Text(
                        "Enter Your Registered Number",
                        color = Color(0xFFAAAAAA),
                        fontSize = labelSize
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAF7),
                    unfocusedContainerColor = Color(0xFFF8FAF7),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFF6A9F50),
                    cursorColor = Color(0xFF6A9F50)
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    autoCorrect = false
                ),
                singleLine = true,
                isError = (phoneNumber.isNotEmpty() && phoneNumber.length < 10) || phoneError != null
            )

            if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                Text(
                    text = "Phone number must be 10 digits",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            if (phoneError != null) {
                Text(
                    text = phoneError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
            }

            Spacer(Modifier.height(if (isCompact) 24.dp else 32.dp))

            // Send OTP Button
            Button(
                onClick = {
                    hasHandledResult = false
                    val fullPhone = "+91$phoneNumber"
                    viewModel.forgotPassword(fullPhone)
                },
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
                ),
                enabled = !isLoading && phoneNumber.length == 10
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send OTP",
                        fontSize = buttonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}