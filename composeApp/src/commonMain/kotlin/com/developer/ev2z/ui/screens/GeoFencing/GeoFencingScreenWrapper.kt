package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.runtime.Composable
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService

@Composable
fun GeoFencingScreenWrapper(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit
) {
    ProvideGeoFenceViewModel(
        apiService = apiService,
        storageManager = storageManager
    ) {
        GeoFencingScreen(
            onAddGeoFence = { /* later */ },
            onBackPressed = onBackPressed
        )
    }
}