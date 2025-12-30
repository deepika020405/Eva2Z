package com.developer.ev2z.ui.screens.Auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    val isLoading by viewModel.loading.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        Analytics.logEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "login_screen",
                "screen_class" to "LoginScreen"
            )
        )
    }

    // ✅ SIMPLE 3 CASES
    LaunchedEffect(error) {
        error?.let { errorMsg ->
            when {
                // CASE 1: User not registered
                errorMsg.contains("not registered", ignoreCase = true) ||
                        errorMsg.contains("not found", ignoreCase = true) -> {
                    dialogMessage = "Phone number not registered!\nRedirecting to signup..."
                    showDialog = true
                    delay(2000)
                    showDialog = false
                    delay(300)
                    onNotRegistered(phoneNumber)
                }

                // CASE 2: Wrong password
                errorMsg.contains("password", ignoreCase = true) ||
                        errorMsg.contains("incorrect", ignoreCase = true) -> {
                    loginAttempts++

                    if (loginAttempts >= 3) {
                        dialogMessage = "Too many failed attempts.\nRedirecting to reset password..."
                        showDialog = true
                        delay(2000)
                        showDialog = false
                        delay(300)
                        onForgetPasswordClick(phoneNumber)
                    } else {
                        dialogMessage = "Incorrect password!\nAttempt $loginAttempts/3"
                        showDialog = true
                        delay(2000)
                        showDialog = false
                    }
                }

                // CASE 3: Other errors
                else -> {
                    dialogMessage = errorMsg
                    showDialog = true
                    delay(3000)
                    showDialog = false
                }
            }
            viewModel.clearError()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Notice", fontWeight = FontWeight.Bold) },
            text = { Text(dialogMessage) },
            confirmButton = { }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 24.dp)
    ) {
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
                }
            },
            prefix = {
                Text(
                    text = "🇮🇳 +91 ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            },
            placeholder = { Text("98765 54321") },
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
            isError = phoneNumber.isNotEmpty() && phoneNumber.length < 10
        )

        if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
            Text(
                text = "Phone number must be 10 digits",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Password*",
            fontSize = 14.sp,
            color = Color(0xFF666666),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Enter your password") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Color(0xFF6A9F50)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = password.isNotEmpty() && password.length < 6
        )

        if (password.isNotEmpty() && password.length < 6) {
            Text(
                text = "Password must be at least 6 characters",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = {
                onForgetPasswordClick(phoneNumber)
            }) {
                Text(
                    text = "Forgot Password?",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val fullPhone = "+91$phoneNumber"
                viewModel.login(LoginRequest(fullPhone, password))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6A9F50),
                contentColor = Color.White
            ),
            enabled = !isLoading && phoneNumber.length == 10 && password.length >= 6
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Login",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

//    LaunchedEffect(loginSuccess) {
//        if (loginSuccess) {
//            onLoginSuccess()
//            viewModel.resetLoginSuccess()
//        }
//    }
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {

            Analytics.logEvent(
                name = "login_success",
                params = mapOf(
                    "login_method" to "password",
                    "country_code" to "IN",
                    "phone_masked" to maskPhone(phoneNumber),
                    "user_name" to viewModel.userName, // ya first name
                )
            )

            onLoginSuccess()
//            viewModel.resetLoginSuccess()
        }
    }

}

fun maskPhone(phone: String): String {
    return if (phone.length >= 4)
        "XXXXXX${phone.takeLast(4)}"
    else
        "XXXX"
}
