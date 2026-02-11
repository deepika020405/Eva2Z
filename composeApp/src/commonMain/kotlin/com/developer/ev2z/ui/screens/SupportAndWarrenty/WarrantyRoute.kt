package com.developer.ev2z.ui.screens.SupportAndWarrenty

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun WarrantyRoute(
    deviceId: String,
    onBack: () -> Unit,
    onBuyWarranty: () -> Unit
) {

    val viewModel = remember { WarrantyViewModel() }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(deviceId) {
        println("🟡 WarrantyRoute received deviceId = $deviceId")
        viewModel.loadWarranty(deviceId)
    }

    if (state == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        SupportWarrantyScreen(
            data = state!!,
            onBack = onBack,
            onBuyWarranty = onBuyWarranty
        )
    }
}