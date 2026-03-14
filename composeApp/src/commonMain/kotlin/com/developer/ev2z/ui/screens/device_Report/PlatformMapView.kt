package com.developer.ev2z.ui.screens.device_Report


import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.developer.ev2z.ui.screens.trackTrace.TrailPoint

@Composable
expect fun PlatformMapView(
    modifier: Modifier,
    latitude: Double,
    longitude: Double,
    zoom: Double,
    trailPoints: List<TrailPoint> = emptyList()
)