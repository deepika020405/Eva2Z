package com.developer.ev2z.data.local.model



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class DeviceResponse(
    val success: Boolean,
    @SerialName("user_id") val userId: Int,
    val name: String,
    @SerialName("total_devices") val totalDevices: Int,
    @SerialName("running_count") val runningCount: Int,
    @SerialName("idle_count") val idleCount: Int,
    @SerialName("parked_count") val parkedCount: Int,
    @SerialName("offline_count") val offlineCount: Int,
    val data: List<Device>
)

@Serializable
data class Device(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String?,
    val latitude: String?,
    val longitude: String?,
    val speed: Int,
    // Raw server value. Read it through getStatusKind() rather than comparing it
    // directly — the casing and the spelling both vary, and the API sends "parked"
    // (it has a parked_count bucket) on top of the four values listed here before.
    // Seen across the app: on, off, moving, running, idle, ideal, parked, stop,
    // stopped, offline, no_data.
    val status: String,
    val timestamp: String?,
    @SerialName("last_seen_minutes_ago") val lastSeenMinutesAgo: Int?
) {
    fun getDisplayName(): String = deviceName?.takeIf { it.isNotBlank() } ?: "Device ${deviceId.take(8)}"

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
     * Normalised first, because the raw `when (status)` this replaces matched
     * lower-case literals against the string exactly as the server sent it: a
     * `"Parked"` or `"PARKED"` matched nothing, fell through to the `else`, and so
     * carried the grey "unknown" dot while the label still read something plausible —
     * the status looked wrong without looking broken.
     *
     * The vocabulary is wider than the four values this model's comment used to claim.
     * `TrackTraceScreen`, `DeviceReportScreen` and `HistoryPlaybackScreen` all accept
     * `on`/`off` and the `running`/`ideal` spellings as well, so the live map accepts
     * the same set rather than being the one screen that does not recognise a status
     * the rest of the app does.
     */
    fun getStatusKind(): String = when (status.trim().lowercase()) {
        "on", "moving", "running" -> "running"
        "idle", "ideal" -> "idle"
        "parked" -> "parked"
        // Ignition off is a stop, not a park: only the server decides a vehicle is
        // parked, and calling every "off" a park would invent that distinction.
        "off", "stop", "stopped" -> "stopped"
        "offline" -> "offline"
        "no_data", "nodata" -> "no_data"
        else -> "unknown"
    }

    fun getStatusDisplay(): String = when (getStatusKind()) {
        "running" -> "Running"
        "idle" -> "Idle"
        "parked" -> "Parked"
        "stopped" -> "Stopped"
        "offline" -> "Offline"
        "no_data" -> "No Data"
        else -> status.trim().replaceFirstChar { it.uppercase() }
    }

    fun getStatusColor(): Long = when (getStatusKind()) {
        "running" -> 0xFF4CAF50  // Green
        "idle" -> 0xFFFFC107     // Yellow
        "parked" -> 0xFFF44336   // Red, matching the Parked swatch in the overview sheet
        "stopped" -> 0xFFF44336  // Red — a stop reads the same as a park on the map
        "offline" -> 0xFFBDBDBD  // Gray
        "no_data" -> 0xFF9E9E9E  // Light Gray
        else -> 0xFF9E9E9E
    }

    private companion object {
        /** ~110 m — tight enough that only a genuine 0,0 sentinel falls inside it. */
        const val NO_FIX_DEGREES = 0.001
    }
}