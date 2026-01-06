package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.Image
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
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun ForgetPasswordPhoneScreen(
    viewModel: AuthViewModel,
    preFilledPhone: String = "",
    onBack: () -> Unit,
    onOtpSent: (String) -> Unit, // ✅ Pass actual phone number
    onNotRegistered: (String) -> Unit = {}
) {
    var phoneNumber by remember(preFilledPhone) { mutableStateOf(preFilledPhone) }
//    var showDialog by remember { mutableStateOf(false) }
//    var dialogMessage by remember { mutableStateOf("") }

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
    var phoneError by remember { mutableStateOf<String?>(null) }

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
                    if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                        phoneNumber = it
                        manuallyEdited = true
                        phoneError = null          // ✅ clear inline error
                    }
                },
                prefix = {
                    Text(
                        text = "🇮🇳 +91 ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                },
                placeholder = { Text("Enter Your valid Number") },
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

            if (phoneError != null) {
                Text(
                    text = phoneError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp)
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