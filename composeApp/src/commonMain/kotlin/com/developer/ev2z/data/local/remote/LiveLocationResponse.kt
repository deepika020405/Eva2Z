package com.developer.ev2z.data.local.remote

import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.isMotionKind
import com.developer.ev2z.data.local.model.statusKindOf
import com.developer.ev2z.data.local.model.vehicleSlugForId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.math.abs

@Serializable
data class LiveLocationResponse(
    val success: Boolean,
    val data: LiveLocationData? = null
)

@Serializable
data class LiveLocationData(
    @SerialName("latest_location")
    val latestLocation: LatestLocation? = null,
    @SerialName("status_count")
    val statusCount: StatusCount? = null
)

/**
 * One fix off `live-location/{deviceId}`.
 *
 * Every field is nullable or carries a default because the server omits and nulls
 * them per device: a wired tracker with no battery sends `"battery_level":null`, and
 * a non-nullable `String` made that one null throw
 * `Expected string literal but 'null' literal was found`, which threw away the whole
 * response — coordinates included — and left the screen on the previously selected
 * device. A default alone does not cover this: it fills a *missing* key, not an
 * explicit null, which is why `ApiJson` (coerceInputValues) parses this type.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LatestLocation(
    @SerialName("device_id")
    @JsonNames("terminal_id")
    val deviceId: String = "",
    val latitude: String = "0",
    val longitude: String = "0",
    val speed: Int = 0,
    /**
     * Motion state. `db_status` rides alongside it with a different word for the same
     * thing ("running" vs "moving"), so read it through [statusKind] rather than
     * comparing the raw string.
     */
    val status: String = "offline",
    @SerialName("db_status")
    val dbStatus: String = "",
    /**
     * The server's artwork for the vehicle this tracker is fitted to — null on a
     * device with no vehicle assigned, which is most of them while `add-device`
     * cannot store a `vehicle_id`.
     *
     * Nullable for the same reason `battery_level` is: the server nulls this field
     * per device rather than omitting it, and a non-nullable `String` would make one
     * unassigned device throw away its own coordinates.
     *
     * Two caveats before drawing it. The URL as sent spells the slug with
     * underscores (`electric_scooter.svg`) while the files are served with hyphens
     * (`electric-scooter.svg`), so the link 404s as given — [iconSlug] is the part
     * worth using. And each file is an SVG wrapping a single emoji glyph, which the
     * app has no decoder for; icons are drawn from the slug instead.
     *
     * **Both names are read**, which is belt and braces rather than a fix. Checked
     * against the live endpoint on 2026-10-13: this one really does send `icon_url`
     * and route history really does send `vehicle_icon_url`, so each model already had
     * its own endpoint's spelling right. The alias is here because the two names for
     * one field are exactly the kind of thing a gateway swaps, and because these models
     * get copied from each other.
     *
     * What can be null is the **value**: a device with no vehicle assigned sends
     * `"icon_url": null`, and the marker is then the heading arrow for want of data
     * rather than for want of parsing.
     */
    /**
     * The catalogue id of the vehicle, as `vehicle-types` numbers them.
     *
     * Read here even though this endpoint does **not** send it yet — checked on
     * 2026-10-13, where `latest-locations` and `route-history` both do and this one
     * still carries only `icon_url`. Reading it costs nothing and means the better key
     * is used the day the gateway adds it, with no release. See `vehicleSlugForId`.
     */
    @SerialName("vehicle_id")
    val vehicleId: Int? = null,
    /** The slug on its own, if this endpoint ever sends it the way the fleet one does. */
    val icon: String? = null,
    @SerialName("icon_url")
    @JsonNames("vehicle_icon_url")
    val iconUrl: String? = null,
    /**
     * UTC fix time. This endpoint calls it `timestamp_utc`, the fleet endpoint calls
     * it `timestamp` and route history calls it `created_at`; all three spellings are
     * accepted here so a gateway rename cannot blank the fix time again.
     */
    @SerialName("timestamp_utc")
    @JsonNames("timestamp", "created_at")
    val timestamp: String = "",
    /** The same instant preformatted in IST — a different value, not an alias. */
    @SerialName("timestamp_ist")
    val timestampIst: String = "",
    /** Percentage as text, or null on a tracker that reports no battery at all. */
    @SerialName("battery_level")
    val batteryLevel: String? = null,
    @SerialName("battery_display")
    val batteryDisplay: String = "",
    @SerialName("battery_status")
    val batteryStatus: String = "",
    // Age is the one divergence an alias cannot absorb: the two endpoints disagree
    // about the *unit*, not just the name, so both are read and [lastSeenMinutesAgo]
    // converts. Aliasing them would have reported a 90-second-old fix as 90 minutes.
    @SerialName("last_seen_seconds_ago")
    val lastSeenSecondsAgo: Int? = null,
    @SerialName("last_seen_minutes_ago")
    val lastSeenMinutes: Int? = null
) {
    /**
     * Fix age in whole minutes, from whichever unit the server sent. Rounds down, so
     * a fix 9 seconds old reads 0 — which is what "just now" should say.
     */
    val lastSeenMinutesAgo: Int
        get() = lastSeenMinutes ?: lastSeenSecondsAgo?.let { it / 60 } ?: 0

    /** The motion state in the app's own vocabulary. Prefers `status` over `db_status`. */
    fun statusKind(): String = statusKindOf(status.ifBlank { dbStatus })

    /**
     * The state with the connection taken out of it: whether the vehicle is *moving*.
     *
     * [statusKind] prefers `status` and only reads `db_status` when `status` is blank,
     * which loses the motion state on every device this endpoint reports as offline —
     * it sends `"status":"offline"` alongside `"db_status":"parked"`, so the map
     * painted a parked vehicle grey instead of red. The two fields answer different
     * questions: `status` is whether the tracker is reporting, `db_status` is what the
     * vehicle is doing.
     *
     * Falls back to [statusKind] when neither field mentions motion, so a device that
     * really only reports a connection still gets its grey.
     */
    fun motionKind(): String {
        statusKindOf(status).let { if (isMotionKind(it)) return it }
        statusKindOf(dbStatus).let { if (isMotionKind(it)) return it }
        return statusKind()
    }

    /**
     * The vehicle type's name out of [iconUrl] — "electric-scooter" — in the same
     * spelling `vehicle-types` uses, so one lookup table serves both endpoints.
     *
     * Underscores are folded to hyphens because the two endpoints disagree about
     * which they use for the identical slug, and this endpoint's spelling is the one
     * that does not resolve. Null when no vehicle is assigned.
     */
    val iconSlug: String?
        get() = vehicleSlugForId(vehicleId)
            ?: icon?.trim()?.replace('_', '-')?.takeIf { it.isNotBlank() }
            ?: iconUrl
                ?.substringAfterLast('/')
                ?.substringBeforeLast('.')
                ?.replace('_', '-')
                ?.takeIf { it.isNotBlank() }

    /**
     * The coordinates, or null when this fix carries no usable position.
     *
     * A tracker with no satellite fix still reports in, with "0"/"0" in these fields —
     * which is a real place in the Gulf of Guinea. Fed to the map that teleports the
     * marker thousands of kilometres and back again on the next good fix, so every
     * caller must go through here rather than calling `toDoubleOrNull()` itself.
     *
     * The same check as [Device.validPosition] and deliberately to the same tolerance:
     * both read positions off the same trackers, one per device and one for the whole
     * fleet, so a reading the live map rejects must not be one the fleet map draws.
     */
    fun validPosition(): Pair<Double, Double>? {
        val lat = latitude.toDoubleOrNull() ?: return null
        val lng = longitude.toDoubleOrNull() ?: return null

        // NaN and Infinity parse but fail every comparison, so the range check rejects
        // them too.
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null

        // Null Island: both axes within ~110 m of zero is the no-fix sentinel, not
        // anywhere a tracked vehicle reports from.
        if (abs(lat) < Device.NO_FIX_DEGREES && abs(lng) < Device.NO_FIX_DEGREES) return null

        return lat to lng
    }
}

