package com.developer.ev2z.repository

import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.MyDevicesResponse

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
}