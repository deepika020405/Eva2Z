package com.developer.ev2z.data.local

/**
 * Which kind of tracker each device is — "wired" or "wireless".
 *
 * Only `devices/my-devices` reports `device_type`. The fleet endpoint the live screens
 * poll (`user/devices/latest-locations`) does not send it, and
 * [com.developer.ev2z.repository.DeviceReportRepository.getMyDevices] builds its device
 * list out of that one — so the screens that must not offer engine controls on a wireless
 * tracker (there is no relay wired to the ignition to cut) had nothing to decide on.
 *
 * Kept per device id rather than once for the account: one login can own a wired car and
 * a wireless bike, and the answer belongs to the device, not the user.
 */
object DeviceTypeStore {

    // Lazy because on Android createStorageManager() needs the Context MainActivity
    // installs, and this is touched from a composable's first frame.
    private val storage: StorageManager by lazy { createStorageManager() }

    fun save(deviceId: String, deviceType: String) {
        if (deviceId.isBlank() || deviceType.isBlank()) return
        storage.saveString(key(deviceId), deviceType.trim().lowercase())
    }

    fun saveAll(types: Map<String, String>) {
        types.forEach { (deviceId, deviceType) -> save(deviceId, deviceType) }
    }

    /** The stored type, or null for a device this install has never seen a type for. */
    fun get(deviceId: String): String? =
        storage.getString(key(deviceId), "").takeIf { it.isNotBlank() }

    private fun key(deviceId: String) = "device_type_$deviceId"
}

/**
 * Whether a `device_type` names a wireless tracker.
 *
 * Substring, not equality: the field arrives as "Wireless" from the add-device form and
 * lowercase from the API, and the shop spells the same thing "wireless gps". Null — a
 * device whose type nobody has told us — is deliberately **not** wireless: an unknown
 * type must leave the engine controls as they were rather than silently removing them.
 */
fun isWirelessDeviceType(deviceType: String?): Boolean =
    deviceType?.trim()?.lowercase()?.contains("wireless") == true
