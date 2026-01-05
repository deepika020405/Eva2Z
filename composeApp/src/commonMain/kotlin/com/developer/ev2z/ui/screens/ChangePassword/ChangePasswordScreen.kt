// ChangePasswordScreen.kt - FIXED NAVIGATION
package com.developer.ev2z.ui.screens.ChangePassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService
import eva2z_connect_multiplatform.composeapp.generated.resources.Res
import eva2z_connect_multiplatform.composeapp.generated.resources.app_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@Composable
fun ChangePasswordScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit = {}
) {
    val viewModel = remember { UpdatePasswordViewModel(apiService, storageManager) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

//    var showSuccessDialog by remember { mutableStateOf(false) }
//    var showErrorDialog by remember { mutableStateOf(false) }
//    var dialogMessage by remember { mutableStateOf("") }
    var submitAttempted by remember { mutableStateOf(false) }

    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.loading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    // ✅ FIXED: Success handler with proper navigation
//    LaunchedEffect(updateSuccess) {
//        if (updateSuccess) {
//            println("✅ Password changed successfully - showing dialog")
////            dialogMessage = "Password changed successfully!"
////            showSuccessDialog = true
//            delay(2000)
//            println("✅ Hiding dialog")
////            showSuccessDialog = false
//            viewModel.resetUpdateSuccess()
//            delay(300) // Extra delay for smooth transition
//            println("✅ Navigating back")
//            onBackPressed()
//        }
//    }
//
//    // ✅ Error handler with dialog
//    LaunchedEffect(error) {
//        error?.let {
//            println("❌ Error: $it")
////            dialogMessage = when {
////                it.contains("incorrect", ignoreCase = true) -> "Current password is incorrect"
////                it.contains("network", ignoreCase = true) ||
////                        it.contains("failed", ignoreCase = true) -> "Network error. Please try again."
////                else -> it
////            }
////            showErrorDialog = true
//            delay(2000)
////            showErrorDialog = false
//            viewModel.clearError()
//        }
//    }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            viewModel.resetUpdateSuccess()
            onBackPressed() // ✅ Direct navigation
        }
    }

    LaunchedEffect(error) {
        error?.let {
            submitAttempted = true

            currentPasswordError = when {
                it.contains("incorrect", true) -> "Current password is incorrect"
                it.contains("network", true) || it.contains("failed", true) ->
                    "Network error. Please try again."
                else -> it
            }

            viewModel.clearError()
        }
    }

    PlatformBackHandler(enabled = true) {
        onBackPressed()
    }
    // Validation
    val isValid = currentPassword.length >= 6 &&
            newPassword.length >= 6 &&
            newPassword == confirmPassword &&
            currentPassword != newPassword

    val scrollState = rememberScrollState()

    // ✅ BackPress Handler - Only when NOT showing success dialog
//    if (!showSuccessDialog) {
//        BackPressHandler {
//            println("🔙 Back pressed")
//            onBackPressed()
//        }
//    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFE6EDE3)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.20f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(80.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Eva2z Connect",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight * 0.20f)
                    .background(
                        color = Color(0xFFFFFFFF),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .verticalScroll(scrollState)
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ✅ HEADER - Direct back
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        println("🔙 Header back clicked")
                        onBackPressed()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Change Password",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ CURRENT PASSWORD
                Text(
                    text = "Current Password*",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        currentPasswordError = null
                    },
                    placeholder = { Text("Enter current password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF6A9F50)
                    ),
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = submitAttempted && currentPasswordError != null
                )

                if (submitAttempted && currentPasswordError != null) {
                    Text(
                        text = currentPasswordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                if (currentPassword.isNotEmpty() && currentPassword.length < 6) {
                    Text(
                        text = "Password must be at least 6 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ NEW PASSWORD
                Text(
                    text = "New Password*",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        newPasswordError = null
                    },
                    placeholder = { Text("Enter new password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF6A9F50)
                    ),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = submitAttempted && newPasswordError != null
                )

                if (newPassword.isNotEmpty() && newPassword.length < 6) {
                    Text(
                        text = "Password must be at least 6 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                // Check if new password is different from current
                if (currentPassword.isNotEmpty() &&
                    newPassword.isNotEmpty() &&
                    currentPassword == newPassword
                ) {
                    Text(
                        text = "New password must be different from current password",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ CONFIRM PASSWORD
                Text(
                    text = "Confirm New Password*",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        confirmPasswordError = null
                    },
                    placeholder = { Text("Confirm new password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF6A9F50)
                    ),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = submitAttempted && confirmPasswordError != null
                )

                if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                    Text(
                        text = "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ✅ SUBMIT BUTTON
                Button(
                    onClick = {
                        submitAttempted = true

                        currentPasswordError = null
                        newPasswordError = null
                        confirmPasswordError = null

                        when {
                            currentPassword.length < 6 ->
                                currentPasswordError = "Password must be at least 6 characters"

                            newPassword.length < 6 ->
                                newPasswordError = "Password must be at least 6 characters"

                            currentPassword == newPassword ->
                                newPasswordError = "New password must be different from current password"

                            newPassword != confirmPassword ->
                                confirmPasswordError = "Passwords do not match"

                            else -> {
                                viewModel.changePassword(
                                    currentPassword,
                                    newPassword,
                                    confirmPassword
                                )
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
                    enabled = !isLoading && isValid
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Update Password",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ✅ Success Dialog - BLOCKS back press
//    if (showSuccessDialog) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.5f)),
//            contentAlignment = Alignment.Center
//        ) {
//            Card(
//                modifier = Modifier.padding(32.dp),
//                colors = CardDefaults.cardColors(containerColor = Color.White),
//                shape = RoundedCornerShape(16.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(24.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "✓ Success",
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFF4CAF50)
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = dialogMessage,
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )
//                }
//            }
//        }
//    }
//
//    // ✅ Error Dialog - Auto-dismiss
//    if (showErrorDialog) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.5f)),
//            contentAlignment = Alignment.Center
//        ) {
//            Card(
//                modifier = Modifier.padding(32.dp),
//                colors = CardDefaults.cardColors(containerColor = Color.White),
//                shape = RoundedCornerShape(16.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(24.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "✗ Error",
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.Bold,
//                        color = Color(0xFFF44336)
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = dialogMessage,
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )
//                }
//            }
//        }
//    }
}