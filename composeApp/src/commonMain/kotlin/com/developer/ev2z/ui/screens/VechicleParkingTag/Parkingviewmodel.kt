package com.developer.ev2z.ui.screens.VechicleParkingTag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.util.ApiProvider
import com.developer.ev2z.util.ApiProvider.apiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

class ParkingViewModel(
    private val apiService: ApiService,
    private val userPrefs: UserPrefs
) : ViewModel() {

    // ── Form fields ───────────────────────────────────────────────

    private val _vehicleType = MutableStateFlow("")
    val vehicleType: StateFlow<String> = _vehicleType.asStateFlow()

    private val _vehicleNumber = MutableStateFlow("")
    val vehicleNumber: StateFlow<String> = _vehicleNumber.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _alternatePhone = MutableStateFlow("")
    val alternatePhone: StateFlow<String> = _alternatePhone.asStateFlow()

    // ── Registered phone ──────────────────────────────────────────
    private val _registeredPhone = MutableStateFlow("")
    val registeredPhone: StateFlow<String> = _registeredPhone.asStateFlow()

    // ── init ──────────────────────────────────────────────────────
    init {
        val phone = userPrefs.getUserPhone()
            ?.filter { it.isDigit() }
            ?.takeLast(10) ?: ""
        _registeredPhone.value = phone
    }

    // ── Field errors ──────────────────────────────────────────────

    private val _vehicleTypeError = MutableStateFlow<String?>(null)
    val vehicleTypeError: StateFlow<String?> = _vehicleTypeError.asStateFlow()

    private val _vehicleNumberError = MutableStateFlow<String?>(null)
    val vehicleNumberError: StateFlow<String?> = _vehicleNumberError.asStateFlow()

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError.asStateFlow()

    private val _alternatePhoneError = MutableStateFlow<String?>(null)
    val alternatePhoneError: StateFlow<String?> = _alternatePhoneError.asStateFlow()

    // ── UI State & History ────────────────────────────────────────

    private val _uiState = MutableStateFlow<ParkingUiState>(ParkingUiState.Idle)
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<ParkingData>>(emptyList())
    val history: StateFlow<List<ParkingData>> = _history.asStateFlow()

    val isFormValid: Boolean
        get() = _vehicleType.value.isNotBlank()
                && _vehicleNumber.value.isNotBlank()
                && _phone.value.length == 10

    // ── Input handlers ────────────────────────────────────────────

    fun onVehicleTypeChange(value: String) {
        _vehicleType.value      = value
        _vehicleTypeError.value = null
        clearErrorState()
    }

    fun onVehicleNumberChange(value: String) {
        _vehicleNumber.value      = value
        _vehicleNumberError.value = null
        clearErrorState()
    }

    fun onPhoneChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(10)
        _phone.value = digits
        _phoneError.value = when {
            digits.isEmpty()    -> null
            digits.length != 10 -> "Enter a valid 10-digit number"
            else                -> null
        }
        clearErrorState()
    }

    fun onAlternatePhoneChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(10)
        _alternatePhone.value = digits
        _alternatePhoneError.value = when {
            digits.isEmpty()    -> null
            digits.length != 10 -> "Enter a valid 10-digit number"
            else                -> null
        }
        clearErrorState()
    }

    fun selectVehicleType(type: String) {
        _vehicleType.value      = type
        _vehicleTypeError.value = null
    }

    // ── Submit ────────────────────────────────────────────────────

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
        if (_phone.value.length != 10) {
            _phoneError.value = "Enter a valid 10-digit mobile number"
            valid = false
        }
        if (_alternatePhone.value.isNotBlank() && _alternatePhone.value.length != 10) {
            _alternatePhoneError.value = "Enter a valid 10-digit number"
            valid = false
        }

        // ✅ Same number check
        if (
            _phone.value.length == 10 &&
            _alternatePhone.value.isNotBlank() &&
            _phone.value == _alternatePhone.value
        ) {
            _alternatePhoneError.value = "Both phone numbers cannot be same"
            valid = false
        }

        // ✅ Registered number check
        val registeredPhone = _registeredPhone.value

        if (registeredPhone.isNotBlank()) {
            val enteredPhone = _phone.value.filter { it.isDigit() }.takeLast(10)
            val enteredAlt   = _alternatePhone.value.filter { it.isDigit() }.takeLast(10)

            if (enteredPhone == registeredPhone) {
                _phoneError.value = "Cannot use your registered number"
                valid = false
            }
            if (enteredAlt.isNotBlank() && enteredAlt == registeredPhone) {
                _alternatePhoneError.value = "Cannot use your registered number"
                valid = false
            }
        }

        if (!valid) return

        viewModelScope.launch {
            _uiState.value = ParkingUiState.Loading

            val response = apiService.parkVehicle(
                vehicleType    = _vehicleType.value,
                vehicleNumber  = _vehicleNumber.value,
                phone          = _phone.value,
                alternatePhone = _alternatePhone.value.ifBlank { null }
            )

            if (!response.status) {
                val msg = response.message ?: "Something went wrong."

                when {
                    msg.contains("same", ignoreCase = true) &&
                            msg.contains("phone", ignoreCase = true) &&
                            !msg.contains("registered", ignoreCase = true) -> {
                        _alternatePhoneError.value = "Both phone numbers cannot be same"
                    }
                    msg.contains("registered", ignoreCase = true) -> {
                        if (_phone.value == registeredPhone) {
                            _phoneError.value = "Cannot use your registered number"
                        } else {
                            _alternatePhoneError.value = "Cannot use your registered number"
                        }
                    }
                    else -> {
                        response.errors?.vehicle_type?.firstOrNull()?.let   { _vehicleTypeError.value = it }
                        response.errors?.vehicle_number?.firstOrNull()?.let  { _vehicleNumberError.value = it }
                        response.errors?.phone?.firstOrNull()?.let           { _phoneError.value = it }
                        response.errors?.alternate_phone?.firstOrNull()?.let { _alternatePhoneError.value = it }
                    }
                }

                _uiState.value = ParkingUiState.Error(
                    message     = msg,
                    fieldErrors = response.errors
                )
            } else {
                response.data?.let { newEntry ->
                    _history.value = listOf(newEntry) + _history.value
                    userPrefs.saveParkingEntry(newEntry)
                }
                _uiState.value = ParkingUiState.Success(response)
            }
        }
    }

    // ── History — local + API ─────────────────────────────────────
    fun loadHistory() {
        // Pehle local dikhao (instant)
        val saved = userPrefs.getParkingHistory()
        if (saved.isNotEmpty()) _history.value = saved

        // API se fresh data fetch karo
        viewModelScope.launch {
            try {
                val response = apiService.getUserParkingDetails()
                if (response.status && !response.data.isNullOrEmpty()) {
                    _history.value = response.data
                    println("✅ Loaded ${response.data.size} parking records from API")
                }
            } catch (e: Exception) {
                println("❌ loadHistory API failed: ${e.message}")
                // Local data already shown — no crash
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    fun resetState() { _uiState.value = ParkingUiState.Idle }

    fun clearForm() {
        _vehicleType.value         = ""
        _vehicleNumber.value       = ""
        _phone.value               = ""
        _alternatePhone.value      = ""
        _vehicleTypeError.value    = null
        _vehicleNumberError.value  = null
        _phoneError.value          = null
        _alternatePhoneError.value = null
        _uiState.value             = ParkingUiState.Idle
    }

    private fun clearErrorState() {
        if (_uiState.value is ParkingUiState.Error) _uiState.value = ParkingUiState.Idle
    }

    // ── clearFormFields — sirf fields clear, uiState preserve ────────
    fun clearFormFields() {
        _vehicleType.value         = ""
        _vehicleNumber.value       = ""
        _phone.value               = ""
        _alternatePhone.value      = ""
        _vehicleTypeError.value    = null
        _vehicleNumberError.value  = null
        _phoneError.value          = null
        _alternatePhoneError.value = null
    }


    // ── Delete Vehicle ────────────────────────────────────────────
    fun deleteVehicle(vehicleId: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = ApiProvider.apiService.deleteVehicle(vehicleId)
                val status   = response["status"]?.jsonPrimitive?.boolean ?: false
                if (status) {
                    // List se remove karo
                    _history.value = _history.value.filter { it.id != vehicleId }
                    println("✅ Vehicle $vehicleId deleted")
                    onSuccess()
                } else {
                    val msg = response["message"]?.jsonPrimitive?.content ?: "Delete failed"
                    onError(msg)
                }
            } catch (e: Exception) {
                println("❌ deleteVehicle: ${e.message}")
                onError(e.message ?: "Something went wrong")
            }
        }
    }
}


