// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/AddDeviceSuccess/AddDeviceSuccessScreen.kt
package com.developer.ev2z.ui.screens.AddDeviceSuccess

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val primaryContainerLight = Color(0xFF00C853)

// ✅ Added deviceType parameter
@Composable
fun AddDeviceSuccessScreen(
    onNavigate: (String) -> Unit,
    deviceName: String,
    deviceId: String,
    simNumber: String,
    deviceType: String // ✅ New parameter
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(primaryContainerLight.copy(alpha = 0.1f))
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(primaryContainerLight),
                contentAlignment = Alignment.Center
            ) {
                PlatformSuccessAnimation(scale)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Congratulations!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(scale)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Device Added Successfully",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(scale)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your Device will be activated within 24 hours",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(scale)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DeviceInfoRow("Device Name", deviceName)
                Divider(color = Color.LightGray, thickness = 1.dp)
                DeviceInfoRow("Device ID", deviceId)
                Divider(color = Color.LightGray, thickness = 1.dp)
                DeviceInfoRow("SIM Number", simNumber)
                Divider(color = Color.LightGray, thickness = 1.dp)
                // ✅ Show device type
                DeviceInfoRow("Device Type", deviceType.replaceFirstChar { it.uppercase() })
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onNavigate("home") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(scale),
            colors = ButtonDefaults.buttonColors(containerColor = primaryContainerLight),
            shape = RoundedCornerShape(100.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Home,
                contentDescription = "Home",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Dashboard",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
expect fun PlatformSuccessAnimation(scale: Float)