package com.developer.ev2z.data.local.model



import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlin.math.abs

/**
 * `user/devices/latest-locations` — one request covering the whole fleet.
 *
 * Every field carries a default on purpose. This response is the home map: if one
 * count arrives null, or the gateway drops a key it used to send, a model without
 * defaults throws and the user's whole fleet vanishes from the screen with no error
 * to explain it. That is not hypothetical here — `live-location` nulls
 * `battery_level` per device and did exactly that. Defaults plus `coerceInputValues`
 * (see `ApiJson`) mean a bad field costs that field alone.
 */
@Serializable
data class DeviceResponse(
    val success: Boolean = false,
    @SerialName("user_id") val userId: Int = 0,
    /** The payload sends the account holder twice, under both names. */
    val username: String = "",
    val name: String = "",
    @SerialName("total_devices") val totalDevices: Int = 0,
    @SerialName("running_count") val runningCount: Int = 0,
    @SerialName("idle_count") val idleCount: Int = 0,
    @SerialName("parked_count") val parkedCount: Int = 0,
    @SerialName("offline_count") val offlineCount: Int = 0,
    val data: List<Device> = emptyList()
) {
    /** Whichever of the two spellings of the account holder's name arrived. */
    val displayName: String get() = name.ifBlank { username }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Device(
    @SerialName("device_id") @JsonNames("terminal_id") val deviceId: String = "",
    @SerialName("device_name") val deviceName: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    // Defaulted rather than nullable: the whole app treats "no speed" as 0, and a
    // nullable Int here would push a `?: 0` into every call site instead.
    val speed: Int = 0,
    // Raw server value. Read it through getStatusKind() rather than comparing it
    // directly — the casing and the spelling both vary, and the API sends "parked"
    // (it has a parked_count bucket) on top of the four values listed here before.
    // Seen across the app: on, off, moving, running, idle, ideal, parked, stop,
    // stopped, offline, no_data.
    @JsonNames("db_status", "movement") val status: String = "offline",
    // `timestamp` here, `timestamp_utc` on live-location, `created_at` on route
    // history — the same fix time under three names, so all three are accepted.
    @JsonNames("timestamp_utc", "created_at") val timestamp: String? = null,
    @SerialName("last_seen_minutes_ago") val lastSeenMinutesAgo: Int? = null,
    // Not an alias of the above: this endpoint counts minutes and live-location
    // counts seconds, so the unit has to be kept apart from the name.
    @SerialName("last_seen_seconds_ago") val lastSeenSecondsAgo: Int? = null,
    /**
     * The catalogue id of the vehicle this tracker is fitted to, as `vehicle-types`
     * numbers them — 1..41, null while no vehicle is assigned.
     *
     * The **first** choice for the marker, ahead of the slug below: this is the field
     * `add-device` actually sets, while `icon` and `icon_url` are derived from it
     * server-side and have been seen disagreeing with it. An integer also cannot arrive
     * hyphenated on one endpoint and underscored on another, inside a URL that 404s, or
     * spelled a way this build has never seen. See [resolveVehicleSlug].
     */
    @SerialName("vehicle_id") val vehicleId: Int? = null,
    /** What the catalogue calls it — "Electric Scooter". Not drawn; useful in logs. */
    @SerialName("vehicle_name") val vehicleName: String? = null,
    /** The slug on its own, which saves picking it back out of [iconUrl]. */
    val icon: String? = null,
    /**
     * The server's artwork for the vehicle — see
     * [com.developer.ev2z.data.local.remote.LatestLocation.iconUrl] for why the URL
     * itself is never fetched. The fleet payload gained this field, along with
     * [vehicleId], [vehicleName] and [icon], on 2026-10-13; before that it carried no
     * vehicle at all and every fleet marker was the heading arrow.
     */
    @SerialName("icon_url")
    @JsonNames("vehicle_icon_url")
    val iconUrl: String? = null
) {
    fun getDisplayName(): String = deviceName?.takeIf { it.isNotBlank() } ?: "Device ${deviceId.take(8)}"

    /**
     * Fix age in minutes, whichever unit this endpoint happens to be counting in.
     * Null when it reported neither.
     */
    fun lastSeenMinutes(): Int? = lastSeenMinutesAgo ?: lastSeenSecondsAgo?.let { it / 60 }

    /**
     * The reported coordinates as a real position on earth, or null if they are not one.
     *
     * Validity is decided on the pair, not on each number by itself. A tracker that has
     * not got a fix yet reports `"0"`/`"0"`, which parses perfectly well — so it used to
     * count as a valid location and got drawn as a marker in the Gulf of Guinea. On the
     * live map that marker also went into the auto-fit `LatLngBounds`, stretching the
     * camera from India out into the Atlantic, which left *every* vehicle looking like it
     * was in the wrong place.
     *
     * Parsed once here rather than in each accessor, because `latitude` without
     * `longitude` is not a position and the two must be rejected together.
     */
    private fun validPosition(): Pair<Double, Double>? {
        val lat = latitude?.toDoubleOrNull() ?: return null
        val lng = longitude?.toDoubleOrNull() ?: return null

        // NaN and Infinity do parse, but fail every comparison, so the range check is
        // what rejects them too.
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null

        // Null Island: both axes inside ~110 m of zero is the no-fix sentinel, not
        // somewhere a tracked vehicle reports from.
        if (abs(lat) < NO_FIX_DEGREES && abs(lng) < NO_FIX_DEGREES) return null

        return lat to lng
    }

    fun getLatitudeDouble(): Double? = validPosition()?.first

    fun getLongitudeDouble(): Double? = validPosition()?.second

    fun hasValidLocation(): Boolean = validPosition() != null

    /**
     * The reported status reduced to the buckets the UI actually shows.
     *
     * The vocabulary lives in [statusKindOf] rather than here, because this model is
     * only one of the shapes the same tracker arrives in — the live-location and
     * route-history payloads describe the identical state under different field names
     * and different words, and a spelling only this model recognised would make the
     * fleet map and the live map disagree about one vehicle.
     */
    fun getStatusKind(): String = statusKindOf(status)

    /**
     * The vehicle type — "electric-scooter" — spelled the way `vehicle-types` spells
     * it, so one icon table serves every endpoint.
     *
     * Three sources, in order of how much they can be trusted: [vehicleId] through the
     * catalogue, then [icon], then the slug picked out of [iconUrl]. The id is exact;
     * the URL is the one that has actually gone wrong, spelling the slug with
     * underscores while the files are served with hyphens.
     *
     * Deliberately the same derivation as
     * [com.developer.ev2z.data.local.remote.LatestLocation.iconSlug]: the fleet map and
     * the single-vehicle map must draw the same vehicle for the same tracker, and that
     * only holds while both read the slug the same way. Null when no vehicle is
     * assigned, which the marker draws as the heading arrow.
     */
    val iconSlug: String? get() = resolveVehicleSlug(vehicleId, icon, iconUrl)

    fun getStatusDisplay(): String = statusDisplayOf(status)

    fun getStatusColor(): Long = statusColorOf(status)

    /**
     * How old this device's position is, or null while it is current.
     *
     * The fleet payload reports this per device and the map never showed it, so a pin
     * whose tracker last spoke five days ago sat on the screen looking exactly as live
     * as the vehicle moving right now — on a screen called Live Location. Null below
     * [FRESH_WITHIN_MINUTES], where the age is noise rather than news.
     */
    fun fixAgeLabel(): String? = lastSeenMinutes()
        ?.takeIf { it >= FRESH_WITHIN_MINUTES }
        ?.let { formatFixAge(it) }

    /**
     * Whether this position is old enough that it describes where the vehicle *was*.
     *
     * What the map dims and the list spells out. Ten minutes: the trackers upload every
     * few tens of seconds while they have signal, so anything past that is a tracker
     * that has stopped reporting rather than one between fixes.
     */
    fun isFixStale(): Boolean = (lastSeenMinutes() ?: 0) >= STALE_AFTER_MINUTES

    /**
     * What the fleet map writes beside this vehicle.
     *
     * The registration first, because a fleet of identical arrows is unreadable without
     * it — three grey pins told the owner only that they had three devices. The age
     * follows when the fix is old, so a pin that cannot be trusted says so where it is
     * drawn rather than one tap away.
     */
    fun mapLabel(): String {
        val age = fixAgeLabel() ?: return getDisplayName()
        return "${getDisplayName()} · $age"
    }

    internal companion object {
        /**
         * ~110 m — tight enough that only a genuine 0,0 sentinel falls inside it.
         *
         * Shared with [com.developer.ev2z.data.local.remote.LatestLocation], which has
         * to reject the same sentinel out of the live-location endpoint: the two models
         * describe the same trackers, so a tolerance tuned here must not be re-guessed
         * there.
         */
        const val NO_FIX_DEGREES = 0.001

        /** Below this the fix is simply current, and its age is not worth the pixels. */
        const val FRESH_WITHIN_MINUTES = 2

        /** Past this the position describes where the vehicle was, not where it is. */
        const val STALE_AFTER_MINUTES = 10
    }
}

/**
 * A fix age in minutes, as short as it can be said.
 *
 * Floored, never rounded up: a position 353 minutes old is reported as five hours old,
 * not six. The number is there to be distrusted by, and rounding it the wrong way
 * makes a stale pin sound staler than the data supports.
 */
fun formatFixAge(minutes: Int): String = when {
    minutes <= 0 -> "just now"
    minutes < 60 -> "${minutes}m ago"
    minutes < 60 * 24 -> "${minutes / 60}h ago"
    else -> "${minutes / (60 * 24)}d ago"
}