package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.runtime.Composable
import com.developer.ev2z.data.local.model.GeoFencePolygon

@Composable
expect fun GeoFenceMap(
    polygon: GeoFencePolygon?
)