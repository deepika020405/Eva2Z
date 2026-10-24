package com.developer.ev2z.ui.screens.deviceStatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.repository.DeviceStatusRepository
import com.developer.ev2z.response.DeviceStatusResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.asLegs
import com.developer.ev2z.util.getAddressFromLatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine

class DeviceStatusViewModel(
    private val repo: DeviceStatusRepository,
    private val tokenProvider: () -> String
) : ViewModel() {

    private val _devices = MutableStateFlow<MyDevicesResponse?>(null)
    val devices = _devices.asStateFlow()



    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _deviceStatus =
        MutableStateFlow<DeviceStatusResponse?>(null)
    val deviceStatus = _deviceStatus.asStateFlow()


    private val _statusFilter = MutableStateFlow("all")
    val statusFilter = _statusFilter.asStateFlow()
    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    /** The chosen day as ISO `yyyy-MM-dd`, or null while the user has not picked one. */
    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate = _selectedDate.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _addressMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val addressMap = _addressMap.asStateFlow()


    /**
     * The day's sessions, each with both ends resolved, then filtered.
     *
     * Resolved *before* filtering, never after: a session's start is derived from the
     * session before it, and in a list of only the parked ones that predecessor is the
     * wrong session — the drive in between is what moved the vehicle. See
     * [com.developer.ev2z.response.asLegs].
     */
    val filteredSessions = combine(
        deviceStatus,
        statusFilter
    ) { statusResponse, filter ->
        val legs = (statusResponse?.data?.sessions ?: emptyList()).asLegs()
        if (filter == "all") legs
        else legs.filter { it.session.status == filter }
    }

    fun setFilter(filter: String) {
        _statusFilter.value = filter
    }


    fun loadDevices() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _devices.value = repo.getMyDevices(
                    token = tokenProvider()
                )
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadDeviceStatus(deviceId: String, date: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _deviceStatus.value = repo.getDeviceStatus(
                    token = tokenProvider(),
                    deviceId = deviceId,
                    date = date
                )
            } catch (e: Exception) {
                // An unparseable body (an HTML error page, a gateway timeout) used to
                // escape viewModelScope and take the screen down with it; the day a
                // device has no data is the common case here, not a crash.
                _deviceStatus.value = null
                _error.value = e.message ?: "Could not load status for this date"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Picks the device. Nothing is fetched until a day is chosen as well — the screen
     * asks for the two in that order, and a device on its own has no report.
     *
     * A day already chosen is kept and re-fetched for the new device, so switching
     * vehicles mid-report does not send the user back to the date picker.
     */
    fun selectDevice(deviceId: String) {
        if (_selectedDeviceId.value == deviceId) return

        _selectedDeviceId.value = deviceId
        // The previous device's day is not this device's day: cleared, so nothing stale
        // is on screen while the new request is in flight.
        _deviceStatus.value = null
        _error.value = null
        _statusFilter.value = "all"

        _selectedDate.value?.let { loadDeviceStatus(deviceId, it) }
    }

    /** [date] is ISO `yyyy-MM-dd`. Fetches as soon as a device is also selected. */
    fun selectDate(date: String) {
        _selectedDate.value = date
        _deviceStatus.value = null
        _error.value = null
        _statusFilter.value = "all"

        _selectedDeviceId.value?.let { loadDeviceStatus(it, date) }
    }

    /** Re-asks for the day already on screen — the retry behind a failed load. */
    fun reload() {
        val deviceId = _selectedDeviceId.value ?: return
        val date = _selectedDate.value ?: return
        loadDeviceStatus(deviceId, date)
    }


    fun resolveAddress(lat: Double, lng: Double) {
        val key = "$lat,$lng"

        // already fetched → skip
        if (_addressMap.value.containsKey(key)) return

        viewModelScope.launch {
            try {
                val address = getAddressFromLatLng(lat, lng)
                _addressMap.value = _addressMap.value + (key to address)
            } catch (e: Exception) {
                _addressMap.value =
                    _addressMap.value + (key to "Location unavailable")
            }
        }
    }


}
