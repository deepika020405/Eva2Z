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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.ui.components.CountryData
import com.developer.ev2z.ui.components.CountryPickerDialog
import com.developer.ev2z.ui.components.PhoneInputField
import com.developer.ev2z.ui.components.defaultCountry
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.utils.EmailValidator
import com.developer.ev2z.utils.PhoneValidator
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
    var fullName       by remember { mutableStateOf("") }
    var phoneNumber    by remember(preFilledPhone) { mutableStateOf(preFilledPhone) }
    var email          by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(defaultCountry()) }
    var showPicker     by remember { mutableStateOf(false) }
    var phoneAlreadyRegisteredError by remember { mutableStateOf<String?>(null) }
    var submitAttempted by remember { mutableStateOf(false) }

    val isLoading  by viewModel.loading.collectAsState()
    val error      by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val scope       = rememberCoroutineScope()
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
        if (preFilledPhone.isNotEmpty()) phoneNumber = preFilledPhone
    }

    LaunchedEffect(navigateToLogin) {
        if (navigateToLogin) {
            delay(3000)
            onAlreadyRegistered(phoneNumber)
            navigateToLogin = false
        }
    }

    LaunchedEffect(error) {
        if (error?.contains("already registered", ignoreCase = true) == true) {
            phoneAlreadyRegisteredError = "This phone number is already registered!"
            navigateToLogin = true
            viewModel.clearError()
        }
    }

    if (showPicker) {
        CountryPickerDialog(
            onDismiss = { showPicker = false },
            onCountrySelected = { country ->
                selectedCountry = country
                if (phoneNumber.length > country.phoneLength) phoneNumber = phoneNumber.take(country.phoneLength)
                showPicker = false
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = maxHeight
        val screenWidth  = maxWidth
        val isCompact    = screenHeight < 600.dp

        val labelSize        = if (isCompact) 12.sp else 14.sp
        val fieldHeight      = if (isCompact) 50.dp  else 56.dp
        val emailFieldHeight = if (isCompact) 56.dp  else 64.dp
        val buttonHeight     = if (isCompact) 46.dp  else 52.dp
        val buttonTextSize = if (isCompact) 15.sp  else 16.sp
        val verticalGap    = if (isCompact) 12.dp  else 18.dp
        val horizontalPad  = (screenWidth * 0.04f).coerceIn(12.dp, 24.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPad, vertical = if (isCompact) 16.dp else 24.dp)
        ) {
            Spacer(Modifier.height(if (isCompact) 8.dp else 16.dp))

            // Full Name
            Text(
                text = "Full Name*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = fullName,
                onValueChange = { input ->
                    val formatted = formatFullName(input)
                    if (formatted.length <= 50) fullName = formatted
                },
                placeholder = {
                    Text("Enter Your Full Name", color = Color(0xFFAAAAAA), fontSize = labelSize)
                },
                modifier = Modifier.fillMaxWidth().height(fieldHeight),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Color(0xFFF8FAF7),
                    unfocusedContainerColor = Color(0xFFF8FAF7),
                    unfocusedBorderColor    = Color(0xFFE0E0E0),
                    focusedBorderColor      = Color(0xFF6A9F50),
                    cursorColor             = Color(0xFF6A9F50)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                isError = submitAttempted && fullName.isBlank()
            )
            if (submitAttempted && fullName.isBlank()) {
                Text(
                    text = "Please enter your full name",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(verticalGap))

            // Phone Number
            Text(
                text = "Phone Number*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp)
            )
            PhoneInputField(
                phoneNumber     = phoneNumber,
                onPhoneChange   = { input ->
                    phoneNumber = input
                    phoneAlreadyRegisteredError = null
                },
                selectedCountry = selectedCountry,
                onCountryClick  = { showPicker = true },
                fieldHeight     = fieldHeight,
                isError         = submitAttempted && (!PhoneValidator.isValid(phoneNumber, selectedCountry.dialCode, selectedCountry.phoneLength) || phoneAlreadyRegisteredError != null),
                labelSize       = labelSize,
                placeholder     = if (isCompact) "Phone number" else "Enter Your Valid Number"
            )
            if (phoneNumber.isNotEmpty() && !PhoneValidator.isValid(phoneNumber, selectedCountry.dialCode, selectedCountry.phoneLength)) {
                Text(
                    text = PhoneValidator.errorMessage(selectedCountry.dialCode, selectedCountry.phoneLength),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
            if (phoneAlreadyRegisteredError != null) {
                Text(
                    text = phoneAlreadyRegisteredError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(Modifier.height(verticalGap))

            // Email
            Text(
                text = "Email*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text(
                        if (isCompact) "Email address" else "Enter Your Valid E-mail",
                        color = Color(0xFFAAAAAA),
                        fontSize = labelSize,
                        maxLines = 1
                    )
                },
                modifier = Modifier.fillMaxWidth().height(emailFieldHeight),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Color(0xFFF8FAF7),
                    unfocusedContainerColor = Color(0xFFF8FAF7),
                    unfocusedBorderColor    = Color(0xFFE0E0E0),
                    focusedBorderColor      = Color(0xFF6A9F50),
                    cursorColor             = Color(0xFF6A9F50)
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

            Spacer(Modifier.height(if (isCompact) 20.dp else 32.dp))

            // Send OTP Button
            val phoneValid = PhoneValidator.isValid(phoneNumber, selectedCountry.dialCode, selectedCountry.phoneLength)
            Button(
                onClick = {
                    focusManager.clearFocus()
                    submitAttempted = true
                    phoneAlreadyRegisteredError = null
                    if (!phoneValid) return@Button

                    val fullPhone = "${selectedCountry.dialCode}$phoneNumber"

                    Analytics.logEvent(
                        name = "signup_details_entered",
                        params = mapOf(
                            "name_length"   to fullName.length,
                            "phone_masked"  to maskPhone(phoneNumber),
                            "email_domain"  to email.substringAfter("@", ""),
                            "country_code"  to selectedCountry.code
                        )
                    )
                    Analytics.logEvent(
                        name = "generate_lead",
                        params = mapOf(
                            "source"       to "signup_screen",
                            "phone_masked" to maskPhone(phoneNumber),
                            "country_code" to selectedCountry.code
                        )
                    )

                    onSendOTPClick(fullName, fullPhone, email)
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
                    defaultElevation  = 2.dp,
                    pressedElevation  = 4.dp
                ),
                enabled = !isLoading &&
                        fullName.isNotEmpty() &&
                        phoneValid &&
                        email.isNotEmpty() &&
                        EmailValidator.isValidEmail(email)
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
                        color      = Color.White,
                        fontSize   = buttonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))

            if (error != null) {
                Text(
                    text      = error!!,
                    color     = MaterialTheme.colorScheme.error,
                    style     = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(if (isCompact) 4.dp else 8.dp))
            }

            // Terms
            val termsSize = if (isCompact) 11.sp else 12.sp
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF888888), fontSize = termsSize)) {
                        append("By signing up, you agree to the ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF6A9F50), fontSize = termsSize, fontWeight = FontWeight.SemiBold)) {
                        append("Terms of Service")
                    }
                    withStyle(SpanStyle(color = Color(0xFF888888), fontSize = termsSize)) {
                        append(" and ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF6A9F50), fontSize = termsSize, fontWeight = FontWeight.SemiBold)) {
                        append("Data Processing Agreement")
                    }
                },
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun formatFullName(input: String): String =
    input
        .filter { it.isLetter() || it == ' ' }
        .replace("\\s+".toRegex(), " ")
        .trimStart()
        .split(" ")
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }