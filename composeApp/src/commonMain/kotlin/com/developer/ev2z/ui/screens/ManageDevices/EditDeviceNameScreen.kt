// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/ManageDevices/EditDeviceNameScreen.kt
package com.developer.ev2z.ui.screens.ManageDevices

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.network.ApiService

private val primaryContainerLight = Color(0xFF00C853)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceNameScreen(
    apiService: ApiService,
    storageManager: StorageManager,
    deviceId: String,
    deviceName: String,
    simNumber: String,
    onBackPressed: () -> Unit,
    onUpdateSuccess: () -> Unit
) {
    val viewModel = remember { EditDeviceNameViewModel(apiService) }
    val uiState by viewModel.uiState.collectAsState()

    val userPrefs = remember { UserPrefs(storageManager) }
    val token = remember { userPrefs.getToken() ?: "" }

    PlatformBackHandler(enabled = true) { onBackPressed() }

    var newDeviceName by remember { mutableStateOf(deviceName) }

    // Handle update success
    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            onUpdateSuccess()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Back Button
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color.Gray, CircleShape)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardBackspace,
                "Back",
                tint = Color.Black
            )
        }

        // Logo
        Image(
            Icons.Filled.AccountCircle,
            "logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(75.dp)
        )

        // Title
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Edit Device Name",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Text(
                "Update your device name",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )
        }

        // Device Name Input (Editable)
        OutlinedTextField(
            value = newDeviceName,
            onValueChange = { newDeviceName = it },
            label = { Text("Device Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            isError = newDeviceName.trim().length < 3,
            supportingText = {
                if (newDeviceName.trim().length < 3) {
                    Text(
                        "Device name must be at least 3 characters",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        // Device ID (Disabled)
        OutlinedTextField(
            value = deviceId,
            onValueChange = {},
            label = { Text("Device ID") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color.LightGray,
                disabledLabelColor = Color.Gray
            )
        )

        // SIM Number (Disabled)
        OutlinedTextField(
            value = simNumber,
            onValueChange = {},
            label = { Text("SIM Number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color.LightGray,
                disabledLabelColor = Color.Gray
            )
        )

        // Note
        Text(
            "* Only device name can be updated",
            modifier = Modifier
                .alpha(0.7f)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        // Error Message
        uiState.error?.let { error ->
            Text(
                error,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // Success Message
        if (uiState.updateSuccess) {
            Text(
                "Device name updated successfully!",
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // Buttons
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .weight(0.3f)
                    .border(1.dp, Color.Gray, RoundedCornerShape(100.dp))
            ) {
                Text("Cancel", color = Color.Black)
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = {
                    val trimmedName = newDeviceName.trim()
                    if (trimmedName.length >= 3) {
                        viewModel.updateDeviceName(
                            token = token,
                            deviceId = deviceId,
                            newName = trimmedName
                        )
                    }
                },
                enabled = !uiState.isLoading &&
                        newDeviceName.trim().length >= 3 &&
                        newDeviceName.trim() != deviceName.trim(),
                modifier = Modifier.weight(0.7f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryContainerLight
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (uiState.isLoading) "Updating..." else "Update Device",
                    color = Color.White
                )
            }
        }
    }
}