package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService

@Composable
fun GeoFencingScreenWrapper(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit
) {
    var showAddGeoFence by remember { mutableStateOf(false) }

    ProvideGeoFenceViewModel(
        apiService = apiService,
        storageManager = storageManager
    ) {

        if (showAddGeoFence) {
            // 👉 Add GeoFence Screen
            AddGeoFenceScreen(
                onBack = { showAddGeoFence = false },
                onSave = {
                    // save api call later
                    showAddGeoFence = false
                }
            )
        } else {
            // 👉 GeoFence Listing Screen
            GeoFencingScreen(
                onAddGeoFence = {
                    showAddGeoFence = true
                },
                onBackPressed = onBackPressed
            )
        }
    }
}