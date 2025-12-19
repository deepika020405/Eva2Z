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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    onOtpSent: (String) -> Unit, // ✅ Pass actual phone number
    onNotRegistered: (String) -> Unit = {}
) {
    var phoneNumber by remember(preFilledPhone) { mutableStateOf(preFilledPhone) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    // ✅ Track if we handled the current request
    var hasHandledResult by remember { mutableStateOf(false) }

    // ✅ Track if phone was auto-filled
    var wasAutoFilled by remember(preFilledPhone) {
        mutableStateOf(preFilledPhone.isNotEmpty())
    }

    // ✅ Track if user manually edited
    var manuallyEdited by remember { mutableStateOf(false) }

    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // ✅ Track previous loading state
    var previousLoading by remember { mutableStateOf(false) }

    // ✅ Auto-submit if pre-filled and 10 digits
    LaunchedEffect(preFilledPhone) {
        if (preFilledPhone.length == 10 && wasAutoFilled && !manuallyEdited) {
            hasHandledResult = false
            delay(500)
            val fullPhone = "+91$preFilledPhone"
            viewModel.forgotPassword(fullPhone)
        }
    }

    // ✅ Handle BOTH success and error when loading finishes
    LaunchedEffect(isLoading, phoneNumber) {
        // When loading transitions from true to false
        if (previousLoading && !isLoading && !hasHandledResult) {
            hasHandledResult = true

            val currentError = error

            when {
                // CASE 1: User not registered - redirect to signup
                currentError != null && currentError.contains("not registered", ignoreCase = true) -> {
                    println("❌ CASE 1: User not registered")
                    dialogMessage = "Phone number not registered!\nRedirecting to signup..."
                    showDialog = true
                    delay(2000)
                    showDialog = false
                    viewModel.clearError()
                    delay(300)
                    onNotRegistered(phoneNumber)
                }

                // CASE 2: Other errors
                currentError != null -> {
                    println("❌ CASE 2: Other error: $currentError")
                    dialogMessage = currentError
                    showDialog = true
                    delay(3000)
                    showDialog = false
                    viewModel.clearError()
                }

                // CASE 3: Success - OTP sent
                currentError == null && phoneNumber.length == 10 -> {
                    println("✅ CASE 3: Success! Calling onOtpSent with: $phoneNumber")
                    onOtpSent(phoneNumber) // ✅ Pass current phone number
                }
            }
        }

        previousLoading = isLoading
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
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Forgot Password",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            Text(
                text = "Phone Number*",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        phoneNumber = it
                        if (it != preFilledPhone) {
                            manuallyEdited = true
                        }
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

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    hasHandledResult = false
                    val fullPhone = "+91$phoneNumber"
                    viewModel.forgotPassword(fullPhone)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A9F50),
                    contentColor = Color.White
                ),
                enabled = !isLoading && phoneNumber.length == 10
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}