package com.developer.ev2z.response

import com.developer.ev2z.data.local.model.resolveVehicleSlug
import com.developer.ev2z.data.local.model.statusKindOf
import com.developer.ev2z.data.local.remote.LatestLocation
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// `user/devices/latest-locations` was modelled twice — once here as
// DeviceResponse/DeviceItem and once as data.local.model.DeviceResponse/Device.
// Only the latter was ever parsed; this pair was dead, and a second model of one
// endpoint is how the two drift apart unnoticed. Removed 2026-10-10.

// ---------------------- MyDevicesResponse (Devices List) ----------------------

@Serializable
data class MyDevicesResponse(
    val success: Boolean = false,
    @SerialName("total_devices")
    val total_devices: Int = 0,
    val devices: List<Device> = emptyList(),
    val username: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Device(
    val id: Int = 0,
    @SerialName("device_id")
    @JsonNames("terminal_id")
    val device_id: String = "",
    @SerialName("device_name")
    val device_name: String = "",
    // The SIM is `sim_no` in this response but `sim_number` in the add-device
    // request — same field, two names, so both are read.
    @SerialName("sim_no")
    @JsonNames("sim_number")
    val sim_no: String? = null,
    @SerialName("device_type")
    val device_type: String? = null,
    // Ignition, not motion — "on"/"off"/"unknown". Deliberately NOT aliased to
    // `status`: this endpoint sends engine_status "unknown" with status "moving",
    // so folding them together would report a running vehicle as unknown.
    @SerialName("engine_status")
    val engine_status: String = "offline",
    @JsonNames("db_status", "movement")
    val status: String? = null,
    @SerialName("created_at")
    val created_at: String = "",
    /**
     * The catalogue id of the vehicle this tracker is fitted to, as `vehicle-types`
     * numbers them — 1..41, null while no vehicle is assigned.
     *
     * Carried so the single-vehicle maps can draw the right vehicle at all.
     * `live-location/{id}` — which is what Track & Trace and Device Report actually
     * poll — sends no `vehicle_id` and a null `icon_url`, so those screens had nothing
     * to identify the vehicle with and fell back to the plain heading arrow. The fleet
     * endpoint they *also* poll for the dropdown does send it; it was being dropped in
     * the mapping into this model rather than being unavailable.
     *
     * Same three fields and the same precedence as
     * [com.developer.ev2z.data.local.model.Device], deliberately: the fleet map and the
     * single-vehicle maps have to draw one tracker as one vehicle.
     */
    @SerialName("vehicle_id")
    val vehicle_id: Int? = null,
    /** The slug on its own, as `latest-locations` sends it beside the id. */
    val icon: String? = null,
    @SerialName("icon_url")
    @JsonNames("vehicle_icon_url")
    val icon_url: String? = null
) {
    /**
     * The motion state in the app's own vocabulary, preferring the real motion field
     * and falling back to ignition when the endpoint sent only that.
     */
    fun statusKind(): String = statusKindOf(status ?: engine_status)

    /**
     * The vehicle type — "electric-scooter" — spelled the way `vehicle-types` spells it,
     * so one icon table serves every endpoint. See [resolveVehicleSlug] for why the slug
     * is preferred over `vehicle_id`.
     */
    val iconSlug: String? get() = resolveVehicleSlug(vehicle_id, icon, icon_url)
}

/**
 * The vehicle to draw for the single-vehicle maps, which have two descriptions of one
 * tracker and have to pick the right field out of both.
 *
 * Neither source alone is enough. `live-location/{id}` — what Track & Trace and Device
 * Report poll for position — sends **no** `vehicle_id`, only `icon_url`; the dropdown's
 * [Device], built from `latest-locations`, is the one carrying the id.
 *
 * So the fields are merged before the precedence is applied, rather than one whole
 * source being preferred over the other. Taking the live fix's slug first is exactly the
 * bug: on device 0869727076942661 that URL says `motorcycle.svg` while the fleet's
 * `vehicle_id` 1 correctly says Car, so whole-source precedence drew a bike for a car
 * however the id was set. An id beats a slug wherever each of them came from.
 */
internal fun markerVehicleSlug(fix: LatestLocation?, device: Device?): String? =
    resolveVehicleSlug(
        vehicleId = fix?.vehicleId ?: device?.vehicle_id,
        icon = fix?.icon ?: device?.icon,
        iconUrl = fix?.iconUrl ?: device?.icon_url,
    )
