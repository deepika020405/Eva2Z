package com.developer.ev2z.ui.screens.GeoFencing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.repository.GeoFenceRepository

private val LocalGeoFenceViewModel =
    staticCompositionLocalOf<GeoFenceViewModel> {
        error("GeoFenceViewModel not provided")
    }

@Composable
fun ProvideGeoFenceViewModel(
    apiService: ApiService,
    storageManager: StorageManager,
    content: @Composable () -> Unit
) {
    val repository = remember {
        GeoFenceRepository(apiService)
    }


    val viewModel = remember {
        GeoFenceViewModel(repository)
    }

    CompositionLocalProvider(
        LocalGeoFenceViewModel provides viewModel
    ) {
        content()
    }
}

@Composable
fun rememberGeoFenceViewModel(): GeoFenceViewModel {
    return LocalGeoFenceViewModel.current
}