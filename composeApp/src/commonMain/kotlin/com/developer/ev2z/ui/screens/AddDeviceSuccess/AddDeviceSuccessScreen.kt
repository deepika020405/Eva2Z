// composeApp/src/commonMain/kotlin/com/developer/ev2z/ui/screens/AddDeviceSuccess/AddDeviceSuccessScreen.kt
package com.developer.ev2z.ui.screens.AddDeviceSuccess

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.developer.ev2z.theme.AppPageBg
import com.developer.ev2z.ui.components.screenHeightDp

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

    // Measured on the window so sizing tracks the device, not this composable's slot.
    val isCompact = screenHeightDp() < 700.dp
    val haloSize = if (isCompact) 96.dp else 120.dp
    val badgeSize = if (isCompact) 64.dp else 80.dp
    val sectionGap = if (isCompact) 20.dp else 32.dp

    // systemBarsPadding on the box keeps content clear of the iOS notch and home indicator
    // (Compose does not inset those automatically there) and leaves maxHeight bar-free.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(AppPageBg)
            .systemBarsPadding()
    ) {
        val horizontalPad = (maxWidth * 0.06f).coerceIn(16.dp, 32.dp)
        val verticalPad = if (isCompact) 16.dp else 24.dp
        // Captured here because ColumnScope shadows BoxWithConstraintsScope further in.
        val viewportHeight = maxHeight

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPad, vertical = verticalPad)
        ) {
            // Fills the viewport so the content still centres on tall screens, but is free to
            // grow past it — and scroll — when the card and button no longer fit.
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight - verticalPad * 2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(haloSize)
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(haloSize)
                            .clip(CircleShape)
                            .background(primaryContainerLight.copy(alpha = 0.1f))
                    )

                    Box(
                        modifier = Modifier
                            .size(badgeSize)
                            .clip(CircleShape)
                            .background(primaryContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        PlatformSuccessAnimation(scale)
                    }
                }

                Spacer(modifier = Modifier.height(sectionGap))

                Text(
                    text = "Congratulations!",
                    fontSize = if (isCompact) 24.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(scale)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Device Added Successfully",
                    fontSize = if (isCompact) 18.sp else 20.sp,
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

                Spacer(modifier = Modifier.height(sectionGap))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        Modifier.padding(if (isCompact) 16.dp else 20.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
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

                // A weighted spacer measures to zero inside a vertical scroll, so the gap is fixed.
                Spacer(modifier = Modifier.height(sectionGap))

                Button(
                    onClick = { onNavigate("home") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 50.dp else 56.dp)
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
                        fontSize = if (isCompact) 15.sp else 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.38f)
        )
        // Weighted so a long value wraps within its own share of the row. Unbounded, a
        // 21-character SIM number squeezed the label off the edge on narrow screens.
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.62f)
        )
    }
}

@Composable
expect fun PlatformSuccessAnimation(scale: Float)