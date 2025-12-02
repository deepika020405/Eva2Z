package com.developer.ev2z.ui.screens.homeScreen

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen() {
    PlatformMapView(
        modifier = Modifier.fillMaxSize(),
        latitude = 28.6139,
        longitude = 77.2090,
        zoom = 12.0
    )
}