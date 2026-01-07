package com.developer.ev2z.repository


import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.response.MyDevicesResponse

class TrackTraceRepository(
    private val api: ApiService
) {

    suspend fun getMyDevices(token: String): MyDevicesResponse {
        return api.getMyDevices(token)
    }

    suspend fun getLiveLocation(
        token: String,
        deviceId: String
    ): LiveLocationResponse {
        return api.getDeviceLiveLocation(token, deviceId)
    }
}
