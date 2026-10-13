package com.developer.ev2z.network

import kotlinx.serialization.json.Json

/**
 * The one JSON configuration every API response is parsed with.
 *
 * The gateway describes the same thing differently on different endpoints — a fix
 * time is `timestamp` on `user/devices/latest-locations`, `timestamp_utc` on
 * `live-location/{id}` and `created_at` on `route-history`; a device is `device_id`
 * in most places and `terminal_id` in route history — and it nulls fields per device
 * rather than omitting them. Models absorb the renames with `@JsonNames`; this
 * handles the rest:
 *
 * - `ignoreUnknownKeys` — a key the server adds (`db_status`, `timestamp_ist`) must
 *   not fail a response the app already understands.
 * - `coerceInputValues` — an explicit `null` on a non-nullable field falls back to
 *   that field's default instead of throwing. Without it one null
 *   `battery_level` discarded an entire live-location response, coordinates
 *   included, and the map silently stayed on the previously selected device.
 * - `encodeDefaults` — request bodies must carry their defaults; several endpoints
 *   reject a body with the field missing.
 *
 * Use this rather than a local `Json { ignoreUnknownKeys = true }`: a tolerance set
 * in one call site is a tolerance the next endpoint does not get.
 */
val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
}
