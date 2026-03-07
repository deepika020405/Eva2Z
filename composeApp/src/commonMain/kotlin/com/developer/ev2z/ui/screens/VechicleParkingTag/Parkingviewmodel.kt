package com.developer.ev2z.ui.screens.VechicleParkingTag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParkingViewModel(
    private val apiService: ApiService,
    private val userPrefs: UserPrefs
) : ViewModel() {

    private val _vehicleType   = MutableStateFlow("")
    val vehicleType: StateFlow<String> = _vehicleType.asStateFlow()

    private val _vehicleNumber = MutableStateFlow("")
    val vehicleNumber: StateFlow<String> = _vehicleNumber.asStateFlow()

    private val _vehicleTypeError   = MutableStateFlow<String?>(null)
    val vehicleTypeError: StateFlow<String?> = _vehicleTypeError.asStateFlow()

    private val _vehicleNumberError = MutableStateFlow<String?>(null)
    val vehicleNumberError: StateFlow<String?> = _vehicleNumberError.asStateFlow()

    private val _uiState = MutableStateFlow<ParkingUiState>(ParkingUiState.Idle)
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<ParkingData>>(emptyList())
    val history: StateFlow<List<ParkingData>> = _history.asStateFlow()

    val isFormValid: Boolean
        get() = _vehicleType.value.isNotBlank() && _vehicleNumber.value.isNotBlank()

    fun onVehicleTypeChange(value: String) {
        _vehicleType.value      = value
        _vehicleTypeError.value = null
        if (_uiState.value is ParkingUiState.Error) _uiState.value = ParkingUiState.Idle
    }

    fun onVehicleNumberChange(value: String) {
        _vehicleNumber.value      = value
        _vehicleNumberError.value = null
        if (_uiState.value is ParkingUiState.Error) _uiState.value = ParkingUiState.Idle
    }

    fun selectVehicleType(type: String) {
        _vehicleType.value      = type
        _vehicleTypeError.value = null
    }

    fun submitParking() {
        if (_uiState.value is ParkingUiState.Loading) return

        var valid = true
        if (_vehicleType.value.isBlank()) {
            _vehicleTypeError.value = "Vehicle type is required"
            valid = false
        }
        if (_vehicleNumber.value.isBlank()) {
            _vehicleNumberError.value = "Vehicle number is required"
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _uiState.value = ParkingUiState.Loading

            val response = apiService.parkVehicle(
                vehicleType   = _vehicleType.value,
                vehicleNumber = _vehicleNumber.value
            )

            if (!response.status) {
                response.errors?.vehicle_type?.firstOrNull()?.let {
                    _vehicleTypeError.value = it
                }
                response.errors?.vehicle_number?.firstOrNull()?.let {
                    _vehicleNumberError.value = it
                }
                _uiState.value = ParkingUiState.Error(
                    message     = response.message ?: "Something went wrong.",
                    fieldErrors = response.errors
                )
            } else {
                response.data?.let { newEntry ->
                    // 1. Update in-memory list immediately
                    _history.value = listOf(newEntry) + _history.value
                    // 2. Persist locally — survives app restart
                    userPrefs.saveParkingEntry(newEntry)
                }
                _uiState.value = ParkingUiState.Success(response)
            }
        }
    }

    // ✅ FIXED: Load from UserPrefs, NOT from API (no GET endpoint exists)
    fun loadHistory() {
        val saved = userPrefs.getParkingHistory()
        if (saved.isNotEmpty()) _history.value = saved
    }

    fun resetState() { _uiState.value = ParkingUiState.Idle }

    fun clearForm() {
        _vehicleType.value        = ""
        _vehicleNumber.value      = ""
        _vehicleTypeError.value   = null
        _vehicleNumberError.value = null
        _uiState.value            = ParkingUiState.Idle
    }


}