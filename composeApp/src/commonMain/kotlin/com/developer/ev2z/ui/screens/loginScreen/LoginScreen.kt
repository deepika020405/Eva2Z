package com.developer.ev2z.ui.screens.loginScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.ui.components.CommonPhoneField
import com.developer.ev2z.ui.components.CommonTextField
import com.developer.ev2z.viewmodel.AuthViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by viewModel.loading.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome Back 👋",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Phone Field - Using Common Component
        CommonPhoneField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field - Using Common Component
        CommonTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login Button
        // Login Button mein logging add karo
        Button(
            onClick = {
                println("🔘 Button clicked!")
                println("🔘 Phone: $phone, Password length: ${password.length}")

                val phoneNumber = "+91$phone"
                println("🔘 Full phone number: $phoneNumber")

                viewModel.login(LoginRequest(phoneNumber, password))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading && phone.length == 10 && password.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        // Error Message
        error?.let { errorMessage ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // Navigate on success
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            onLoginSuccess()
            viewModel.resetLoginSuccess()
        }
    }
}