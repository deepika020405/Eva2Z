package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The tracker's own event log — what the device reported happening, and when.
 *
 * ```
 * { "success": true,
 *   "data": [ { "id": 3528316, "device_id": "0863656047646760", "event": "OFF",
 *               "alarm_type": "Ignition OFF", "latitude": 26.960119, "longitude": 81.000089,
 *               "speed": 0, "occurred_at_utc": "2026-10-28T09:16:07Z",
 *               "occurred_at_ist": "2026-10-28 14:46:07" } ],
 *   "meta": { "current_page": 1, "per_page": 3, "has_more": false } }
 * ```
 *
 * This is a different kind of answer from everything else the app polls: the fleet and
 * live-location endpoints describe a vehicle's *state right now*, and this one is a list
 * of *moments*. That is what makes an engine on/off notification possible at all —
 * `latest-locations` calls an engine-on standstill "parked" (see [com.developer.ev2z.util.IdleStatusResolver]),
 * so the only place ignition is stated plainly is here, in `alarm_type`.
 *
 * Every field defaults, per the house rule: one field the gateway renames or nulls must
 * cost that field and not the whole log. The error-status check lives in
 * [com.developer.ev2z.network.ApiService], which is where a throttle page is stopped from
 * being decoded as an empty list — see `RateLimit.kt`.
 */
@Serializable
data class DeviceAlarmsResponse(
    val success: Boolean = false,
    val data: List<DeviceAlarm> = emptyList(),
    val meta: DeviceAlarmPage? = null,
)

/** One recorded event. */
@Serializable
data class DeviceAlarm(
    /**
     * The server's row id, and the app's de-duplication key: it is what tells an event
     * already announced from one that just happened. Rising, so the newest row is the
     * largest id.
     */
    val id: Long = 0,
    @SerialName("device_id") val deviceId: String = "",
    /** The bare state — "ON" / "OFF" — behind [alarmType]. */
    val event: String? = null,
    /**
     * What happened, in words: "Ignition OFF", "Ignition ON", and whatever else this
     * tracker reports (SOS, tow, overspeed). Read it through [ignitionEventOf] rather
     * than comparing it directly — casing and spelling vary across this account's
     * endpoints, which is the same lesson as `Device.getStatusKind()`.
     */
    @SerialName("alarm_type") val alarmType: String? = null,
    /**
     * Numbers here, unlike `latest-locations`, which sends its coordinates as strings.
     * Nullable because an event raised while the tracker had no fix carries none.
     */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speed: Int = 0,
    @SerialName("occurred_at_utc") val occurredAtUtc: String? = null,
    /** Already in IST — no conversion, just formatting. See [clockTimeOf]. */
    @SerialName("occurred_at_ist") val occurredAtIst: String? = null,
)

@Serializable
data class DeviceAlarmPage(
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
)

/** Ignition, as the app talks about it. Everything else in the log is not this. */
enum class IgnitionEvent { ON, OFF }

/**
 * Whether [alarmType] (with [event] behind it) is an ignition event, and which way.
 *
 * Null for every other kind of alarm — an SOS or a tow alert must not raise an engine
 * notification, and a tracker that reports a type this app has never seen is left alone
 * rather than guessed at.
 *
 * Matching is deliberately loose about form and strict about meaning: `"Ignition OFF"`,
 * `"IGNITION_OFF"`, `"ignition off"` and the `"ACC OFF"` some trackers send all mean the
 * same thing, and this account has already shown that the same concept arrives spelled
 * several ways from one server (see the device-status vocabulary across
 * `latest-locations` and `status-details`).
 *
 * [event] is the fallback, not the primary: on a row whose `alarm_type` is about ignition
 * but unparsable, a bare `"ON"`/`"OFF"` still decides it. A bare `"ON"` with no ignition
 * wording anywhere decides nothing — "ON" alone is not necessarily the engine.
 */
fun ignitionEventOf(alarmType: String?, event: String? = null): IgnitionEvent? {
    val type = alarmType?.lowercase()?.replace('_', ' ')?.trim().orEmpty()

    val isIgnition = type.contains("ignition") || type.contains("acc")
    if (!isIgnition) return null

    // The word naming the line is removed before the state is looked for, because
    // "ignition" *contains* "on": left in, `"Ignition"` with `event:"OFF"` behind it came
    // out as ON — an engine-started banner for an engine that had just stopped.
    val state = type.replace("ignition", " ").replace("acc", " ")

    // Read the state off the type itself first — it is the field that says so.
    if (state.contains("off")) return IgnitionEvent.OFF
    if (state.contains("on")) return IgnitionEvent.ON

    return when (event?.lowercase()?.trim()) {
        "off", "0", "false" -> IgnitionEvent.OFF
        "on", "1", "true" -> IgnitionEvent.ON
        else -> null
    }
}

/**
 * `"2026-10-28 14:46:07"` → `"02:46 PM"`, for the line under the notification.
 *
 * A string read rather than a date parse: `occurred_at_ist` is already in the zone the
 * user reads times in, so parsing it to an instant and back would only risk re-applying
 * an offset to a stamp that has one. Empty when the stamp is missing or not this shape —
 * the caller drops the time and keeps the sentence.
 */
fun clockTimeOf(occurredAtIst: String?): String {
    val time = occurredAtIst?.substringAfter(' ', "")?.trim().orEmpty()
    val hour = time.substringBefore(':', "").toIntOrNull() ?: return ""
    val minute = time.split(':').getOrNull(1)?.toIntOrNull() ?: return ""
    if (hour !in 0..23 || minute !in 0..59) return ""

    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val suffix = if (hour < 12) "AM" else "PM"

    return "${hour12.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $suffix"
}
