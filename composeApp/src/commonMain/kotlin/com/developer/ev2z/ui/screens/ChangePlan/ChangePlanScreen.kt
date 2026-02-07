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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlanScreen(
    devices: List<DevicePlanUi>,
    onBack: () -> Unit,
    onRenew: (DevicePlanUi) -> Unit,
    onChangePlan: (DevicePlanUi) -> Unit
) {

    // 🔥 NEW STATES
    var selectedDevice by remember { mutableStateOf<DevicePlanUi?>(null) }
    var showRenewSheet by remember { mutableStateOf(false) }
    var showChangePlanSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plans & Subscription", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF6F6F6)
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 🔹 SUMMARY CARD (always visible)
            item {
                PlanSummaryCard(devices)
            }

            // 🔹 EMPTY STATE (CMP safe)
            if (devices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active plans found",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 🔹 DEVICE LIST
            items(
                items = devices,
                key = { it.deviceId }   // ✅ recommended for CMP
            ) { device ->
                DevicePlanCard(
                    device = device,
                    onRenew = {
                        selectedDevice = device
                        showRenewSheet = true
                    },
                    onChangePlan = {
                        selectedDevice = device
                        showChangePlanSheet = true
                    }
                )
            }
        }
    }

    // 🔥 RENEW BOTTOM SHEET
    if (showRenewSheet && selectedDevice != null) {
        RenewPlanBottomSheet(
            device = selectedDevice!!,
            onDismiss = { showRenewSheet = false },
            onConfirm = { duration, autoPay ->
                println("Renew ${selectedDevice!!.deviceId} for $duration | AutoPay=$autoPay")
                onRenew(selectedDevice!!)
                showRenewSheet = false
            }
        )
    }

    // 🔥 CHANGE PLAN BOTTOM SHEET
    if (showChangePlanSheet && selectedDevice != null) {
        ChangePlanBottomSheet(
            device = selectedDevice!!,
            onDismiss = { showChangePlanSheet = false },
            onConfirm = { newPlan ->
                println("Change ${selectedDevice!!.deviceId} to $newPlan")
                onChangePlan(selectedDevice!!)
                showChangePlanSheet = false
            }
        )
    }
}

/* ---------------------------------------------------
   SUMMARY CARD
--------------------------------------------------- */

@Composable
fun PlanSummaryCard(devices: List<DevicePlanUi>) {
    val expiringSoon = devices.count { it.daysLeft <= 7 }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem("Total Devices", devices.size.toString())
            SummaryItem("Expiring Soon", expiringSoon.toString(), highlight = true)
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    highlight: Boolean = false
) {
    Column {
        Text(title, fontSize = 12.sp, color = Color.Gray)
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) Color(0xFFE53935) else Color.Black
        )
    }
}

/* ---------------------------------------------------
   DEVICE CARD
--------------------------------------------------- */

@Composable
fun DevicePlanCard(
    device: DevicePlanUi,
    onRenew: () -> Unit,
    onChangePlan: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(device.deviceName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(device.deviceId, fontSize = 12.sp, color = Color.Gray)
                }
                ExpiryChip(device.daysLeft)
            }

            Spacer(Modifier.height(12.dp))
            InfoRow("Plan", device.planType)
            InfoRow("Expiry Date", device.expiryDate)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onChangePlan, modifier = Modifier.weight(1f)) {
                    Text("Change Plan")
                }
                Button(
                    onClick = onRenew,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Renew")
                }
            }
        }
    }
}

/* ---------------------------------------------------
   BOTTOM SHEETS
--------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewPlanBottomSheet(
    device: DevicePlanUi,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var selectedDuration by remember { mutableStateOf("1 Year") }
    var autoPay by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {

            Text("Renew Plan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(device.deviceName, color = Color.Gray)

            Spacer(Modifier.height(16.dp))
            Text("Select Duration", fontWeight = FontWeight.SemiBold)

            listOf("3 Months", "6 Months", "1 Year").forEach {
                DurationOption(it, selectedDuration == it) {
                    selectedDuration = it
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Auto-Pay", Modifier.weight(1f))
                Switch(checked = autoPay, onCheckedChange = { autoPay = it })
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onConfirm(selectedDuration, autoPay) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Proceed to Pay")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePlanBottomSheet(
    device: DevicePlanUi,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf(device.planType) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp)) {

            Text("Change Plan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(device.deviceName, color = Color.Gray)

            Spacer(Modifier.height(16.dp))

            listOf("Wired Plan", "Wireless Plan").forEach { plan ->
                DurationOption(plan, selectedPlan == plan) {
                    selectedPlan = plan
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onConfirm(selectedPlan) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Change")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ---------------------------------------------------
   SHARED UI
--------------------------------------------------- */

@Composable
fun DurationOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        border = if (selected) BorderStroke(1.dp, Color(0xFF4CAF50)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
    val (bg, text) = when {
        daysLeft <= 3 -> Color(0xFFFFEBEE) to Color(0xFFE53935)
        daysLeft <= 7 -> Color(0xFFFFF8E1) to Color(0xFFF9A825)
        else -> Color(0xFFE8F5E9) to Color(0xFF4CAF50)
    }

    Surface(shape = RoundedCornerShape(50), color = bg) {
        Text(
            "$daysLeft days left",
            Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = text
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}