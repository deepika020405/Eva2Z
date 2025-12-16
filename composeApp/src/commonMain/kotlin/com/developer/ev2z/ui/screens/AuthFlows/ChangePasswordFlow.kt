package com.developer.ev2z.ui.screens.AuthFlows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.developer.ev2z.ui.screens.Auth.PasswordScreen
import com.developer.ev2z.ui.screens.Auth.PasswordScreenMode
import com.developer.ev2z.ui.screens.Auth.UpdatePasswordViewModel
import kotlinx.coroutines.delay

@Composable
fun ChangePasswordFlow(
    viewModel: UpdatePasswordViewModel,
    onBack: () -> Unit = {},
    onSuccess: () -> Unit = {}
) {
    val isLoading by viewModel.loading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    var showSuccessDialog by remember { mutableStateOf(false) }

    // ✅ Auto-navigate after success
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            println("✅ Password updated successfully!")
            showSuccessDialog = true
            delay(2000)
            viewModel.resetUpdateSuccess()
            onSuccess()
        }
    }

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
                Modifier
                    .fillMaxSize()
                    .padding(top = screenHeight * 0.20f)
                    .background(
                        color = Color(0xFFFFFFFF),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
                PasswordScreen(
                    mode = PasswordScreenMode.CHANGE,
                    title = "Change Password",
                    subtitle = "Enter your current password and create a new one",
                    buttonText = "Update Password",
                    isLoading = isLoading,
                    error = error,
                    onChangePasswordSubmit = { current, new, confirm ->
                        println("🔐 Changing password...")
                        viewModel.changePassword(current, new, confirm)
                    },
                    onBack = onBack
                )
            }
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { },
            title = { androidx.compose.material3.Text("Success") },
            text = { androidx.compose.material3.Text("Password updated successfully!") },
            confirmButton = { }
        )
    }
}