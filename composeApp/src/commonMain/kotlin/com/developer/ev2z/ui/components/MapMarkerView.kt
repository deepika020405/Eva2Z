// File Location: commonMain/ui/components/MapMarkerView.kt
package com.developer.ev2z.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developer.ev2z.data.local.model.Device
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import eva2z_connect_multiplatform.composeapp.generated.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview()
fun MapMarkerView(device: Device) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Info Card - Name aur Speed
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = device.getDisplayName(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
                Text(
                    text = "${device.speed ?: 0} kmh",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vehicle Icon with Status Color
        val (iconRes, iconColor) = getVehicleIconAndColor(device.status)

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = device.status,
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }
}

// Helper function to get icon and color based on status
@Composable
private fun getVehicleIconAndColor(status: String?): Pair<DrawableResource, Color> {
    return when (status?.lowercase()) {
        "moving", "running" -> Pair(
            Res.drawable.ic_vehicle_running,
            Color(0xFF4CAF50) // Green
        )
        "idle", "ideal" -> Pair(
            Res.drawable.ic_vehicle_idle,
            Color(0xFFFFC107) // Yellow
        )
        "stop", "stopped", "parked" -> Pair(
            Res.drawable.ic_vehicle_stop,
            Color(0xFFF44336) // Red
        )
        "offline" -> Pair(
            Res.drawable.ic_vehicle_offline,
            Color(0xFF9E9E9E) // Gray
        )
        else -> Pair(
            Res.drawable.ic_vehicle_offline,
            Color(0xFF9E9E9E)
        )
    }
}