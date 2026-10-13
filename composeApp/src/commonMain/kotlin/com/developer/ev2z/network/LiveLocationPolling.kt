package com.developer.ev2z.network

import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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

/**
 * How often `user/devices/latest-locations` is polled.
 *
 * One request covers the whole fleet, and three screens poll it — the live map, the
 * Track & Trace dropdown and the Device Report dropdown — so it lives here rather than
 * being spelled out in each of them, as two of the three used to.
 *
 * Unlike the live-location interval this does **not** set the pace of anything the eye
 * follows: Home's map markers glide on their own animation, and the Running/Stopped
 * verdict is deliberately decided over [com.developer.ev2z.ui.screens.homeScreen.MOVEMENT_WINDOW_MS]
 * instead of over one poll, so this can be retuned without changing what either shows.
 */
const val DEVICES_POLL_INTERVAL_MS = 5_000L

/**
 * Runs [tick] forever, once per [intervalMs] measured from the *start* of each tick.
 *
 * A plain `tick(); delay(interval)` loop has a period of interval **plus** request
 * latency, so fixes land every ~10.4 s while the marker animation is paced for 10 s: the
 * marker completes its glide and then stands still for the remainder of every single
 * cycle, which reads as a periodic hitch rather than as motion. Subtracting the time
 * the request took keeps arrivals on a steady cadence.
 *
 * A tick slower than the interval simply starts the next one immediately instead of
 * queueing up overlapping requests — two live-location calls in flight at once can
 * complete out of order, and the older response landing last jumps the marker
 * backwards.
 */
suspend fun pollAtFixedRate(
    intervalMs: Long = LIVE_LOCATION_POLL_INTERVAL_MS,
    tick: suspend () -> Unit
) {
    while (currentCoroutineContext().isActive) {
        val startedAt = getTimeMillis()
        tick()
        val remaining = intervalMs - (getTimeMillis() - startedAt)
        if (remaining > 0) delay(remaining)
    }
}
