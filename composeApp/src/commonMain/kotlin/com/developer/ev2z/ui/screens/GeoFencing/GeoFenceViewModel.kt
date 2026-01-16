package com.developer.ev2z.ui.screens.GeoFencing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.model.GeoFencePolygon
import com.developer.ev2z.data.local.model.LatLng
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
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


sealed class GeoFenceUiState {
    object Loading : GeoFenceUiState()
    object Empty : GeoFenceUiState()
    object NoDevice : GeoFenceUiState()   // 🔥 NEW

    data class Success(val list: List<GeoFenceDto>) : GeoFenceUiState()
    data class Error(val message: String) : GeoFenceUiState()
}

sealed class DeviceUiState {
    object Idle : DeviceUiState()
    object Loading : DeviceUiState()
    data class Success(val list: List<Device>) : DeviceUiState()
    data class Error(val msg: String) : DeviceUiState()
}

sealed class SaveGeoFenceState {
    object Idle : SaveGeoFenceState()
    object Loading : SaveGeoFenceState()
    object Success : SaveGeoFenceState()
    data class Error(val msg: String) : SaveGeoFenceState()
}

sealed class DeleteGeoFenceState {
    object Idle : DeleteGeoFenceState()
    object Loading : DeleteGeoFenceState()
    object Success : DeleteGeoFenceState()
    data class Error(val msg: String) : DeleteGeoFenceState()
}

sealed class EditGeoFenceState {
    object Idle : EditGeoFenceState()
    object Loading : EditGeoFenceState()
    object Success : EditGeoFenceState()
    data class Error(val msg: String) : EditGeoFenceState()
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
//    init {
//        loadGeoFences()
//    }
    private var searchJob: Job? = null
    val _isSearching = MutableStateFlow(false)
    private val _polygon =
        MutableStateFlow<GeoFencePolygon?>(null)

    val polygon: StateFlow<GeoFencePolygon?> = _polygon

    private val _saveState =
        MutableStateFlow<SaveGeoFenceState>(SaveGeoFenceState.Idle)

    val saveState: StateFlow<SaveGeoFenceState> = _saveState

    private val _deleteState =
        MutableStateFlow<DeleteGeoFenceState>(DeleteGeoFenceState.Idle)
    val deleteState: StateFlow<DeleteGeoFenceState> = _deleteState

    private val _editState =
        MutableStateFlow<EditGeoFenceState>(EditGeoFenceState.Idle)
    val editState: StateFlow<EditGeoFenceState> = _editState

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

    fun loadGeoFencesSafely() {
        viewModelScope.launch {
            _uiState.value = GeoFenceUiState.Loading

            // 1️⃣ First check devices
            repository.fetchMyDevices(token)
                .onSuccess { devices ->

                    if (devices.isEmpty()) {
                        _uiState.value = GeoFenceUiState.NoDevice
                        return@launch   // ⛔ STOP HERE
                    }

                    // 2️⃣ Devices exist → load geofences
                    repository.fetchGeoFences()
                        .onSuccess { list ->
                            _uiState.value =
                                if (list.isEmpty())
                                    GeoFenceUiState.Empty
                                else
                                    GeoFenceUiState.Success(list)
                        }
                        .onFailure {
                            _uiState.value =
                                GeoFenceUiState.Error(it.message ?: "GeoFence load failed")
                        }
                }
                .onFailure {
                    _uiState.value =
                        GeoFenceUiState.Error(it.message ?: "Device load failed")
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
        _polygon.value = createCirclePolygon(
            centerLat = location.lat,
            centerLng = location.lng,
            radiusMeters = 1500.0
        )
    }

    private fun createCirclePolygon(
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double = 1000.0,
        points: Int = 32
    ): GeoFencePolygon {

        val earthRadius = 6371000.0

        val latRad = centerLat * PI / 180
        val lngRad = centerLng * PI / 180

        val polygonPoints: List<LatLng> = (0 until points).map { i ->

            val angle = 2 * PI * i / points

            val lat = asin(
                sin(latRad) * cos(radiusMeters / earthRadius) +
                        cos(latRad) * sin(radiusMeters / earthRadius) * cos(angle)
            )

            val lng = lngRad + atan2(
                sin(angle) * sin(radiusMeters / earthRadius) * cos(latRad),
                cos(radiusMeters / earthRadius) - sin(latRad) * sin(lat)
            )

            LatLng(
                lat = lat * 180 / PI,
                lng = lng * 180 / PI
            )
        }

        return GeoFencePolygon(polygonPoints)
    }


    fun saveGeoFence(
        name: String,
        device: Device,
        radiusMeters: Int,
        lat: String,
        lng: String
    ) {
        val location = selected.value ?: return

        viewModelScope.launch {
            _saveState.value = SaveGeoFenceState.Loading

            repository.saveGeoFence(
                token = token,
                deviceId = device.device_id,
                name = name,
                latitude = lat.toDouble(),
                longitude = lng.toDouble(),
                radius = radiusMeters
            ).onSuccess {
                _saveState.value = SaveGeoFenceState.Success
                loadGeoFences() // refresh list
            }.onFailure {
                _saveState.value =
                    SaveGeoFenceState.Error(it.message ?: "Failed to save geofence")
            }
        }
    }

        fun deleteGeoFence(item: GeoFenceDto) {
            viewModelScope.launch {
                _deleteState.value = DeleteGeoFenceState.Loading

                repository.deleteGeoFence(
                    geoFence = item,
                    token = token
                ).onSuccess {
                    _deleteState.value = DeleteGeoFenceState.Success
                    loadGeoFences() // 🔄 refresh list
                }.onFailure {
                    _deleteState.value =
                        DeleteGeoFenceState.Error(it.message ?: "Delete failed")
                }
            }
        }



//    fun updateGeoFence(
//        geoFence: GeoFenceDto,
//        name: String,
//        radius: Int
//    ) {
//        viewModelScope.launch {
//            _saveState.value = SaveGeoFenceState.Loading
//
//            repository.updateGeoFence(
//                geoFence = geoFence,
//                name = name,
//                radius = radius,
//                lat = geoFence.latitude,
//                lng = geoFence.longitude
//            ).onSuccess {
//                _saveState.value = SaveGeoFenceState.Success
//                loadGeoFences()
//            }.onFailure {
//                _saveState.value =
//                    SaveGeoFenceState.Error(it.message ?: "Update failed")
//            }
//        }
//    }

    fun updateGeoFence(
        geoFence: GeoFenceDto,
        name: String,
        radius: Int,
        lat: String,
        lng: String
    ) {
        viewModelScope.launch {
            _saveState.value = SaveGeoFenceState.Loading

            repository.updateGeoFence(
                geoFence = geoFence,
                name = name,
                radius = radius,
                lat = lat,          // ✅ NEW
                lng = lng           // ✅ NEW
            ).onSuccess {
                _saveState.value = SaveGeoFenceState.Success
                loadGeoFences()
            }.onFailure {
                _saveState.value =
                    SaveGeoFenceState.Error(it.message ?: "Update failed")
            }
        }
    }


    fun setEditLocation(lat: String, lng: String) {
        _polygon.value = createCirclePolygon(
            centerLat = lat.toDouble(),
            centerLng = lng.toDouble(),
            radiusMeters = 1500.0
        )
    }

    fun resetSaveState() {
        _saveState.value = SaveGeoFenceState.Idle
    }


    fun getDeviceById(deviceId: String): Device? {
        val state = deviceState.value
        return if (state is DeviceUiState.Success) {
            state.list.find { it.device_id == deviceId }
        } else null
    }

}