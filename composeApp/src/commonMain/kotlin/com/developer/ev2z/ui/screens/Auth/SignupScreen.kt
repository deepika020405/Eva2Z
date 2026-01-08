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
    preFilledPhone: String = "" // ✅ Accept pre-filled phone
) {
    var fullName by remember { mutableStateOf("") }

    // ✅ Initialize with pre-filled phone
    var phoneNumber by remember(preFilledPhone) {
        mutableStateOf(preFilledPhone)
    }

    var email by remember { mutableStateOf("") }
    var phoneAlreadyRegisteredError by remember { mutableStateOf<String?>(null) }
    var submitAttempted by remember { mutableStateOf(false) }

    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "sign_up_screen",
                "screen_class" to "SignupScreen"
            )
        )
    }


    // ✅ Clear pre-filled phone after using it
    LaunchedEffect(preFilledPhone) {
        if (preFilledPhone.isNotEmpty()) {
            phoneNumber = preFilledPhone
        }
    }

//    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Full Name*",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = {
                if (it.length <= 50) fullName = it
            },
            placeholder = { Text("Enter Your Full Name") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF6A9F50)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Phone Number*",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                    phoneNumber = it
                    phoneAlreadyRegisteredError = null
                }
            },
            prefix = {
                Text(
                    text = "🇮🇳 +91 ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            },
            placeholder = { Text("Enter Your Valid Number") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF6A9F50)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        if (submitAttempted) {
            when {

                phoneAlreadyRegisteredError != null -> {
                    Text(
                        text = phoneAlreadyRegisteredError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }
//
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Email*",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Enter Your Valid E-mail") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF6A9F50)
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
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                submitAttempted = true
                phoneAlreadyRegisteredError = null

                if (phoneNumber.length < 10) return@Button

                val fullPhone = "+91$phoneNumber"

                // 🔥 EVENT 1: Signup details filled
                Analytics.logEvent(
                    name = "signup_details_entered",
                    params = mapOf(
                        "name_length" to fullName.length,
                        "phone_masked" to maskPhone(phoneNumber),
                        "email_domain" to email.substringAfter("@", ""),
                        "country_code" to "IN"
                    )
                )

                // 🔥 EVENT 2: Generate Lead (marketing / Meta)
                Analytics.logEvent(
                    name = "generate_lead",
                    params = mapOf(
                        "source" to "signup_screen",
                        "phone_masked" to maskPhone(phoneNumber),
                        "email_domain" to email.substringAfter("@", ""),
                        "country_code" to "IN"
                    )
                )

                // ✅ First check if phone is already registered
//                viewModel.checkPhoneRegistered(fullPhone) { isRegistered, message ->
//                    if (isRegistered) {
//                        scope.launch {
//                            onAlreadyRegistered(phoneNumber)
//                        }
//                    } else {
//                        // ✅ Proceed with signup
//                        onSendOTPClick(fullName, phoneNumber, email)
//                    }
//                }

                viewModel.checkPhoneRegistered(fullPhone) { isRegistered ->
                    if (isRegistered) {
                        phoneAlreadyRegisteredError = "This phone number is already registered!"
                        onAlreadyRegistered(phoneNumber)
                    } else {
                        onSendOTPClick(fullName, phoneNumber, email)
                    }
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
            enabled = !isLoading &&
                    fullName.isNotEmpty() &&
                    phoneNumber.length == 10 &&
                    email.isNotEmpty() &&
                    EmailValidator.isValidEmail(email)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Send OTP",
                    color = Color.White,
                    fontSize = 16.sp,
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
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color(0xFF666666), fontSize = 12.sp)) {
                append("By signing up, you agree to the ")
            }
            withStyle(style = SpanStyle(color = Color(0xFF6A9F50), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                append("Terms of Service")
            }
            withStyle(style = SpanStyle(color = Color(0xFF666666), fontSize = 12.sp)) {
                append(" and ")
            }
            withStyle(style = SpanStyle(color = Color(0xFF6A9F50), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)) {
                append("Data Processing Agreement")
            }
        }


        Spacer(modifier = Modifier.height(32.dp))
    }
}