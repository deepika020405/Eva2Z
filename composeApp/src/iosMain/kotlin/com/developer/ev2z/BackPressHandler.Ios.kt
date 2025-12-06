package com.developer.ev2z

import androidx.compose.runtime.Composable

@Composable
actual fun BackPressHandler(onBack: () -> Unit) {
    // iOS has swipe back gestures controlled by navigator, so no-op
}