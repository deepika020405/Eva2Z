package com.developer.ev2z.ui.screens.Auth

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
//    var showDialog by remember { mutableStateOf(false) }
//    var dialogMessage by remember { mutableStateOf("") }
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
//    LaunchedEffect(error) {
//        error?.let { errorMsg ->
//            when {
//                // CASE 1: User not registered
//                errorMsg.contains("not registered", ignoreCase = true) ||
//                        errorMsg.contains("not found", ignoreCase = true) -> {
////                    dialogMessage = "Phone number not registered!\nRedirecting to signup..."
////                    showDialog = true
//                    delay(2000)
////                    showDialog = false
//                    delay(300)
//                    onNotRegistered(phoneNumber)
//                }
//
//                // CASE 2: Wrong password
//                errorMsg.contains("password", ignoreCase = true) ||
//                        errorMsg.contains("incorrect", ignoreCase = true) -> {
//                    loginAttempts++
//
//                    if (loginAttempts >= 3) {
////                        dialogMessage = "Too many failed attempts.\nRedirecting to reset password..."
////                        showDialog = true
//                        delay(2000)
////                        showDialog = false
//                        delay(300)
//                        onForgetPasswordClick(phoneNumber)
//                    } else {
////                        dialogMessage = "Incorrect password!\nAttempt $loginAttempts/3"
////                        showDialog = true
//                        delay(2000)
////                        showDialog = false
//                    }
//                }
//
//                // CASE 3: Other errors
//                else -> {
////                    dialogMessage = errorMsg
////                    showDialog = true
//                    delay(3000)
////                    showDialog = false
//                }
//            }
//            viewModel.clearError()
//        }
//    }

    LaunchedEffect(error) {
        error?.let { errorMsg ->

            // reset all
            phoneError = null
            passwordError = null
            generalError = null

            when {
                // CASE 1: Phone not registered
                errorMsg.contains("not registered", true) ||
                        errorMsg.contains("not found", true) -> {

                    phoneError = "This phone number is not registered"

                    delay(1500)
                    onNotRegistered(phoneNumber)
                }

                // CASE 2: Wrong password
                errorMsg.contains("password", true) ||
                        errorMsg.contains("incorrect", true) -> {

                    loginAttempts++

                    passwordError =
                        if (loginAttempts >= 3)
                            "Too many failed attempts. Redirecting to reset password…"
                        else
                            "Incorrect password (Attempt $loginAttempts/3)"

                    if (loginAttempts >= 3) {
                        delay(1500)
                        onForgetPasswordClick(phoneNumber)
                    }
                }

                // CASE 3: Other / server error
                else -> {
                    generalError = errorMsg
                }
            }

            viewModel.clearError()
        }
    }

//    if (showDialog) {
//        AlertDialog(
//            onDismissRequest = { },
//            title = { Text("Notice", fontWeight = FontWeight.Bold) },
//            text = { Text(dialogMessage) },
//            confirmButton = { }
//        )
//    }

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
                    phoneError = null
                    generalError = null
                    hasTriedLogin = false
//                    loginAttempts = 0
                }
            },
            prefix = {
                Text(
                    text = "🇮🇳 +91 ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
            },
            placeholder = { Text("Enter Your Registered Number") },
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

        if(hasTriedLogin){
            phoneError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
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
            onValueChange = {
                password = it
                passwordError = null
                generalError = null
                hasTriedLogin = false
            },
            placeholder = { Text("Enter Your Password") },
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
            isError = password.isNotEmpty() && password.length < 6,

            // 👇 PASSWORD SHOW / HIDE LOGIC
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
                        contentDescription = if (passwordVisible)
                            "Hide password"
                        else
                            "Show password",
                        tint = Color.Gray
                    )
                }
            }
        )

        if (password.isNotEmpty() && password.length < 6) {
            Text(
                text = "Password must be at least 6 characters",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        passwordError?.let {
            Text(
                text = it,
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
                hasTriedLogin = true
                phoneError = null
                passwordError = null
                generalError = null
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
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Start
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
