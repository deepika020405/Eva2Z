// composeApp/src/iosMain/kotlin/com/developer/ev2z/ui/screens/AddDeviceSuccess/AddDeviceSuccessScreen.ios.kt
package com.developer.ev2z.ui.screens.AddDeviceSuccess

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
actual fun PlatformSuccessAnimation(scale: Float) {
    // Simple checkmark with scale
    Text(
        text = "✓",
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}