package com.developer.ev2z.network

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

/**
 * What the app does when `gps.eva2z.in` answers **429 Too Many Requests**.
 *
 * The server runs Laravel's `throttle` middleware, so past its budget every route on
 * the host answers 429 with a `ThrottleRequestsException` body. Two things used to go
 * wrong at that moment, and together they read to the user as "GPS tracking switched
 * itself off":
 *
 *  1. The 429 body was *decoded as a fleet*. [com.developer.ev2z.data.local.model.DeviceResponse]
 *     defaults every field — deliberately, so one missing key cannot blank the map — so
 *     `{"message":"Too Many Attempts.",…}` parses happily into a response with
 *     `success=false` and no devices, which the home map then drew: no markers, no
 *     vehicles, nothing. The log line for it is `✅ Devices loaded: 0` immediately after
 *     a 429.
 *  2. Nothing backed off. The pollers kept asking once a second, so the throttle window
 *     never got a chance to clear and the map stayed empty for as long as the screen was
 *     open.
 *
 * So a 429 is now an error ([RateLimitedException]) rather than data, and it closes
 * [RateLimitGate] for as long as the server asked — which every polling loop waits on
 * before spending another request.
 */

/** Used when the server throttles us without a `Retry-After` we can read. */
internal const val DEFAULT_RETRY_AFTER_MS = 15_000L

/**
 * Never hold the pollers off longer than this, however large `Retry-After` is.
 *
 * Laravel's throttle answers with the remainder of its window, which can be a full
 * minute. Honouring that exactly is correct for the server and wrong for the user: a
 * minute of frozen tracking looks like the same fault being fixed here. Capped, so a
 * long window is retried a few times inside it instead.
 */
internal const val MAX_RETRY_AFTER_MS = 20_000L

/** The server refused this request because the app is over its request budget. */
class RateLimitedException(val retryAfterMs: Long) :
    Exception("Rate limited by the server — retrying in ${retryAfterMs / 1000}s.")

/** A non-2xx the caller has no specific handling for. Never decoded as a payload. */
class ApiStatusException(val status: Int, val path: String) :
    Exception("$path answered HTTP $status.")

/**
 * The account cannot read this device — `ignition-log/{id}` answers **403** with
 * `{"success":false,"message":"Unauthorized or device not found."}`.
 *
 * Kept well away from [UnauthorizedException], which means the *session* is finished and
 * sends the user to the login screen. This one says the session is fine and this one
 * device is not ours, so the right response is to stop asking about that device and carry
 * on with the rest of the fleet — not to log anybody out.
 */
class DeviceForbiddenException(val deviceId: String) :
    Exception("Device $deviceId is not readable with this session.")

/**
 * `Retry-After`, in milliseconds, clamped to [MAX_RETRY_AFTER_MS].
 *
 * Sent in seconds per RFC 9110; Laravel sends it as a bare integer. An HTTP-date form
 * is not parsed — it does not occur on this host — and falls back to the default.
 */
internal fun retryAfterMsOf(response: HttpResponse): Long {
    val fromHeader = response.headers[HttpHeaders.RetryAfter]
        ?.trim()
        ?.toLongOrNull()
        ?.times(1_000)

    return min(max(fromHeader ?: DEFAULT_RETRY_AFTER_MS, 1_000L), MAX_RETRY_AFTER_MS)
}

/**
 * Shut while the server is throttling the app; every polling loop waits on it.
 *
 * A gate rather than an exception the loops catch, because the ticks swallow their own
 * failures — `fetchDeviceLiveLocation` ends in `catch (_: Exception)` — so a 429 raised
 * inside one of them never reaches its loop. This way one 429 anywhere quiets *all* of
 * them, which is what the server is asking for: the budget is per app, not per screen.
 *
 * One-shot calls the user is waiting on (adding a device, loading a report) deliberately
 * do not consult it. They are not what spends the budget, and failing them silently
 * while a timer is parked would be worse than letting them answer 429.
 *
 * The timestamp is a plain `var`: it is written from whichever IO thread took the 429 and
 * read from the pollers, and the only cost of a lost update is one extra request.
 */
object RateLimitGate {

    private var openAtMs = 0L

    /** How much longer the pollers should hold off; 0 when the gate is open. */
    fun remainingMs(): Long = max(0L, openAtMs - getTimeMillis())

    /** True while the server is still throttling us. */
    val isClosed: Boolean get() = remainingMs() > 0

    /**
     * Holds the pollers off for [forMs], or leaves an existing, longer hold in place —
     * three screens taking a 429 in the same window must not shorten each other's wait.
     */
    fun close(forMs: Long) {
        val until = getTimeMillis() + forMs
        if (until <= openAtMs) return
        openAtMs = until
        println("⏳ Rate limited — pausing polling for ${forMs / 1000}s")
    }

    /** Suspends until the gate opens. Returns at once when it already is. */
    suspend fun awaitOpen() {
        while (true) {
            val wait = remainingMs()
            if (wait <= 0L) return
            delay(wait)
        }
    }

    /** Lets polling resume immediately — on a successful response, and for tests. */
    fun open() {
        openAtMs = 0L
    }
}
