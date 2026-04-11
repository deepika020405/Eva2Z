package com.developer.ev2z.repository

import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.DeviceAnalyticsResponse
import com.developer.ev2z.response.MobilizeResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.TotalMilagesResponse

class DeviceReportRepository(
    private val apiService: ApiService
) {

    /**
     * Returns the device list with **real-time** per-device status.
     * Internally calls the `user/devices/latest-locations` endpoint (same
     * one LiveLocation screen uses) and maps its payload into the
     * MyDevicesResponse shape the VM already consumes.
     *
     * The old `devices/my-devices` endpoint returned a stale `engine_status`
     * and caused moving devices to show as "Stopped" in the dropdown.
     */
    suspend fun getMyDevices(token: String): MyDevicesResponse {
        val latest = apiService.getDevicesLatestLocations(token)
        return MyDevicesResponse(
            success = latest.success,
            total_devices = latest.totalDevices,
            username = latest.name,
            devices = latest.data.map { src ->
                Device(
                    id = 0,
                    device_id = src.deviceId,
                    device_name = src.deviceName ?: src.deviceId,
                    engine_status = src.status, // "moving" / "idle" / "offline" / "no_data"
                    status = src.status,
                    created_at = src.timestamp ?: ""
                )
            }
        )
    }

    suspend fun getDeviceLiveLocation(
        token: String,
        deviceId: String
    ): LiveLocationResponse {
        return apiService.getDeviceLiveLocation(token, deviceId)
    }


    suspend fun getTotalMilages(
        token: String,
        terminalId: String
    ): TotalMilagesResponse {
        return apiService.getTotalMilages(token, terminalId)
    }

    suspend fun sendMobilizeCommand(
        token: String,
        terminalId: String,
        command: String
    ): MobilizeResponse {
        return apiService.sendMobilizeCommand(token, terminalId, command)
    }


    suspend fun getDeviceAnalytics(
        token: String,
        deviceId: String,
        range: String
    ): DeviceAnalyticsResponse {
        return apiService.getDeviceAnalytics(token, deviceId, range)
    }
}