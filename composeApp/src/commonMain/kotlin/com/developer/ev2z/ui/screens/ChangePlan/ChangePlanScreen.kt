package com.developer.ev2z.ui.screens.ChangePlan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.*
import com.developer.ev2z.navigation.PlatformBackHandler

/* =========================================================
   RESPONSIVE HELPERS (CMP SAFE)
========================================================= */

@Composable
fun responsivePadding(maxWidth: Dp): Dp = when {
    maxWidth >= 840.dp -> 32.dp
    maxWidth >= 600.dp -> 24.dp
    else -> 16.dp
}

@Composable
fun responsiveContentWidth(maxWidth: Dp): Dp =
    if (maxWidth >= 840.dp) 700.dp else Dp.Unspecified

/* =========================================================
   MAIN SCREEN
========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlanScreen(
    devices: List<DevicePlanUi>,
    onBack: () -> Unit,
    onRenewConfirm: (deviceId: String, duration: String) -> Unit
) {
    PlatformBackHandler(true) { onBack() }

    var selectedDevice by remember { mutableStateOf<DevicePlanUi?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plans & Subscription") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back")
                    }
                }
            )
        }
    ) { padding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val pad = responsivePadding(maxWidth)
            val contentWidth = responsiveContentWidth(maxWidth)

            // ✅ EMPTY STATE (NEW — safe addition)
            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Add a device to start using plans & subscriptions.",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // ✅ EXISTING DEVICE LIST (unchanged)
                LazyColumn(

                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = pad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(devices) { device ->
                        DevicePlanCard(
                            device = device,
                            onRenew = { selectedDevice = device },
                            contentWidth = contentWidth
                        )
                    }
                }
            }
        }
    }

    // ✅ Bottom sheet logic (unchanged)
    selectedDevice?.let { device ->
        RenewPlanBottomSheet(
            device = device,
            onDismiss = { selectedDevice = null },
            onConfirm = {
                onRenewConfirm(device.imei, it)
                selectedDevice = null
            }
        )
    }
}

/* =========================================================
   DEVICE CARD (Responsive)
========================================================= */

@Composable
fun DevicePlanCard(
    device: DevicePlanUi,
    onRenew: () -> Unit,
    contentWidth: Dp
) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .widthIn(max = contentWidth)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column {

            // 🔹 Accent strip (modern look)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(Modifier.padding(18.dp)) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            device.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            "ID: ${device.imei}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    ExpiryChip(device.daysLeft)
                }

                Spacer(Modifier.height(16.dp))

                // Info Rows
                InfoRow("Plan", device.planType)
                InfoRow("Expiry Date", device.expiryDate)
                InfoRow(
                    "SIM Number",
                    device.simNumber?.takeIf { it.isNotBlank() } ?: "Not Available"
                )

                Spacer(Modifier.height(18.dp))

                // 🔹 Modern Button
                Button(
                    onClick = onRenew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    Text(
                        "Renew Plan",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/* =========================================================
   RESPONSIVE INFO ROW (KEY COMPONENT)
========================================================= */

@Composable
fun InfoRow(label: String, value: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {

        val isCompact = maxWidth < 360.dp

        if (isCompact) {
            // 📱 Small screens → stacked layout
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(label, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(2.dp))
                Text(value, fontWeight = FontWeight.SemiBold)
            }
        } else {
            // 📱 Normal & large screens → row layout
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    value,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
            }
        }
    }
}

/* =========================================================
   BOTTOM SHEET
========================================================= */

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
                text = "Renew: ${device.deviceName}",
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

/* =========================================================
   SMALL COMPONENTS
========================================================= */

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
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun ExpiryChip(daysLeft: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (daysLeft <= 7) Color(0xFFFFF8E1) else Color(0xFFE8F5E9)
    ) {
        Text(
            "$daysLeft days left",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp
        )
    }
}