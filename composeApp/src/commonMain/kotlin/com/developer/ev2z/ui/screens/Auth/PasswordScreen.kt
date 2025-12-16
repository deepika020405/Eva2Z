package com.developer.ev2z.ui.screens.Auth

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.BackPressHandler

enum class PasswordScreenMode {
    CREATE,      // For registration (2 fields)
    CHANGE       // For updating password (3 fields)
}

@Composable
fun PasswordScreen(
    mode: PasswordScreenMode = PasswordScreenMode.CREATE,
    title: String = if (mode == PasswordScreenMode.CREATE) "Create Password" else "Change Password",
    subtitle: String = if (mode == PasswordScreenMode.CREATE)
        "Create a secure password for your account"
    else "Update your account password",
    buttonText: String = if (mode == PasswordScreenMode.CREATE) "Submit" else "Update Password",
    isLoading: Boolean = false,
    error: String? = null,
    onPasswordSubmit: (String, String) -> Unit = { _, _ -> }, // For CREATE mode (new, confirm)
    onChangePasswordSubmit: (String, String, String) -> Unit = { _, _, _ -> }, // For CHANGE mode (current, new, confirm)
    onBack: () -> Unit = {}
) {
    // Common password states
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Change password specific state
    var currentPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }

    var showBackDialog by remember { mutableStateOf(false) }

    // Validation
    val isNewPasswordValid = newPassword.length >= 6
    val doPasswordsMatch = newPassword == confirmPassword && confirmPassword.isNotEmpty()
    val isCurrentPasswordValid = mode == PasswordScreenMode.CREATE || currentPassword.length >= 6

    val isFormValid = when (mode) {
        PasswordScreenMode.CREATE -> isNewPasswordValid && doPasswordsMatch
        PasswordScreenMode.CHANGE -> isCurrentPasswordValid && isNewPasswordValid && doPasswordsMatch
    }

    val scrollState = rememberScrollState()

    // ✅ Cross-platform back button handler
    BackPressHandler { showBackDialog = true }

    // ✅ Back confirmation dialog
    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = { Text("Confirm", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = {
                    showBackDialog = false
                    onBack()
                }) {
                    Text("Yes", color = Color(0xFF6A9F50), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) {
                    Text("No", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showBackDialog = true }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF252A23),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ✅ CURRENT PASSWORD (Only for CHANGE mode)
        if (mode == PasswordScreenMode.CHANGE) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password*") },
                modifier = Modifier.fillMaxWidth(),
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
                isError = currentPassword.isNotEmpty() && currentPassword.length < 6
            )

            if (currentPassword.isNotEmpty() && currentPassword.length < 6) {
                Text(
                    text = "Password must be at least 6 characters",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ✅ NEW PASSWORD
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(if (mode == PasswordScreenMode.CHANGE) "New Password*" else "Password*") },
            modifier = Modifier.fillMaxWidth(),
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
            isError = newPassword.isNotEmpty() && newPassword.length < 6
        )

        if (newPassword.isNotEmpty() && newPassword.length < 6) {
            Text(
                text = "Password must be at least 6 characters",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        // Check if new password is different from current (CHANGE mode only)
        if (mode == PasswordScreenMode.CHANGE &&
            currentPassword.isNotEmpty() &&
            newPassword.isNotEmpty() &&
            currentPassword == newPassword) {
            Text(
                text = "New password must be different from current password",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ CONFIRM PASSWORD
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(if (mode == PasswordScreenMode.CHANGE) "Confirm New Password*" else "Confirm Password*") },
            modifier = Modifier.fillMaxWidth(),
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
            isError = confirmPassword.isNotEmpty() && !doPasswordsMatch
        )

        if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ✅ SUBMIT BUTTON
        Button(
            onClick = {
                if (isFormValid) {
                    when (mode) {
                        PasswordScreenMode.CREATE -> {
                            onPasswordSubmit(newPassword, confirmPassword)
                        }
                        PasswordScreenMode.CHANGE -> {
                            onChangePasswordSubmit(currentPassword, newPassword, confirmPassword)
                        }
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
            enabled = !isLoading && isFormValid
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = buttonText,
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
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}