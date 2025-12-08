package com.developer.ev2z.repository

import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.DeviceAnalyticsResponse
import com.developer.ev2z.response.MobilizeResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.response.TotalMilagesResponse

class DeviceReportRepository(
    private val apiService: ApiService
) {

    suspend fun getMyDevices(token: String): MyDevicesResponse {
        return apiService.getMyDevices(token)
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