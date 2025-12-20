package com.developer.ev2z.ui.screens.GeoFencing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.repository.GeoFenceRepository
import com.developer.ev2z.response.GeoFenceDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


sealed class GeoFenceUiState {
    object Loading : GeoFenceUiState()
    object Empty : GeoFenceUiState()
    data class Success(val list: List<GeoFenceDto>) : GeoFenceUiState()
    data class Error(val message: String) : GeoFenceUiState()
}

class GeoFenceViewModel(
    private val repository: GeoFenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GeoFenceUiState>(GeoFenceUiState.Loading)
    val uiState: StateFlow<GeoFenceUiState> = _uiState

    init {
        loadGeoFences()
    }

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
}