//package com.developer.ev2z.ui.screens.GPSData
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.developer.ev2z.network.ApiService
//import com.developer.ev2z.data.local.StorageManager
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.serialization.json.jsonPrimitive
//import kotlinx.datetime.*
//
//data class GPSDevice(
//    val deviceId: String,
//    val deviceName: String,
//    val status: DeviceStatus,
//    val ignition: IgnitionStatus,
//    val battery: Int,
//    val batteryStatus: BatteryStatus,
//    val distance: Double,
//    val speed: Double,
//    val location: String,
//    val latitude: Double,
//    val longitude: Double,
//    val fixTime: String,
//    val serverTime: String,
//    val lastUpdated: String
//)
//
//enum class DeviceStatus {
//    ONLINE, OFFLINE
//}
//
//enum class IgnitionStatus {
//    ON, OFF, UNKNOWN
//}
//
//enum class BatteryStatus {
//    CHARGING, NO_POWER, NORMAL, UNKNOWN
//}
//
//data class GPSDataState(
//    val isLoading: Boolean = false,
//    val devices: List<GPSDevice> = emptyList(),
//    val selectedDevice: GPSDevice? = null,
//    val error: String? = null
//)
//
//class GPSDataViewModel(
//    private val apiService: ApiService,
//    private val storageManager: StorageManager
//) : ViewModel() {
//
//    private val _state = MutableStateFlow(GPSDataState())
//    val state: StateFlow<GPSDataState> = _state.asStateFlow()
//
//    init {
//        loadDevices()
//    }
//
//    fun loadDevices() {
//        viewModelScope.launch {
//            _state.value = _state.value.copy(isLoading = true, error = null)
//
//            try {
//                val token = storageManager.getString("token", "")
//                if (token.isEmpty()) {
//                    _state.value = _state.value.copy(
//                        isLoading = false,
//                        error = "Authentication required"
//                    )
//                    return@launch
//                }
//
//                // Get all devices
//                val devicesResponse = apiService.getMyDevices("Bearer $token")
//
//                if (!devicesResponse.success) {
//                    _state.value = _state.value.copy(
//                        isLoading = false,
//                        error = "Failed to load devices"
//                    )
//                    return@launch
//                }
//
//                // Fetch GPS data for each device
//                val gpsDevices = mutableListOf<GPSDevice>()
//
//                for (device in devicesResponse.devices) {
//                    try {
//                        // Get live location
//                        val locationResponse = apiService.getDeviceLiveLocation(
//                            "Bearer $token",
//                            device.device_id
//                        )
//
//                        // Get device status (ignition)
//                        val statusResponse = apiService.getDeviceStatus(
//                            "Bearer $token",
//                            device.device_id
//                        )
//
//                        // ✅ FIXED: Use correct property names (camelCase)
//                        val latestLocation = locationResponse.data?.latestLocation
//
//                        if (latestLocation != null) {
//                            val gpsDevice = GPSDevice(
//                                deviceId = device.device_id,
//                                deviceName = device.device_name,
//                                status = parseDeviceStatus(latestLocation.status),
//                                ignition = parseIgnitionStatus(
//                                    statusResponse["status"]?.jsonPrimitive?.content ?: "unknown"
//                                ),
//                                battery = parseBattery(latestLocation.batteryDisplay),
//                                batteryStatus = parseBatteryStatus(latestLocation.batteryStatus),
//                                distance = 0.0, // Not available in API
//                                speed = latestLocation.speed.toDouble(),
//                                location = "Loading address...",
//                                latitude = latestLocation.latitude.toDoubleOrNull() ?: 0.0,
//                                longitude = latestLocation.longitude.toDoubleOrNull() ?: 0.0,
//                                fixTime = convertUtcToIst(latestLocation.timestamp),
//                                serverTime = convertUtcToIst(latestLocation.timestamp),
//                                lastUpdated = getRelativeTime(latestLocation.timestamp)
//                            )
//                            gpsDevices.add(gpsDevice)
//                        } else {
//                            // Device exists but no location data
//                            gpsDevices.add(createOfflineDevice(device))
//                        }
//
//                    } catch (e: Exception) {
//                        println("❌ Error loading GPS data for ${device.device_id}: ${e.message}")
//                        // Add device as offline if API fails
//                        gpsDevices.add(createOfflineDevice(device))
//                    }
//                }
//
//                _state.value = _state.value.copy(
//                    isLoading = false,
//                    devices = gpsDevices,
//                    selectedDevice = null
//                )
//
//            } catch (e: Exception) {
//                _state.value = _state.value.copy(
//                    isLoading = false,
//                    error = e.message ?: "Failed to load devices"
//                )
//                println("❌ Load devices error: ${e.message}")
//            }
//        }
//    }
//
//    private fun createOfflineDevice(device: com.developer.ev2z.response.Device): GPSDevice {
//        return GPSDevice(
//            deviceId = device.device_id,
//            deviceName = device.device_name,
//            status = DeviceStatus.OFFLINE,
//            ignition = IgnitionStatus.UNKNOWN,
//            battery = 0,
//            batteryStatus = BatteryStatus.UNKNOWN,
//            distance = 0.0,
//            speed = 0.0,
//            location = "No recent data",
//            latitude = 0.0,
//            longitude = 0.0,
//            fixTime = getCurrentTime(),
//            serverTime = getCurrentTime(),
//            lastUpdated = "No data"
//        )
//    }
//
//    private fun parseDeviceStatus(status: String): DeviceStatus {
//        return when (status.lowercase()) {
//            "online", "active" -> DeviceStatus.ONLINE
//            else -> DeviceStatus.OFFLINE
//        }
//    }
//
//    private fun parseIgnitionStatus(status: String): IgnitionStatus {
//        return when (status.lowercase()) {
//            "on" -> IgnitionStatus.ON
//            "off" -> IgnitionStatus.OFF
//            else -> IgnitionStatus.UNKNOWN
//        }
//    }
//
//    private fun parseBattery(batteryDisplay: String?): Int {
//        return try {
//            batteryDisplay?.replace("%", "")?.trim()?.toIntOrNull() ?: 0
//        } catch (e: Exception) {
//            0
//        }
//    }
//
//    private fun parseBatteryStatus(status: String?): BatteryStatus {
//        return when (status?.lowercase()) {
//            "charging" -> BatteryStatus.CHARGING
//            "no power" -> BatteryStatus.NO_POWER
//            "normal" -> BatteryStatus.NORMAL
//            else -> BatteryStatus.UNKNOWN
//        }
//    }
//
//    // ✅ KMP-COMPATIBLE: Use kotlinx-datetime instead of SimpleDateFormat
//    private fun convertUtcToIst(utcTime: String): String {
//        return try {
//            // Parse UTC time: "2025-12-29 14:30:00"
//            val instant = utcTime.replace(" ", "T")
//                .let { "$it:00Z" } // Add seconds and Z for UTC
//                .let { Instant.parse(it) }
//
//            // Convert to IST
//            val istTime = instant.toLocalDateTime(TimeZone.of("Asia/Kolkata"))
//
//            // Format: "2025-12-29 20:00:00"
//            "${istTime.date} ${istTime.time}"
//        } catch (e: Exception) {
//            println("❌ Time parse error: ${e.message}")
//            utcTime
//        }
//    }
//
//    private fun getCurrentTime(): String {
//        val now = Clock.System.now()
//        val istTime = now.toLocalDateTime(TimeZone.of("Asia/Kolkata"))
//        return "${istTime.date} ${istTime.time}"
//    }
//
//    private fun getRelativeTime(utcTime: String): String {
//        return try {
//            // Parse UTC timestamp
//            val instant = utcTime.replace(" ", "T")
//                .let { "$it:00Z" }
//                .let { Instant.parse(it) }
//
//            val now = Clock.System.now()
//            val diff = now - instant
//
//            val minutes = diff.inWholeMinutes
//            val hours = diff.inWholeHours
//            val days = diff.inWholeDays
//
//            when {
//                minutes < 1 -> "Just now"
//                minutes < 60 -> "$minutes mins ago"
//                hours < 24 -> "$hours hours ago"
//                else -> "$days days ago"
//            }
//        } catch (e: Exception) {
//            println("❌ Relative time error: ${e.message}")
//            "Unknown"
//        }
//    }
//
//    fun selectDevice(device: GPSDevice) {
//        _state.value = _state.value.copy(selectedDevice = device)
//    }
//
//    fun deselectDevice() {
//        _state.value = _state.value.copy(selectedDevice = null)
//    }
//
//    fun refreshDeviceData(deviceId: String) {
//        viewModelScope.launch {
//            try {
//                println("🔄 Refreshing device data for: $deviceId")
//                loadDevices()
//            } catch (e: Exception) {
//                println("❌ Refresh error: ${e.message}")
//            }
//        }
//    }
//
//    fun clearError() {
//        _state.value = _state.value.copy(error = null)
//    }
//}