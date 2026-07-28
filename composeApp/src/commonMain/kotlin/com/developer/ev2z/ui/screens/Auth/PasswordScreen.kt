package com.developer.ev2z.ui.screens.Auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.BackPressHandler
import com.developer.ev2z.ui.components.dismissKeyboardOnTap
import com.developer.ev2z.ui.components.isKeyboardVisible
import com.developer.ev2z.ui.components.screenHeightDp

@Composable
fun PasswordScreen(
    title: String = "Create Password",
    subtitle: String = "Create a secure password for your account",
    buttonText: String = "Submit",
    isLoading: Boolean = false,
    error: String? = null,
    onPasswordSubmit: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {}
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }

    val isNewPasswordValid = newPassword.length >= 6
    val doPasswordsMatch = newPassword == confirmPassword && confirmPassword.isNotEmpty()
    val isFormValid = isNewPasswordValid && doPasswordsMatch

    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val keyboardVisible = isKeyboardVisible()

    BackPressHandler { showBackDialog = true }

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = {
                Text(
                    "Confirm",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text("Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = {
                    showBackDialog = false
                    onBack()
                }) {
                    Text("Yes", color = Color(0xFF6A9F50), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) {
                    Text("No", color = Color(0xFF888888))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        // Window height, not this box's — the screen is hosted inside a card below a header.
        val isCompact = screenHeightDp() < 700.dp

        // Responsive sizes
        val headerFontSize = if (isCompact) 18.sp else 20.sp
        val titleSize = if (isCompact) 24.sp else 28.sp
        val subtitleSize = if (isCompact) 13.sp else 14.sp
        val labelSize = if (isCompact) 12.sp else 14.sp
        val fieldHeight = if (isCompact) 50.dp else 56.dp
        val buttonHeight = if (isCompact) 46.dp else 52.dp
        val buttonTextSize = if (isCompact) 15.sp else 16.sp
        val verticalGap = if (isCompact) 12.dp else 18.dp
        val horizontalPad = (screenWidth * 0.06f).coerceIn(16.dp, 32.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                // imePadding before the scroll so the keyboard shrinks the viewport — that
                // is what lets the focused field scroll itself into view.
                .imePadding()
                .verticalScroll(scrollState)
                .dismissKeyboardOnTap(focusManager)
                .padding(horizontal = horizontalPad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showBackDialog = true }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Back",
                        tint = Color(0xFF333333)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = headerFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )
            }

            // Title + subtitle are folded away while typing so both password fields and the
            // submit button stay above the keyboard.
            if (!keyboardVisible) {
                Spacer(modifier = Modifier.height(if (isCompact) 20.dp else 28.dp))

                // Title
                Text(
                    text = title,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = subtitle,
                    fontSize = subtitleSize,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    lineHeight = (subtitleSize.value * 1.5f).sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(if (isCompact) 32.dp else 48.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // New Password Label
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
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = {
                    Text(
                        "Enter Your Password",
                        color = Color(0xFFAAAAAA),
                        fontSize = labelSize
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = fieldHeight),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAF7),
                    unfocusedContainerColor = Color(0xFFF8FAF7),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFF6A9F50),
                    cursorColor = Color(0xFF6A9F50)
                ),
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF999999)
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
                        .padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(verticalGap))

            // Confirm Password Label
            Text(
                text = "Confirm Password*",
                fontSize = labelSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF555555),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = {
                    Text(
                        "Confirm Your Password",
                        color = Color(0xFFAAAAAA),
                        fontSize = labelSize
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = fieldHeight),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAF7),
                    unfocusedContainerColor = Color(0xFFF8FAF7),
                    disabledContainerColor = Color(0xFFF5F5F5),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedBorderColor = Color(0xFF6A9F50),
                    cursorColor = Color(0xFF6A9F50)
                ),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = Color(0xFF999999)
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
                        .padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (isCompact) 24.dp else 32.dp))

            // Submit Button
            Button(
                onClick = {
                    if (isFormValid) {
                        onPasswordSubmit(newPassword, confirmPassword)
                    }
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
                            fontSize = buttonTextSize,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontSize = buttonTextSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Server / API Error
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = if (isCompact) 12.sp else 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}