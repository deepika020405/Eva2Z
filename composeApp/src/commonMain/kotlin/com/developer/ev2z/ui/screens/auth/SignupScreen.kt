package com.developer.ev2z.ui.screens.signupScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
//import com.developer.ev2z.request.SignupRequest
import com.developer.ev2z.ui.components.CommonPhoneField
import com.developer.ev2z.ui.components.CommonTextField
import com.developer.ev2z.viewmodel.AuthViewModel

@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onSignupSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isLoading by viewModel.loading.collectAsState()
    val signupSuccess by viewModel.signupSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account 🚀",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Name Field
        CommonTextField(
            value = name,
            onValueChange = { name = it },
            label = "Full Name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Field
        CommonPhoneField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone Number"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        CommonTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password Field
        CommonTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            isPassword = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Signup Button
        Button(
            onClick = {
                if (password == confirmPassword) {
                    val phoneNumber = "+91$phone"
//                    viewModel.signup(SignupRequest(name, phoneNumber, password))
                } else {
                    viewModel.setError("Passwords do not match")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading &&
                    name.isNotEmpty() &&
                    phone.length == 10 &&
                    password.isNotEmpty() &&
                    confirmPassword.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
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
    LaunchedEffect(signupSuccess) {
        if (signupSuccess) {
            onSignupSuccess()
            viewModel.resetSignupSuccess()
        }
    }
}