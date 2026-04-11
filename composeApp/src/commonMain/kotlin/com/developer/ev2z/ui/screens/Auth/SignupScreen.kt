package com.developer.ev2z.ui.screens.Auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.utils.EmailValidator
import com.developer.ev2z.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onSignupSuccess: () -> Unit = {},
    onSendOTPClick: (String, String, String) -> Unit = { _, _, _ -> },
    onAlreadyRegistered: (String) -> Unit = {},
    preFilledPhone: String = ""
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember(preFilledPhone) { mutableStateOf(preFilledPhone) }
    var email by remember { mutableStateOf("") }
    var phoneAlreadyRegisteredError by remember { mutableStateOf<String?>(null) }
    var submitAttempted by remember { mutableStateOf(false) }

    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var navigateToLogin by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "sign_up_screen",
                "screen_class" to "SignupScreen"
            )
        )
    }

    LaunchedEffect(preFilledPhone) {
        if (preFilledPhone.isNotEmpty()) {
            phoneNumber = preFilledPhone
        }
    }

    LaunchedEffect(navigateToLogin) {
        if (navigateToLogin) {
            delay(3000)
            onAlreadyRegistered(phoneNumber)
            navigateToLogin = false
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth
        val isCompact = screenHeight < 600.dp
        val density = LocalDensity.current

        // Responsive sizes
        val labelSize = if (isCompact) 12.sp else 14.sp
        val fieldHeight = if (isCompact) 50.dp else 56.dp
        val buttonHeight = if (isCompact) 46.dp else 52.dp
        val buttonTextSize = if (isCompact) 15.sp else 16.sp
        val verticalGap = if (isCompact) 12.dp else 18.dp
        val horizontalPad = (screenWidth * 0.04f).coerceIn(12.dp, 24.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPad, vertical = if (isCompact) 16.dp else 24.dp)
        ) {
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))

            // Full Name Label
            Text(
                text = "Full Name*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { input ->
                    val formatted = formatFullName(input)
                    if (formatted.length <= 50) {
                        fullName = formatted
                    }
                },
                placeholder = {
                    Text(
                        "Enter Your Full Name",
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                isError = submitAttempted && fullName.isBlank()
            )

            if (submitAttempted && fullName.isBlank()) {
                Text(
                    text = "Please enter a valid full name (letters only)",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(verticalGap))

            // Phone Number Label
            Text(
                text = "Phone Number*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    if (digitsOnly.length > 10) {
                        if (input.contains("+") || input.contains(" ")) {
                            phoneNumber = digitsOnly.takeLast(10)
                        }
                        return@OutlinedTextField
                    }
                    phoneNumber = digitsOnly
                    phoneAlreadyRegisteredError = null
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
                        "Enter Your Valid Number",
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
                isError = submitAttempted && (
                        phoneNumber.length < 10 || phoneAlreadyRegisteredError != null
                        )
            )

            if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                Text(
                    text = "Phone number must be 10 digits",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            if (submitAttempted) {
                when {
                    phoneAlreadyRegisteredError != null -> {
                        Text(
                            text = phoneAlreadyRegisteredError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(verticalGap))

            // Email Label
            Text(
                text = "Email*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text(
                        "Enter Your Valid E-mail",
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = email.isNotEmpty() && !EmailValidator.isValidEmail(email)
            )

            if (email.isNotEmpty() && !EmailValidator.isValidEmail(email)) {
                Text(
                    text = "Please enter a valid email",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 32.dp))

            // Send OTP Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    submitAttempted = true
                    phoneAlreadyRegisteredError = null

                    if (phoneNumber.length < 10) return@Button

                    val fullPhone = "+91$phoneNumber"

                    Analytics.logEvent(
                        name = "signup_details_entered",
                        params = mapOf(
                            "name_length" to fullName.length,
                            "phone_masked" to maskPhone(phoneNumber),
                            "email_domain" to email.substringAfter("@", ""),
                            "country_code" to "IN"
                        )
                    )

                    Analytics.logEvent(
                        name = "generate_lead",
                        params = mapOf(
                            "source" to "signup_screen",
                            "phone_masked" to maskPhone(phoneNumber),
                            "email_domain" to email.substringAfter("@", ""),
                            "country_code" to "IN"
                        )
                    )

                    viewModel.checkPhoneRegistered(fullPhone) { isRegistered ->
                        if (isRegistered) {
                            phoneAlreadyRegisteredError = "This phone number is already registered!"
                            navigateToLogin = true
                        } else {
                            onSendOTPClick(fullName, phoneNumber, email)
                        }
                    }
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
                enabled = !isLoading &&
                        fullName.isNotEmpty() &&
                        phoneNumber.length == 10 &&
                        email.isNotEmpty() &&
                        EmailValidator.isValidEmail(email)
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
                        color = Color.White,
                        fontSize = buttonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))

            // Terms Text
            val termsSize = if (isCompact) 11.sp else 12.sp
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF888888), fontSize = termsSize)) {
                    append("By signing up, you agree to the ")
                }
                withStyle(style = SpanStyle(color = Color(0xFF6A9F50), fontSize = termsSize, fontWeight = FontWeight.SemiBold)) {
                    append("Terms of Service")
                }
                withStyle(style = SpanStyle(color = Color(0xFF888888), fontSize = termsSize)) {
                    append(" and ")
                }
                withStyle(style = SpanStyle(color = Color(0xFF6A9F50), fontSize = termsSize, fontWeight = FontWeight.SemiBold)) {
                    append("Data Processing Agreement")
                }
            }

            Text(
                text = annotatedText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatFullName(input: String): String {
    return input
        .filter { it.isLetter() || it == ' ' }
        .replace("\\s+".toRegex(), " ")
        .trimStart()
        .split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }
}