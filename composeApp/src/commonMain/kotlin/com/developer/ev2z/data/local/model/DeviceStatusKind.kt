package com.developer.ev2z.data.local.model

/**
 * The one place a tracker's reported state is turned into the app's vocabulary.
 *
 * Every endpoint names this differently and spells it differently: `status`
 * ("moving") and `engine_status` ("unknown") on `devices/my-devices`, `status`
 * ("running") plus `db_status` ("moving") on `live-location/{id}`, `status`
 * ("offline") on `user/devices/latest-locations`, and `movement` ("parked") on
 * `route-history`. Casing varies too.
 *
 * Comparing a raw value against a literal therefore fails silently — it falls through
 * to "unknown", which renders a grey dot next to a label that still reads plausibly,
 * so the status looks wrong without looking broken. Every model routes through here
 * instead; a spelling added here is understood everywhere at once.
 */
fun statusKindOf(raw: String?): String = when (raw?.trim()?.lowercase()) {
    null, "" -> "unknown"
    "on", "moving", "move", "running" -> "running"
    "idle", "ideal" -> "idle"
    "parked", "park" -> "parked"
    // Ignition off is a stop, not a park: only the server decides a vehicle is
    // parked, and calling every "off" a park would invent that distinction.
    "off", "stop", "stopped" -> "stopped"
    "offline" -> "offline"
    "no_data", "nodata" -> "no_data"
    else -> "unknown"
}

/**
 * Whether a kind out of [statusKindOf] says anything about the vehicle *moving*.
 *
 * "offline", "no_data" and "unknown" answer a different question — whether the
 * tracker is reporting at all — and a payload routinely carries one of those in one
 * field and a real motion state in another: `live-location` sends
 * `"status":"offline"` next to `"db_status":"parked"`. Picking the first non-empty
 * field therefore loses the motion state, which is what the map colour is about.
 */
fun isMotionKind(kind: String): Boolean = kind !in CONNECTIVITY_KINDS

/**
 * Whether the vehicle is actually travelling.
 *
 * What the map markers key off: a moving vehicle is drawn in the glyph's own colours,
 * because that is what the vehicle really looks like, and anything that is not moving
 * is tinted so it reads as stopped at a glance — red for a stop or a park, yellow for
 * idle, grey for a tracker that has gone quiet (see [statusColorOf]).
 *
 * Takes a raw server value or a kind out of [statusKindOf]; the mapping is idempotent,
 * so both work.
 */
fun isRunningKind(raw: String?): Boolean = statusKindOf(raw) == "running"

private val CONNECTIVITY_KINDS = setOf("offline", "no_data", "unknown")

/** The label the UI shows for a kind out of [statusKindOf]. */
fun statusDisplayOf(raw: String?): String = when (val kind = statusKindOf(raw)) {
    "running" -> "Running"
    "idle" -> "Idle"
    "parked" -> "Parked"
    "stopped" -> "Stopped"
    "offline" -> "Offline"
    "no_data" -> "No Data"
    // An unrecognised value is shown as the server wrote it rather than as "Unknown",
    // so a new spelling is visible on screen instead of hiding behind a generic word.
    else -> raw?.trim()?.replaceFirstChar { it.uppercase() }?.takeIf { it.isNotEmpty() }
        ?: kind.replaceFirstChar { it.uppercase() }
}

/** The dot/badge colour for a kind out of [statusKindOf]. */
fun statusColorOf(raw: String?): Long = when (statusKindOf(raw)) {
    "running" -> 0xFF4CAF50  // Green
    "idle" -> 0xFFFFC107     // Yellow
    "parked" -> 0xFFF44336   // Red, matching the Parked swatch in the overview sheet
    "stopped" -> 0xFFF44336  // Red — a stop reads the same as a park on the map
    "offline" -> 0xFFBDBDBD  // Gray
    "no_data" -> 0xFF9E9E9E  // Light Gray
    else -> 0xFF9E9E9E
}
