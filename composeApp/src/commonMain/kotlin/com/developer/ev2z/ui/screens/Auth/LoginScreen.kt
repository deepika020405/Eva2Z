package com.developer.ev2z.ui.screens.Auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.util.Analytics
import com.developer.ev2z.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onForgetPasswordClick: (String) -> Unit = {},
    onNotRegistered: (String) -> Unit = {},
    preFilledPhone: String = ""
) {
    var phoneNumber by remember(preFilledPhone) { mutableStateOf(preFilledPhone) }
    var password by remember { mutableStateOf("") }
    var loginAttempts by remember { mutableStateOf(0) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var hasTriedLogin by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by viewModel.loading.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "login_screen",
                "screen_class" to "LoginScreen"
            )
        )
    }

    LaunchedEffect(error) {
        error?.let { errorMsg ->
            phoneError = null
            passwordError = null
            generalError = null

            when {
                errorMsg.contains("not registered", true) ||
                        errorMsg.contains("not found", true) -> {
                    phoneError = "This phone number is not registered"
                    delay(1500)
                    onNotRegistered(phoneNumber)
                }

                errorMsg.contains("password", true) ||
                        errorMsg.contains("incorrect", true) -> {
                    loginAttempts++
                    passwordError =
                        if (loginAttempts >= 3)
                            "Too many failed attempts. Please reset your password…"
                        else
                            "Incorrect password "

                    if (loginAttempts >= 5) {
                        delay(1500)
                        onForgetPasswordClick(phoneNumber)
                    }
                }

                else -> {
                    generalError = errorMsg
                }
            }
            viewModel.clearError()
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = horizontalPad, vertical = if (isCompact) 16.dp else 24.dp)
        ) {
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))

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
                    if (digitsOnly.length > 10 && input.length > phoneNumber.length) {
                        return@OutlinedTextField
                    }
                    val finalNumber =
                        if (digitsOnly.length <= 10) digitsOnly
                        else digitsOnly.takeLast(10)
                    phoneNumber = finalNumber
                    phoneError = null
                    generalError = null
                    hasTriedLogin = false
                    loginAttempts = 0
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
                isError = phoneNumber.isNotEmpty() && phoneNumber.length < 10
            )

            if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                Text(
                    text = "Phone number must be 10 digits",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            if (hasTriedLogin) {
                phoneError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(verticalGap))

            // Password Label
            Text(
                text = "Password*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    generalError = null
                    hasTriedLogin = false
                },
                placeholder = {
                    Text(
                        "Enter Your Password",
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = password.isNotEmpty() && password.length < 6,
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Filled.VisibilityOff
                            else
                                Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF999999)
                        )
                    }
                }
            )

            if (password.isNotEmpty() && password.length < 6) {
                Text(
                    text = "Password must be at least 6 characters",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            passwordError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Forgot Password
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(onClick = { onForgetPasswordClick(phoneNumber) }) {
                    Text(
                        text = "Forgot Password?",
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        color = Color(0xFF6A9F50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 24.dp))

            // Login Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    hasTriedLogin = true
                    phoneError = null
                    passwordError = null
                    generalError = null
                    val fullPhone = "+91$phoneNumber"
                    viewModel.login(LoginRequest(fullPhone, password))
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
                enabled = !isLoading && phoneNumber.length == 10 && password.length >= 6
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Login",
                        color = Color.White,
                        fontSize = buttonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // General Error Banner
            generalError?.let {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = if (isCompact) 12.sp else 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        LaunchedEffect(loginSuccess) {
            if (loginSuccess) {
                Analytics.logEvent(
                    name = "login_success",
                    params = mapOf(
                        "login_method" to "password",
                        "country_code" to "IN",
                        "phone_masked" to maskPhone(phoneNumber),
                        "user_name" to viewModel.userName,
                    )
                )
                onLoginSuccess()
            }
        }
    }
}

fun maskPhone(phone: String): String {
    return if (phone.length >= 4)
        "XXXXXX${phone.takeLast(4)}"
    else
        "XXXX"
}