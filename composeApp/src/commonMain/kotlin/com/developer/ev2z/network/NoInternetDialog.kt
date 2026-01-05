package com.developer.ev2z.network

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun NoInternetDialog(
    settingsOpener: NetworkSettingsOpener,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No Internet Connection") },
        text = { Text("Please turn on your internet to continue") },
        confirmButton = {
            TextButton(onClick = {
                settingsOpener.openInternetSettings()
            }) {
                Text("Turn On Internet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
