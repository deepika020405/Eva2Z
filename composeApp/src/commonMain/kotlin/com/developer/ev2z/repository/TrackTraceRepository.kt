package com.developer.ev2z.repository


import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.RouteHistoryResponse

class TrackTraceRepository(
    private val api: ApiService
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
        println("🌐 [TrackTraceRepo] Calling getDevicesLatestLocations, token=${if (token.isBlank()) "EMPTY ❌" else "OK ✅"}")
        val latest = api.getDevicesLatestLocations(token)
        println("🌐 [TrackTraceRepo] Raw response: success=${latest.success}, totalDevices=${latest.totalDevices}, name=${latest.name}, dataSize=${latest.data.size}")
        latest.data.forEachIndexed { i, d ->
            println("  raw[$i] deviceId=${d.deviceId} name=${d.deviceName} status=${d.status} lat=${d.latitude} lng=${d.longitude}")
        }
        val mapped = MyDevicesResponse(
            success = latest.success,
            total_devices = latest.totalDevices,
            username = latest.name,
            devices = latest.data.map { src ->
                Device(
                    id = 0,
                    device_id = src.deviceId,
                    device_name = src.deviceName ?: src.deviceId,
                    engine_status = src.status,
                    status = src.status,
                    created_at = src.timestamp ?: ""
                )
            }
        )
        println("🌐 [TrackTraceRepo] Mapped devices count: ${mapped.devices.size}")
        return mapped
    }

    suspend fun getLiveLocation(
        token: String,
        deviceId: String
    ): LiveLocationResponse {
        return api.getDeviceLiveLocation(token, deviceId)
    }

    suspend fun getTodayRouteHistory(
        token: String,
        deviceId: String,
        today: String
    ): RouteHistoryResponse {
        return api.getRouteHistory(token, today, today, deviceId)
    }
}