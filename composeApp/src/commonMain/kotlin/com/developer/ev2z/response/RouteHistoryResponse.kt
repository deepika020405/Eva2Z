package com.developer.ev2z.response

import com.developer.ev2z.data.local.model.vehicleSlugForId

import com.developer.ev2z.data.local.model.statusKindOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RouteHistoryResponse(
    @SerialName("success") val success: Boolean,
    // Route history is the one endpoint that calls a device a "terminal", in both
    // the request and the response. Both spellings are read so the app has a single
    // word for it.
    @SerialName("terminal_id") @JsonNames("device_id") val terminalId: String,

    @SerialName("count")
    val count: Int? = null,

    @SerialName("average_speed")
    val averageSpeed: Double? = 0.0,

    /**
     * The server's artwork for the vehicle this terminal is fitted to — the same field
     * `live-location` sends as `icon_url`, under a third name.
     *
     * Not fetched, and [vehicleIconSlug] is the part worth using. Checked again on
     * 2026-10-12: the URL as sent spells the slug with underscores
     * (`electric_scooter.svg`) while the files are served with hyphens, so it answers
     * 404 verbatim; and every file that *does* resolve is the same 96×96 SVG — a grey
     * rounded square wrapping one car emoji (`&#128663;`) — with only its `<title>`
     * differing, so a motorcycle and a car download byte-for-byte the same picture.
     * Drawing from the slug gives each vehicle type its own glyph, needs no SVG
     * decoder (the app has none, on either platform) and works offline.
     */
    /**
     * The catalogue id of the vehicle, as `vehicle-types` numbers them — the first
     * choice for the playback marker, ahead of the URL below. See `vehicleSlugForId`.
     */
    @SerialName("vehicle_id")
    val vehicleId: Int? = null,

    @SerialName("vehicle_icon_url")
    val vehicleIconUrl: String? = null,


    @SerialName("data")
    val data: List<RoutePoint>
) {
    /**
     * The vehicle type's name out of [vehicleIconUrl] — "electric-scooter" — in the
     * spelling `vehicle-types` uses, so one lookup table serves every endpoint.
     *
     * Deliberately identical to `LatestLocation.iconSlug`: both endpoints describe the
     * same vehicle, and a marker that changes shape between the live map and playback
     * would read as a different vehicle.
     */
    val vehicleIconSlug: String?
        get() = vehicleSlugForId(vehicleId)
            ?: vehicleIconUrl
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                ?.replace('_', '-')
                ?.takeIf { it.isNotBlank() }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RoutePoint(
    @SerialName("latitude") val latitude: String,
    @SerialName("longitude") val longitude: String,
    // Speed is a string here and an Int on the live endpoints, so it is parsed
    // through getSpeedInt() rather than used as it arrives.
    @SerialName("speed") val speed: String? = null,
    // The fix time, which this endpoint calls `created_at` even though it is when
    // the tracker was there, not when the row was written.
    @SerialName("created_at") @JsonNames("timestamp", "timestamp_utc") val createdAt: String,
    // Motion state, called `movement` only here. Read it through [statusKind].
    @SerialName("movement") @JsonNames("status", "db_status") val movement: String
) {
    fun getLatDouble(): Double? = latitude.toDoubleOrNull()
    fun getLngDouble(): Double? = longitude.toDoubleOrNull()
    fun getSpeedInt(): Int = speed?.toDoubleOrNull()?.toInt() ?: 0

    /** The motion state in the app's own vocabulary — the same words every other model uses. */
    fun statusKind(): String = statusKindOf(movement)

    fun isValid(): Boolean {
        val lat = getLatDouble() ?: return false
        val lng = getLngDouble() ?: return false
        return lat != 0.0 && lng != 0.0 &&
                lat in -90.0..90.0 &&
                lng in -180.0..180.0
    }
}