@Serializable
data class StatusCount(
    val idle: Int = 0,
    val moving: Int = 0,
    val parked: Int = 0,
    val offline: Int = 0
)
//package com.developer.ev2z.data.local.remote
//
//
//
//import kotlinx.serialization.SerialName
//import kotlinx.serialization.Serializable
//
//@Serializable
//data class LiveLocationResponse(
//    val success: Boolean,
//    val data: LiveLocationData? = null
//)
//
//@Serializable
//data class LiveLocationData(
//    @SerialName("latest_location")
//    val latestLocation: LatestLocation? = null,
//    @SerialName("status_count")
//    val statusCount: StatusCount? = null
//)
//
//@Serializable
//data class LatestLocation(
//    @SerialName("device_id") val deviceId: String,
//    val latitude: String,
//    val longitude: String,
//    val speed: Int,
//    val status: String,
//    val timestamp: String,
//    @SerialName("battery_level") val batteryLevel: String,
//    @SerialName("battery_display") val batteryDisplay: String,
//    @SerialName("battery_status") val batteryStatus: String,
//    @SerialName("last_seen_minutes_ago") val lastSeenMinutesAgo: Int
//)
//
//@Serializable
//data class StatusCount(
//    val idle: Int,
//    val moving: Int,
//    val parked: Int,
//    val offline: Int
//)