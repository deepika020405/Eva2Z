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

    /** [date] is the single ISO `yyyy-MM-dd` day the report covers, start and end. */
    suspend fun getDeviceStatus(
        token: String,
        deviceId: String,
        date: String? = null
    ): DeviceStatusResponse {
        return api.getDevicesStatus(
            token = token,
            deviceId = deviceId,
            startDate = date,
            endDate = date
        )
    }


}


