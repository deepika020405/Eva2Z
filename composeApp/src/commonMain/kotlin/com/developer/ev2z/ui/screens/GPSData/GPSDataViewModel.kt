package com.developer.ev2z.ui.screens.GPSData

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.util.getAddressFromLatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
//import kotlinx.datetime.Clock
//import kotlinx.datetime.Instant
//import kotlinx.datetime.TimeZone
//import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs


data class GPSDevice(
    val deviceId: String,
    val deviceName: String,
    val status: String,
    val ignition: String,
    val battery: Int,
    val batteryStatus: BatteryStatus,
    val distance: Double,
    val speed: Double,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val fixTime: String,
    val serverTime: String,
    val lastUpdated: String
)

enum class DeviceStatus { ONLINE, OFFLINE }
//enum class IgnitionStatus { ON, OFF, UNKNOWN }
enum class BatteryStatus { CHARGING, NO_POWER, NORMAL, MEDUIM }



data class GPSDataState(
    val isLoading: Boolean = false,
    val devices: List<GPSDevice> = emptyList(),
    val selectedDevice: GPSDevice? = null,
    val error: String? = null
)

class GPSDataViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) : ViewModel() {

    private val _state = MutableStateFlow(GPSDataState())
    val state: StateFlow<GPSDataState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                loadDevices()
                delay(10000)
            }
        }
    }

    fun loadDevices() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val token = storageManager.getString("token", "")
                if (token.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Authentication required"
                    )
                    return@launch
                }

                val devicesResponse = apiService.getMyDevices("Bearer $token")
                if (!devicesResponse.success) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load devices"
                    )
                    return@launch
                }

                val gpsDevices = mutableListOf<GPSDevice>()

                for (device in devicesResponse.devices) {
                    try {
                        val locationResponse = apiService.getDeviceLiveLocation(
                            "Bearer $token",
                            device.device_id
                        )

                        val latestLocation = locationResponse.data?.latestLocation

                        if (latestLocation != null) {

                            val minutesAgo = latestLocation.lastSeenMinutesAgo ?: 999
                            val isOnline = minutesAgo <= 5

                            println("📡 Device ${device.device_id}")
                            println("   API Status: ${latestLocation.status}")
                            println("   Last Seen: $minutesAgo min")
                            println("   Calculated: ${if (isOnline) "ONLINE" else "OFFLINE"}")
                            println("   Battery: ${latestLocation.batteryDisplay}")
                            println("------------------------------------------------")

                            val address = getAddressFromLatLng(
                                latestLocation.latitude.toDouble(),
                                latestLocation.longitude.toDouble()
                            )

                            gpsDevices.add(
                                GPSDevice(
                                    deviceId = device.device_id,
                                    deviceName = device.device_name,

                                    // ✅ FIXED STATUS
                                    status = if (isOnline) "Online" else "Offline",

                                    ignition = if (latestLocation.speed.toDouble() > 0) "On" else "Off",

                                    battery = parseBattery(latestLocation.batteryDisplay),
                                    batteryStatus = parseBatteryStatus(latestLocation.batteryStatus),
                                    distance = 0.0,
                                    speed = latestLocation.speed.toDouble(),
                                    location = address,
                                    latitude = latestLocation.latitude.toDoubleOrNull() ?: 0.0,
                                    longitude = latestLocation.longitude.toDoubleOrNull() ?: 0.0,
                                    fixTime = convertUtcToIst(latestLocation.timestamp),
                                    serverTime = convertUtcToIst(latestLocation.timestamp),
                                    lastUpdated = formatLastSeen(minutesAgo)
                                )
                            )
                        } else {
                            gpsDevices.add(createOfflineDevice(device))
                        }

                    } catch (e: Exception) {
                        gpsDevices.add(createOfflineDevice(device))
                    }
                }

                // ✅ Preserve selected device & update it
                val currentSelectedId = _state.value.selectedDevice?.deviceId
                val updatedSelected =
                    gpsDevices.find { it.deviceId == currentSelectedId }

                _state.value = _state.value.copy(
                    isLoading = false,
                    devices = gpsDevices.toList(), // force new reference
                    selectedDevice = updatedSelected
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed"
                )
            }
        }
    }




    // ✅ Refresh single device (used in detail screen)
    fun refreshDeviceData(deviceId: String) {
        viewModelScope.launch {
            loadDevices()
        }
    }

    fun selectDevice(device: GPSDevice) {
        _state.value = _state.value.copy(selectedDevice = device)
    }

    fun deselectDevice() {
        _state.value = _state.value.copy(selectedDevice = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }


    private fun createOfflineDevice(device: com.developer.ev2z.response.Device) = GPSDevice(
        deviceId = device.device_id,
        deviceName = device.device_name,
        status = "Offline",
        ignition = "Off",
        battery = 0,
        batteryStatus = BatteryStatus.NO_POWER,
        distance = 0.0,
        speed = 0.0,
        location = "No recent data",
        latitude = 0.0,
        longitude = 0.0,
        fixTime = "—",
        serverTime = "—",
        lastUpdated = "No data"
    )


    private fun parseBattery(batteryDisplay: String?): Int {
        if (batteryDisplay.isNullOrBlank()) return -1   // unknown

        val value = batteryDisplay.replace("%", "").trim().toIntOrNull()
        return value ?: -1

    }

    private fun parseBatteryStatus(status: String?) =
        when (status?.lowercase()) {
            "charging" -> BatteryStatus.CHARGING
            "no power" -> BatteryStatus.NO_POWER
            "normal" -> BatteryStatus.NORMAL
            else -> BatteryStatus.NORMAL
        }

//    private fun convertUtcToIst(utcTime: String): String {
//        return try {
//            val instant = Instant.parse(utcTime.replace(" ", "T") + "Z")
//            val ist = instant.toLocalDateTime(TimeZone.of("Asia/Kolkata"))
//            "${ist.date} ${ist.time}"
//        } catch (e: Exception) {
//            utcTime
//        }
//    }
//
//    private fun getCurrentTime(): String {
//        val instant = Clock.System.now()
//        val ist = instant.toLocalDateTime(TimeZone.of("Asia/Kolkata"))
//        return "${ist.date} ${ist.time}"
//    }
//
//    private fun getRelativeTime(utcTime: String): String {
//        return try {
//            val instant = Instant.parse(utcTime.replace(" ", "T") + "Z")
//            val diff = Clock.System.now().toEpochMilliseconds() - instant.toEpochMilliseconds()
//
//            val minutes = diff / 60000
//            val hours = diff / 3600000
//            val days = diff / 86400000
//
//            when {
//                minutes < 1L -> "Just now"
//                minutes < 60L -> "$minutes mins ago"
//                hours < 24L -> "$hours hours ago"
//                else -> "$days days ago"
//            }
//        } catch (e: Exception) {
//            "Unknown"
//        }
//    }

//    private fun convertUtcToIst(utcTime: String): String {
//        return try {
//            // Expected: "2025-12-29 14:30:00"
//            val parts = utcTime.replace("T", " ").split(" ", ":", "-")
//            val year = parts[0].toInt()
//            val month = parts[1].toInt()
//            val day = parts[2].toInt()
//            val hour = parts[3].toInt() + 5
//            val minute = parts[4].toInt() + 30
//            val second = parts[5].toInt()
//
//            val finalHour = (hour + minute / 60) % 24
//            val finalMinute = minute % 60
//
//            "%04d-%02d-%02d %02d:%02d:%02d".format(
//                year, month, day, finalHour, finalMinute, second
//            )
//        } catch (e: Exception) {
//            utcTime
//        }
//    }

    private fun convertUtcToIst(utcTime: String): String {
        return try {
            val parts = utcTime.replace("T", " ").split(" ", ":", "-")

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            var hour = parts[3].toInt()
            var minute = parts[4].toInt()
            val second = parts[5].toInt()

            hour += 5
            minute += 30
            hour += minute / 60
            minute %= 60
            hour %= 24

            "${year.pad()}-${month.pad()}-${day.pad()} ${hour.pad()}:${minute.pad()}:${second.pad()}"
        } catch (e: Exception) {
            utcTime
        }
    }

    private fun Int.pad(): String =
        if (this < 10) "0$this" else this.toString()

    private fun formatLastSeen(minutes: Int?): String {
        if (minutes == null) return "Live location"

        return when {
            minutes <= 1 -> "Live location"
            minutes < 60 -> "$minutes mins ago"
            minutes < 1440 -> "${minutes / 60} hours ago"
            else -> "${minutes / 1440} days ago"
        }
    }
}


