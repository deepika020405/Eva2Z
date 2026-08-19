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
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.theme.AppPageBg

/* =========================================================
   RESPONSIVE HELPERS
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
                title = {
                    Text(
                        "Plans & Subscription",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF222222)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            "Back",
                            tint = Color(0xFF333333)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppPageBg
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AppPageBg)
        ) {
            val pad = responsivePadding(maxWidth)
            val contentWidth = responsiveContentWidth(maxWidth)
            val isCompact = maxHeight < 700.dp

            if (devices.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    Color(0xFFF0F7EC),
                                    RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Devices,
                                contentDescription = null,
                                tint = Color(0xFF6A9F50),
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "No devices found",
                            fontSize = if (isCompact) 17.sp else 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222)
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Add a device to start using plans & subscriptions.",
                            color = Color(0xFF888888),
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = pad),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(devices) { device ->
                        DevicePlanCard(
                            device = device,
                            onRenew = { selectedDevice = device },
                            contentWidth = contentWidth,
                            isCompact = isCompact
                        )
                    }
                }
            }
        }
    }

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
   DEVICE CARD
========================================================= */

@Composable
fun DevicePlanCard(
    device: DevicePlanUi,
    onRenew: () -> Unit,
    contentWidth: Dp,
    isCompact: Boolean = false
) {
    val cardPad = if (isCompact) 16.dp else 18.dp
    val titleSize = if (isCompact) 15.sp else 16.sp
    val labelSize = if (isCompact) 13.sp else 14.sp

    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .widthIn(max = contentWidth)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Green accent strip
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF6A9F50), Color(0xFF8BC34A))
                        )
                    )
            )

            Column(Modifier.padding(cardPad)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            device.deviceName,
                            fontSize = titleSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF222222)
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            "ID: ${device.imei}",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    ExpiryChip(device.daysLeft)
                }

                Spacer(Modifier.height(16.dp))

                // Info Rows
                InfoRow("Plan", device.planType, labelSize)
                InfoRow("Expiry Date", device.expiryDate, labelSize)
                InfoRow(
                    "SIM Number",
                    device.simNumber?.takeIf { it.isNotBlank() } ?: "Not Available",
                    labelSize
                )

                Spacer(Modifier.height(18.dp))

                // Renew Button
                Button(
                    onClick = onRenew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 46.dp else 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A9F50),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    )
                ) {
                    Text(
                        "Renew Plan",
                        fontSize = if (isCompact) 14.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/* =========================================================
   INFO ROW
========================================================= */

@Composable
fun InfoRow(
    label: String,
    value: String,
    fontSize: TextUnit = 14.sp
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isNarrow = maxWidth < 360.dp

        if (isNarrow) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(label, color = Color(0xFF999999), fontSize = (fontSize.value - 1).sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    value,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize,
                    color = Color(0xFF333333)
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    color = Color(0xFF999999),
                    fontSize = fontSize,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    value,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize,
                    color = Color(0xFF333333),
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Renew: ${device.deviceName}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF222222)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Select a plan duration to continue",
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )

            Spacer(Modifier.height(20.dp))

            DurationOption(
                text = "1 Year",
                selected = true,
                onClick = { duration = "1 Year" }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onConfirm(duration) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A9F50),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    "Proceed to Payment",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))
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
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFF0F7EC) else Color.White
        ),
        border = if (selected) BorderStroke(1.5.dp, Color(0xFF6A9F50)) else BorderStroke(1.dp, Color(0xFFE8E8E8)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                modifier = Modifier.weight(1f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFF333333) else Color(0xFF777777),
                fontSize = 15.sp
            )
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color(0xFF6A9F50),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ExpiryChip(daysLeft: Int) {
    val isExpiringSoon = daysLeft <= 7
    val bgColor = if (isExpiringSoon) Color(0xFFFFF3E0) else Color(0xFFF0F7EC)
    val textColor = if (isExpiringSoon) Color(0xFFE65100) else Color(0xFF6A9F50)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            "$daysLeft days left",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}