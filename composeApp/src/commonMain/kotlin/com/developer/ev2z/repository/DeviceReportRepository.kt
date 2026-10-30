package com.developer.ev2z.repository

import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.DeviceAnalyticsResponse
import com.developer.ev2z.response.MobilizeResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.RouteHistoryResponse
import com.developer.ev2z.response.TotalMilagesResponse
import kotlinx.coroutines.Dispatchers
// Not redundant: `Dispatchers.IO` is an extension on the Dispatchers object in
// commonMain, so without this import Android still compiles (it resolves the JVM
// member) and the iOS targets fail.
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

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
    /**
     * On `IO`, not merely *called* from it. `getDevicesLatestLocations` returns to
     * whatever dispatcher asked for it, and the caller is `viewModelScope` — so the
     * remapping below used to walk the whole fleet and allocate a [Device] per vehicle
     * on the UI thread, once every [com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS]
     * for as long as this screen was open. Small per device, but it is the fleet, on a
     * timer, competing with the frame it is about to cause.
     */
    suspend fun getMyDevices(token: String): MyDevicesResponse = withContext(Dispatchers.IO) {
        val latest = apiService.getDevicesLatestLocations(token)
        MyDevicesResponse(
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
                    created_at = src.timestamp ?: "",
                    // Carried, not dropped. This is the only endpoint on this screen
                    // that knows which vehicle the tracker is fitted to —
                    // `live-location/{id}` sends no vehicle_id and a null icon_url — so
                    // losing it here is what left the map marker as a bare arrow for a
                    // device that has a vehicle assigned.
                    vehicle_id = src.vehicleId,
                    icon = src.icon,
                    icon_url = src.iconUrl
                )
            }
        )
    }

    /**
     * `device_id` → `device_type` ("wired" / "wireless") for every device on the account.
     *
     * A second endpoint on purpose: [getMyDevices] above reads `latest-locations` for
     * real-time status, and that payload carries no `device_type` at all — so the screens
     * deciding whether a tracker has a relay to cut would see null for every device. The
     * stale `engine_status` that moved this screen off `devices/my-devices` doesn't matter
     * here, because only the type is taken from it. Devices that report no type are
     * dropped rather than guessed at.
     */
    suspend fun getDeviceTypes(token: String): Map<String, String> = withContext(Dispatchers.IO) {
        apiService.getMyDevices(token).devices
            .filter { it.device_id.isNotBlank() && !it.device_type.isNullOrBlank() }
            .associate { it.device_id to it.device_type!!.trim().lowercase() }
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

    /**
     * The raw fixes behind the activity breakdown.
     *
     * `device/analytics` already reports running / stopped / parked, but it has been
     * answering zero for all three — so the screen works the figures out from the fixes
     * themselves instead (see `computeActivityBreakdown`). Dates are `YYYY-MM-DD`,
     * inclusive at both ends, the same as the playback screen sends.
     */
    suspend fun getRouteHistory(
        token: String,
        terminalId: String,
        startDate: String,
        endDate: String
    ): RouteHistoryResponse {
        return apiService.getRouteHistory(
            token = token,
            startDate = startDate,
            endDate = endDate,
            terminalId = terminalId
        )
    }
}