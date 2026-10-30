package com.developer.ev2z.repository

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.DeviceAlarm
import com.developer.ev2z.util.todayInReportZone

class HomeRepository(private val apiService: ApiService) {

    suspend fun getDevicesLatestLocations(token: String): Result<DeviceResponse> {
        return try {
            val response = apiService.getDevicesLatestLocations(token)
            Result.success(response)
        } catch (e: Exception) {
            println("❌ Repository Error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * One device's ignition — "on", "off", or null when it cannot be told.
     *
     * No `Result` wrapper, unlike the call above: the only caller is the idle derivation
     * in [com.developer.ev2z.util.IdleStatusResolver], which treats every failure as
     * "not idling" and has nothing to do with a failure it can distinguish.
     */
    suspend fun getDeviceIgnition(token: String, deviceId: String): String? =
        apiService.getDeviceIgnition(token, deviceId)

    /**
     * One device's recorded events, for the engine on/off alerts.
     *
     * Unwrapped, and deliberately *not* swallowing failures the way [getDeviceIgnition]
     * does: the notifier has to be able to tell "no new events" from "could not read the
     * log", because the two mean opposite things to something that works by comparing
     * against what it last saw. [com.developer.ev2z.util.IgnitionAlerts] handles the
     * failures, including the 404 that stands the feature down.
     */
    suspend fun getDeviceAlarms(token: String, deviceId: String): List<DeviceAlarm> =
        apiService.getDeviceAlarms(token, deviceId).data

    /**
     * The newest status the server recorded for this device today, for the same
     * derivation — unwrapped for the same reason, and defaulting to the server's own day
     * so no caller has to work out which date "today" is (see [todayInReportZone]).
     */
    suspend fun getDeviceLatestSessionStatus(
        token: String,
        deviceId: String,
        date: String = todayInReportZone(),
    ): String? = apiService.getDeviceLatestSessionStatus(token, deviceId, date)
}