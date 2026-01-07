package com.developer.ev2z.repository

import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.DeviceStatusResponse
import com.developer.ev2z.response.MyDevicesResponse

class DeviceStatusRepository(
    private val api: ApiService,
) {

    suspend fun getMyDevices(token: String): MyDevicesResponse {
        return api.getMyDevices(token)
    }

    suspend fun getDeviceStatus(
        token: String,
        deviceId: String
    ): DeviceStatusResponse {
        return api.getDevicesStatus(token, deviceId)
    }


}


