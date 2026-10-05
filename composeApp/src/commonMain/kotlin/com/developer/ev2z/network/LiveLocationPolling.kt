package com.developer.ev2z.network

/**
 * How often `live-location/{deviceId}` is polled.
 *
 * Shared by every screen that tracks a vehicle — Track & Trace and Device Report — so
 * the two never drift apart, and by the marker animation, which paces itself to this
 * interval: a marker animation shorter than the gap between fixes draws a whole
 * interval of travel in a fraction of a second and then freezes until the next
 * response, which reads as a stuttering crawl rather than movement.
 *
 * Each polling screen spends one request per interval per selected device, so halving
 * this doubles the request rate on `gps.eva2z.in`.
 */
const val LIVE_LOCATION_POLL_INTERVAL_MS = 5_000L