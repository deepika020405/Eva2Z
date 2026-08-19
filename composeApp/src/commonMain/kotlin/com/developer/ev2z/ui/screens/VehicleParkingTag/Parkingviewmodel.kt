package com.developer.ev2z.ui.screens.VehicleParkingTag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.ui.components.CountryData
import com.developer.ev2z.ui.components.defaultCountry
import com.developer.ev2z.util.ApiProvider
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

    private val _phoneCountry = MutableStateFlow(defaultCountry())
    val phoneCountry: StateFlow<CountryData> = _phoneCountry.asStateFlow()

    private val _alternatePhoneCountry = MutableStateFlow(defaultCountry())
    val alternatePhoneCountry: StateFlow<CountryData> = _alternatePhoneCountry.asStateFlow()

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
        get() {
            val phone    = _phone.value
            val altPhone = _alternatePhone.value
            val regPhone = _registeredPhone.value
            val phoneLen = _phoneCountry.value.phoneLength
            val altLen   = _alternatePhoneCountry.value.phoneLength
            val vNum = _vehicleNumber.value
            return _vehicleType.value.isNotBlank()
                && vNum.length >= 4
                && vNum.any { it.isLetter() }
                && vNum.any { it.isDigit() }
                && phone.length == phoneLen
                && (altPhone.isEmpty() || altPhone.length == altLen)
                && (regPhone.isBlank() || phone.takeLast(10) != regPhone)
                && (regPhone.isBlank() || altPhone.isEmpty() || altPhone.takeLast(10) != regPhone)
                && (altPhone.isEmpty() || phone != altPhone)
        }

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
        val maxLen   = _phoneCountry.value.phoneLength
        val digits   = value.filter { it.isDigit() }.take(maxLen)
        val regPhone = _registeredPhone.value
        _phone.value = digits
        _phoneError.value = when {
            digits.isEmpty()                                                      -> null
            regPhone.isNotBlank() && digits.takeLast(10) == regPhone              -> "Cannot use your registered number"
            _alternatePhone.value.isNotBlank() && digits == _alternatePhone.value -> "Both emergency numbers cannot be the same"
            else                                                                  -> null
        }
        if (_alternatePhoneError.value == "Both emergency numbers cannot be the same" && digits != _alternatePhone.value) {
            _alternatePhoneError.value = null
        }
        clearErrorState()
    }

    fun onAlternatePhoneChange(value: String) {
        val maxLen   = _alternatePhoneCountry.value.phoneLength
        val digits   = value.filter { it.isDigit() }.take(maxLen)
        val regPhone = _registeredPhone.value
        _alternatePhone.value = digits
        _alternatePhoneError.value = when {
            digits.isEmpty()                                             -> null
            digits == _phone.value                                       -> "Both emergency numbers cannot be the same"
            regPhone.isNotBlank() && digits.takeLast(10) == regPhone     -> "Cannot use your registered number"
            else                                                         -> null
        }
        if (_phoneError.value == "Both emergency numbers cannot be the same" && digits != _phone.value) {
            _phoneError.value = null
        }
        clearErrorState()
    }

    fun onPhoneCountryChange(country: CountryData) {
        _phoneCountry.value = country
        if (_phone.value.length > country.phoneLength) _phone.value = _phone.value.take(country.phoneLength)
        _phoneError.value = null
    }

    fun onAlternatePhoneCountryChange(country: CountryData) {
        _alternatePhoneCountry.value = country
        if (_alternatePhone.value.length > country.phoneLength) _alternatePhone.value = _alternatePhone.value.take(country.phoneLength)
        _alternatePhoneError.value = null
    }

    fun selectVehicleType(type: String) {
        _vehicleType.value      = type
        _vehicleTypeError.value = null
    }

    // ── Submit ────────────────────────────────────────────────────

    fun submitParking(
        promoCode: String = ""
    ) {
        if (_uiState.value is ParkingUiState.Loading) return

        var valid = true

        if (_vehicleType.value.isBlank()) {
            _vehicleTypeError.value = "Vehicle type is required"
            valid = false
        }
        val vNum = _vehicleNumber.value
        if (vNum.isBlank()) {
            _vehicleNumberError.value = "Vehicle number is required"
            valid = false
        } else if (vNum.length < 4) {
            _vehicleNumberError.value = "Enter a valid vehicle number (min 4 characters)"
            valid = false
        } else if (!vNum.any { it.isLetter() } || !vNum.any { it.isDigit() }) {
            _vehicleNumberError.value = "Vehicle number must contain both letters and digits (e.g. DL01AB1234)"
            valid = false
        }
        val phoneLen = _phoneCountry.value.phoneLength
        val altLen   = _alternatePhoneCountry.value.phoneLength
        if (_phone.value.length != phoneLen) {
            _phoneError.value = "Enter a valid ${phoneLen}-digit number"
            valid = false
        }
        if (_alternatePhone.value.isNotBlank() && _alternatePhone.value.length != altLen) {
            _alternatePhoneError.value = "Enter a valid ${altLen}-digit number"
            valid = false
        }

        // Same number check (only when alternate phone is provided)
        if (
            _phone.value.length in 7..15 &&
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
            if (_alternatePhone.value.isNotBlank() && enteredAlt == registeredPhone) {
                _alternatePhoneError.value = "Cannot use your registered number"
                valid = false
            }
        }

        if (!valid) return

        viewModelScope.launch {
            _uiState.value = ParkingUiState.Loading

            val response = apiService.parkVehicle(
                vehicleType     = _vehicleType.value,
                vehicleNumber   = _vehicleNumber.value,
                phone           = _registeredPhone.value,
                alternatePhone1 = "${_phoneCountry.value.dialCode.filter { it.isDigit() }}${_phone.value}",
                alternatePhone2 = _alternatePhone.value.ifEmpty { null }
                    ?.let { "${_alternatePhoneCountry.value.dialCode.filter { it.isDigit() }}$it" },
                countryName     = _phoneCountry.value.name,
                countryCode     = _phoneCountry.value.dialCode,
                referralCode    = promoCode.ifEmpty { null }
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
                        response.errors?.vehicle_type?.firstOrNull()?.let      { _vehicleTypeError.value = it }
                        response.errors?.vehicle_number?.firstOrNull()?.let    { _vehicleNumberError.value = it }
                        (response.errors?.alternate_phone_1 ?: response.errors?.phone)
                            ?.firstOrNull()?.let { _phoneError.value = it }
                        (response.errors?.alternate_phone_2 ?: response.errors?.alternate_phone)
                            ?.firstOrNull()?.let { _alternatePhoneError.value = it }
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
        _vehicleType.value           = ""
        _vehicleNumber.value         = ""
        _phone.value                 = ""
        _alternatePhone.value        = ""
        _phoneCountry.value          = defaultCountry()
        _alternatePhoneCountry.value = defaultCountry()
        _vehicleTypeError.value      = null
        _vehicleNumberError.value    = null
        _phoneError.value            = null
        _alternatePhoneError.value   = null
        _uiState.value               = ParkingUiState.Idle
    }

    private fun clearErrorState() {
        if (_uiState.value is ParkingUiState.Error) _uiState.value = ParkingUiState.Idle
    }

    // ── clearFormFields — sirf fields clear, uiState preserve ────────
    fun clearFormFields() {
        _vehicleType.value           = ""
        _vehicleNumber.value         = ""
        _phone.value                 = ""
        _alternatePhone.value        = ""
        _phoneCountry.value          = defaultCountry()
        _alternatePhoneCountry.value = defaultCountry()
        _vehicleTypeError.value      = null
        _vehicleNumberError.value    = null
        _phoneError.value            = null
        _alternatePhoneError.value   = null
    }


    // ── Update Vehicle ────────────────────────────────────────────
    fun updateVehicle(
        vehicleId: Int,
        vehicleType: String,
        vehicleNumber: String,
        phone: String,
        alternatePhone1: String?,
        alternatePhone2: String?,
        countryName: String,
        countryCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.updateVehicle(
                    vehicleId       = vehicleId,
                    vehicleType     = vehicleType,
                    vehicleNumber   = vehicleNumber,
                    phone           = phone,
                    alternatePhone1 = alternatePhone1,
                    alternatePhone2 = alternatePhone2,
                    countryName     = countryName,
                    countryCode     = countryCode
                )
                if (response.status) {
                    response.data?.let { updated ->
                        _history.value = _history.value.map { if (it.id == vehicleId) updated else it }
                    }
                    onSuccess()
                } else {
                    onError(response.message ?: "Update failed")
                }
            } catch (e: Exception) {
                println("❌ updateVehicle: ${e.message}")
                onError(e.message ?: "Something went wrong")
            }
        }
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


