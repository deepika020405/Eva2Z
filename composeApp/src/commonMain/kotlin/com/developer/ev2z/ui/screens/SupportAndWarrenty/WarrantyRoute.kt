package com.developer.ev2z.ui.screens.SupportAndWarrenty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

@Composable
fun WarrantyRoute(
    deviceId: String,
    onBack: () -> Unit,
    onBuyWarranty: () -> Unit,
    onAddDevice: () -> Unit
) {
    val viewModel = remember { WarrantyViewModel() }
    val state by viewModel.state.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState() // 👈 new flag

    LaunchedEffect(deviceId) {
        println("🟡 WarrantyRoute received deviceId = $deviceId")
        viewModel.loadWarranty(deviceId)
    }

    when {
        !isLoaded -> {
            // ⏳ Still loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state == null -> {
            // ❌ No device found
            NoDeviceScreen(onBack = onBack)
        }

        else -> {
            // ✅ Device found
            SupportWarrantyScreen(
                data = state!!,
                onBack = onBack,
                onBuyWarranty = onBuyWarranty
            )
        }
    }
}

@Composable
fun NoDeviceScreen(onBack: () -> Unit) {
    PlatformBackHandler(true) { onBack() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "No Device Added",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "It looks like you haven't added any device yet.\nPlease add a device to view warranty details.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}