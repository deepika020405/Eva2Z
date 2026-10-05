package com.developer.ev2z.data.local

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class UserPrefs(private val storage: StorageManager) {  // ✅ Storage → StorageManager

    fun saveUser(
        token: String,
        name: String,
        phone: String,
        email: String?,
        photo: String?,
        userId: Int = 0
    ) {
        storage.saveString("token", token)
        storage.saveString("name", name)
        storage.saveString("phone", phone)
        email?.let { storage.saveString("email", it) }
        photo?.let { storage.saveString("photo", it) }
        if (userId > 0) storage.saveString("user_id", userId.toString())
    }

    fun getToken() = storage.getString("token", "")
    fun getName() = storage.getString("name", "")
    fun getPhone() = storage.getString("phone", "")
    fun getEmail() = storage.getString("email", "")
    fun getPhoto() = storage.getString("photo", "")
    fun getLastFcmToken(): String? =
        storage.getString("last_fcm_token", "")

    fun saveLastFcmToken(token: String) {
        storage.saveString("last_fcm_token", token)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun getOrCreateDeviceUuid(): String {
        val existing = storage.getString("device_uuid", "")
        if (existing.isNotBlank()) return existing
        val uuid = Uuid.random().toString()
        storage.saveString("device_uuid", uuid)
        return uuid
    }


    fun isLoggedIn() = getToken()?.isNotEmpty()

    /**
     * The signed-in user's real id, saved from the login response. 0 when the
     * session was created before this was stored — callers that need certainty
     * (ownership checks) must treat 0 as "unknown", not as a user.
     */
    fun getAuthUserId(): Int =
        storage.getString("user_id", "")?.toIntOrNull() ?: 0

    /**
     * Legacy id accessor. Prefers the stored user id and falls back to the token
     * prefix — note that "6639|abc…" is the *token* id, NOT the user id, so this
     * fallback is a guess. Use [getAuthUserId] for anything the server authorizes.
     */
    fun getUserId(): Int {
        getAuthUserId().takeIf { it > 0 }?.let { return it }
        val token = getToken() ?: return 0
        return token.substringBefore("|").toIntOrNull() ?: 0
    }

    // device_uuid identifies the phone, not the session — keep it across logout so the
    // next login updates the same server-side device row instead of creating a new one.
    fun clear() {
        val deviceUuid = storage.getString("device_uuid", "")
        storage.clear()
        if (!deviceUuid.isNullOrBlank()) storage.saveString("device_uuid", deviceUuid)
    }


    fun getString(key: String, default: String): String =
        storage.getString(key, default) ?: default

    fun saveString(key: String, value: String) =
        storage.saveString(key, value)

    fun getUserPhone(): String? {
        // ✅ storage.getString use karo, not Compose getString
        val phone = storage.getString("phone", "")
        return if (phone.isNullOrBlank()) null else phone
    }


    // ── PARKING HISTORY ──────────────────────────────────────────────
    private val parkingJson = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        isLenient         = true
        coerceInputValues = true
    }

    fun saveParkingEntry(entry: com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingData) {
        val existing = getParkingHistory()
        val updated  = listOf(migrateParkingEntry(entry)) + existing.take(49)  // max 50 entries
        storage.saveString("parking_history", parkingJson.encodeToString(updated))
    }

    fun getParkingHistory(): List<com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingData> {
        val raw = storage.getString("parking_history", "")
        if (raw.isBlank()) return emptyList()
        return runCatching {
            parkingJson.decodeFromString<List<com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingData>>(raw)
                .map { migrateParkingEntry(it) }
        }.getOrElse { emptyList() }
    }

    /**
     * Migrates entries that were stored with old field names (phone / alternate_phone)
     * to the new names (alternate_phone_1 / alternate_phone_2) so both old cached data
     * and new API responses are consistent in local storage.
     */
    private fun migrateParkingEntry(
        entry: com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingData
    ): com.developer.ev2z.ui.screens.VehicleParkingTag.ParkingData {
        return entry.copy(
            alternate_phone_1 = entry.alternate_phone_1 ?: entry.phone,
            alternate_phone_2 = entry.alternate_phone_2 ?: entry.alternate_phone
        )
    }

}


