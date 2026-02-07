package com.developer.ev2z.ui.screens.ChangePlan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ChangePlanRoute(
    onBack: () -> Unit
) {
    // ✅ ViewModel created when route is shown (CMP safe)
    val viewModel = remember {
        ChangePlanViewModel()
    }

    // ✅ Flow collection (CMP)
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.error ?: "Something went wrong",
                    color = Color.Red
                )
            }
        }

        else -> {
            ChangePlanScreen(
                devices = state.devices,
                onBack = onBack,
                onRenew = { device ->
                    viewModel.renewPlan(device.deviceId)
                },
                onChangePlan = { device ->
                    viewModel.changePlan(device.deviceId)
                }
            )
        }
    }
}