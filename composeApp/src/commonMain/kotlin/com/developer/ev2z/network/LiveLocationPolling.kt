package com.developer.ev2z.network

import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CancellationException
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
 *
 * **This is a server budget, not a smoothness dial.** `gps.eva2z.in` runs Laravel's
 * `throttle` middleware, and at 1 s this endpoint alone asks 60 times a minute — the
 * whole budget, before the fleet poll below has asked for anything. The app then spent
 * minutes at a time taking 429s (seen 2026-10-26 17:54:13 onwards: every request on the
 * host throttled, the home map emptied, and tracking looked switched off). At 5 s the
 * two endpoints together come to roughly 24 requests a minute, which the server has
 * always accepted.
 *
 * Nothing the eye follows is paced by this: the marker glides between fixes on its own
 * animation and walks the snapped road with [com.developer.ev2z.util.rememberRoadWalk],
 * so a longer interval costs accuracy between fixes, not frame rate. If this is raised
 * again, watch for `⏳ Rate limited` in the log — that is the budget being exceeded.
 */
const val
        LIVE_LOCATION_POLL_INTERVAL_MS = 1_000L

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
 *
 * Same budget warning as [LIVE_LOCATION_POLL_INTERVAL_MS]: this was at 1 s while a
 * vehicle was also being tracked at 1 s, which is twice what the server allows. The
 * three screens that poll it now share one request per interval — see
 * [SharedFleetLocations] — so the cost is per interval rather than per open screen.
 */
const val DEVICES_POLL_INTERVAL_MS = 1_000L

/**
 * How long one fleet response is handed to other callers before it is re-fetched.
 *
 * Just under the interval, so a poller arriving on time still gets a fresh request
 * while a *second* poller a moment later reuses that answer rather than paying for its
 * own. At exactly the interval the first poller of each round would be served stale
 * data and the fleet would update at half the rate it claims to.
 */
internal const val DEVICES_SHARE_WINDOW_MS = DEVICES_POLL_INTERVAL_MS * 9 / 10

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
 *
 * Two things keep the loop alive that it used to get wrong, both of which stopped
 * tracking for the rest of the screen's life:
 *
 *  - **It waits on [RateLimitGate].** While the server is throttling the app, spending
 *    another request only extends the throttle window. One 429 anywhere parks every
 *    poller until the server says it will answer again.
 *  - **A throwing tick does not kill it.** `tick()` was called bare, so one exception
 *    escaping a tick — a decode failure on an error body, say — ended the `while` loop
 *    permanently. Nothing restarts it, so the screen kept its last fix forever and
 *    looked exactly like a tracker that had gone offline.
 */
suspend fun pollAtFixedRate(
    intervalMs: Long = LIVE_LOCATION_POLL_INTERVAL_MS,
    tick: suspend () -> Unit
) {
    while (currentCoroutineContext().isActive) {
        RateLimitGate.awaitOpen()

        val startedAt = getTimeMillis()

        try {
            tick()
        } catch (e: CancellationException) {
            // The screen is gone, or the job was replaced. Not ours to absorb.
            throw e
        } catch (e: Exception) {
            // Every tick in the app already logs and handles its own failures; this is
            // the backstop for the one that does not, and the whole point is that the
            // next tick still happens.
            println("⚠️ Poll tick failed, keeping the loop alive: ${e.message}")
        }

        val remaining = intervalMs - (getTimeMillis() - startedAt)
        if (remaining > 0) delay(remaining)
    }
}
