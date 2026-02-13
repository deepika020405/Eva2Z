package com.developer.ev2z.ui.screens.ChangePlan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.navigation.PlatformBackHandler

/* =========================================================
   MAIN SCREEN (UI ONLY)
========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlanScreen(
    devices: List<DevicePlanUi>,
    onBack: () -> Unit,
    onRenewConfirm: (deviceId: String, duration: String) -> Unit
) {
    PlatformBackHandler(true){
        onBack()
    }
    var selectedDevice by remember { mutableStateOf<DevicePlanUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plans & Subscription") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back"
                        )

                    }
                }
            )
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            items(devices) { device ->
                DevicePlanCard(
                    device = device,
                    onRenew = { selectedDevice = device }
                )
            }
        }
    }

    // 🔽 Bottom Sheet
    selectedDevice?.let { device ->
        RenewPlanBottomSheet(
            device = device,
            onDismiss = { selectedDevice = null },
            onConfirm = { duration ->
                onRenewConfirm(device.imei, duration) // ✅ correct
            selectedDevice = null
            }
        )
    }
}

/* =========================================================
   UI COMPONENTS
========================================================= */

@Composable
fun DevicePlanCard(
    device: DevicePlanUi,
    onRenew: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(device.deviceName, fontWeight = FontWeight.Bold)
                    Text("Id: ${device.imei}", fontSize = 12.sp, color = Color.Gray)                }
                ExpiryChip(device.daysLeft)
            }

            Spacer(Modifier.height(12.dp))

            InfoRow("Plan", device.planType)
            InfoRow("Expiry Date", device.expiryDate)
            InfoRow(
                "SIM Number",
                device.simNumber?.takeIf { it.isNotBlank() } ?: "Not Available"
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onRenew,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Renew")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewPlanBottomSheet(
    device: DevicePlanUi,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var duration by remember { mutableStateOf("1 Year") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            Text(
                text = "Renew:  ${device.deviceName}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            DurationOption(
                text = "1 Year",
                selected = true,
                onClick = { duration = "1 Year" }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onConfirm(duration) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Proceed to Payment")
            }
        }
    }
}

@Composable
fun DurationOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        border = if (selected) BorderStroke(1.dp, Color(0xFF4CAF50)) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, Modifier.weight(1f))
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun ExpiryChip(daysLeft: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (daysLeft <= 7)
            Color(0xFFFFF8E1)
        else
            Color(0xFFE8F5E9)
    ) {
        Text(
            text = "$daysLeft days left",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
    }
}
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}