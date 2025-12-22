package com.developer.ev2z.ui.screens.GeoFencing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.NominatimApi
import com.developer.ev2z.repository.GeoFenceRepository
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.GeoFenceDto
import com.developer.ev2z.util.AppTokenProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed class GeoFenceUiState {
    object Loading : GeoFenceUiState()
    object Empty : GeoFenceUiState()
    data class Success(val list: List<GeoFenceDto>) : GeoFenceUiState()
    data class Error(val message: String) : GeoFenceUiState()
}

sealed class DeviceUiState {
    object Idle : DeviceUiState()
    object Loading : DeviceUiState()
    data class Success(val list: List<Device>) : DeviceUiState()
    data class Error(val msg: String) : DeviceUiState()
}

class GeoFenceViewModel(
    private val repository: GeoFenceRepository
) : ViewModel() {

    val token = AppTokenProvider.getToken()
    private val _uiState = MutableStateFlow<GeoFenceUiState>(GeoFenceUiState.Loading)
    val uiState: StateFlow<GeoFenceUiState> = _uiState

    // ---------- DEVICE STATE ----------
    private val _deviceState =
        MutableStateFlow<DeviceUiState>(DeviceUiState.Idle)

    private val _results = MutableStateFlow<List<LocationResult>>(emptyList())
    val results: StateFlow<List<LocationResult>> = _results

    private val _selected = MutableStateFlow<LocationResult?>(null)
    val selected: StateFlow<LocationResult?> = _selected
    val deviceState: StateFlow<DeviceUiState> = _deviceState
    init {
        loadGeoFences()
    }
    private var searchJob: Job? = null
    val _isSearching = MutableStateFlow(false)
    fun loadGeoFences() {
        viewModelScope.launch {
            _uiState.value = GeoFenceUiState.Loading

            repository.fetchGeoFences()
                .onSuccess {
                    _uiState.value =
                        if (it.isEmpty()) GeoFenceUiState.Empty
                        else GeoFenceUiState.Success(it)
                }
                .onFailure {
                    _uiState.value =
                        GeoFenceUiState.Error(it.message ?: "Something went wrong")
                }
        }
    }



    fun loadDevices() {
        viewModelScope.launch {
            _deviceState.value = DeviceUiState.Loading

            repository.fetchMyDevices(token)
                .onSuccess {
                    _deviceState.value = DeviceUiState.Success(it)
                }
                .onFailure {
                    _deviceState.value =
                        DeviceUiState.Error(it.message ?: "Device load failed")
                }
        }
    }






    fun searchLocation(query: String) {
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)
            _results.value = NominatimApi.search(query)
            _isSearching.value = false
        }
    }

    fun selectLocation(location: LocationResult) {
        _selected.value = location
    }

}