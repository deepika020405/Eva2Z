package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.navigation.PlatformBackHandler
import com.developer.ev2z.response.GeoFenceDto

@Composable
fun GeoFencingScreenWrapper(
    apiService: ApiService,
    storageManager: StorageManager,
    onBackPressed: () -> Unit
) {
    PlatformBackHandler(enabled = true) { onBackPressed() }

    var showAddGeoFence by remember { mutableStateOf(false) }
    var editingGeoFence by remember { mutableStateOf<GeoFenceDto?>(null) }

    ProvideGeoFenceViewModel(
        apiService = apiService,
        storageManager = storageManager
    ) {

        when {
            // ✏️ EDIT MODE
            editingGeoFence != null -> {
                AddEditGeoFenceScreen(
                    editingGeoFence = editingGeoFence,
                    onBack = {
                        editingGeoFence = null   // 🔥 back to list
                    }
                )
            }

            // ➕ ADD MODE
            showAddGeoFence -> {
                AddEditGeoFenceScreen(
                    editingGeoFence = null,
                    onBack = {
                        showAddGeoFence = false
                    }
                )
            }

            // 📃 LIST MODE
            else -> {
                GeoFencingScreen(
                    onAddGeoFence = {
                        showAddGeoFence = true
                    },
                    onBackPressed = onBackPressed,
                    onEditGeoFence = { geoFence ->
                        editingGeoFence = geoFence   // 🔥 THIS WAS MISSING
                    }
                )
            }
        }
    }
}