package com.developer.ev2z.network

import com.developer.ev2z.response.DeviceResponse
import com.developer.ev2z.response.MyDevicesResponse
import com.developer.ev2z.request.LoginRequest
import com.developer.ev2z.response.LoginResponse
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject

class ApiService {

    private val client = KtorClient.client
    private val baseUrl = KtorClient.BASE_URL

    suspend fun login(request: LoginRequest): LoginResponse {
        return client.post("${baseUrl}login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }


    suspend fun getUserDevices(token: String): DeviceResponse {
        return client.get("${baseUrl}user/devices/latest-locations") {
            header("Authorization", token)
        }.body()
    }


    suspend fun addDevice(token: String, payload: JsonObject): JsonObject {
        return client.post("${baseUrl}add-device") {
            header("Authorization", token)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }


    suspend fun updateDevice(token: String, payload: JsonObject): JsonObject {
        return client.post("${baseUrl}update-device") {
            header("Authorization", token)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }


    suspend fun getMyDevices(token: String): MyDevicesResponse {
        return client.get("${baseUrl}devices/my-devices") {
            header("Authorization", token)
        }.body()
    }


    suspend fun deleteDevice(deviceId: String, token: String): JsonObject {
        return client.delete("${baseUrl}api/delete-device/$deviceId") {
            header("Authorization", token)
        }.body()
    }


    suspend fun mobilizeDevice(
        token: String,
        terminalId: String,
        command: String
    ): JsonObject {
        return client.submitForm(
            url = "${baseUrl}api/mobilize",
            formParameters = Parameters.build {
                append("terminal_id", terminalId)
                append("command", command)
            }
        ) {
            header("Authorization", token)
            header("Accept", "application/json")
        }.body()
    }
}