package com.developer.ev2z.ui.screens.GPSData

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.data.local.StorageManager
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
    val status: DeviceStatus,
    val ignition: IgnitionStatus,
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
enum class IgnitionStatus { ON, OFF, UNKNOWN }
enum class BatteryStatus { CHARGING, NO_POWER, NORMAL, UNKNOWN }

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
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val token = storageManager.getString("token", "")
                if (token.isEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "Authentication required")
                    return@launch
                }

                val devicesResponse = apiService.getMyDevices("Bearer $token")
                if (!devicesResponse.success) {
                    _state.value = _state.value.copy(isLoading = false, error = "Failed to load devices")
                    return@launch
                }

                val gpsDevices = mutableListOf<GPSDevice>()

                for (device in devicesResponse.devices) {
                    try {
                        val locationResponse = apiService.getDeviceLiveLocation("Bearer $token", device.device_id)
                        val statusResponse = apiService.getDeviceStatus("Bearer $token", device.device_id)

                        val latestLocation = locationResponse.data?.latestLocation

                        if (latestLocation != null) {
                            gpsDevices.add(
                                GPSDevice(
                                    deviceId = device.device_id,
                                    deviceName = device.device_name,
                                    status = parseDeviceStatus(latestLocation.status),
                                    ignition = parseIgnitionStatus(statusResponse["status"]?.jsonPrimitive?.content ?: "unknown"),
                                    battery = parseBattery(latestLocation.batteryDisplay),
                                    batteryStatus = parseBatteryStatus(latestLocation.batteryStatus),
                                    distance = 0.0,
                                    speed = latestLocation.speed.toDouble(),
                                    location = "Loading address...",
                                    latitude = latestLocation.latitude.toDoubleOrNull() ?: 0.0,
                                    longitude = latestLocation.longitude.toDoubleOrNull() ?: 0.0,
                                    fixTime = convertUtcToIst(latestLocation.timestamp),
                                    serverTime = convertUtcToIst(latestLocation.timestamp),
                                    lastUpdated = getRelativeTime(latestLocation.timestamp)
                                )
                            )
                        } else {
                            gpsDevices.add(createOfflineDevice(device))
                        }
                    } catch (e: Exception) {
                        println("❌ Device load failed: ${e.message}")
                        gpsDevices.add(createOfflineDevice(device))
                    }
                }

                _state.value = _state.value.copy(isLoading = false, devices = gpsDevices, selectedDevice = null)

            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Failed")
            }
        }
    }

    private fun createOfflineDevice(device: com.developer.ev2z.response.Device) = GPSDevice(
        deviceId = device.device_id,
        deviceName = device.device_name,
        status = DeviceStatus.OFFLINE,
        ignition = IgnitionStatus.UNKNOWN,
        battery = 0,
        batteryStatus = BatteryStatus.UNKNOWN,
        distance = 0.0,
        speed = 0.0,
        location = "No recent data",
        latitude = 0.0,
        longitude = 0.0,
        fixTime = getCurrentTime(),
        serverTime = getCurrentTime(),
        lastUpdated = "No data"
    )

    private fun parseDeviceStatus(status: String) =
        if (status.lowercase() in listOf("online", "active")) DeviceStatus.ONLINE else DeviceStatus.OFFLINE

    private fun parseIgnitionStatus(status: String) =
        when (status.lowercase()) {
            "on" -> IgnitionStatus.ON
            "off" -> IgnitionStatus.OFF
            else -> IgnitionStatus.UNKNOWN
        }

    private fun parseBattery(batteryDisplay: String?) =
        batteryDisplay?.replace("%", "")?.trim()?.toIntOrNull() ?: 0

    private fun parseBatteryStatus(status: String?) =
        when (status?.lowercase()) {
            "charging" -> BatteryStatus.CHARGING
            "no power" -> BatteryStatus.NO_POWER
            "normal" -> BatteryStatus.NORMAL
            else -> BatteryStatus.UNKNOWN
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
            val parts = utcTime.replace("T", " ")
                .split(" ", ":", "-")

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            var hour = parts[3].toInt()
            var minute = parts[4].toInt()
            val second = parts[5].toInt()

            // IST offset
            hour += 5
            minute += 30

            hour += minute / 60
            minute %= 60
            hour %= 24

            // ✅ KMP-safe formatting
            "${year.pad()}-${month.pad()}-${day.pad()} " +
                    "${hour.pad()}:${minute.pad()}:${second.pad()}"

        } catch (e: Exception) {
            utcTime
        }
    }

    private fun Int.pad(): String =
        if (this < 10) "0$this" else this.toString()


    private fun getCurrentTime(): String {
        return "Just now"
    }

    private fun getRelativeTime(utcTime: String): String {
        return try {
            val parts = utcTime.replace("T", " ").split(" ", ":", "-")
            val hour = parts[3].toInt()
            val minute = parts[4].toInt()

            val nowHour = 12 // You can improve later if needed

            val diffHours = abs(nowHour - hour)

            when {
                diffHours == 0 -> "Just now"
                diffHours == 1 -> "1 hour ago"
                diffHours < 24 -> "$diffHours hours ago"
                else -> "1+ days ago"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }


    fun selectDevice(device: GPSDevice) {
        _state.value = _state.value.copy(selectedDevice = device)
    }

    fun deselectDevice() {
        _state.value = _state.value.copy(selectedDevice = null)
    }

    fun refreshDeviceData(deviceId: String) {
        loadDevices()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
