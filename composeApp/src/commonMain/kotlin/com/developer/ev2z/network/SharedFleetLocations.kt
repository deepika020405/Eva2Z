package com.developer.ev2z.network

import com.developer.ev2z.data.local.model.DeviceResponse
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One `user/devices/latest-locations` request serves every screen asking for it at the
 * same moment.
 *
 * Three pollers want this payload — the home map, the Track & Trace dropdown and the
 * Device Report dropdown — and they run at the same interval but not in step. Two of
 * them visible at once therefore spent two, sometimes three requests per interval on
 * identical data, and that is a third of the server's whole budget going on duplicates.
 * The logs show it plainly: `🌐 API: Fetching latest locations` twice inside 80 ms.
 *
 * A caller arriving while an answer from the last [DEVICES_SHARE_WINDOW_MS] is in hand
 * gets that answer; a caller arriving while a request is in flight waits for it instead
 * of starting a second one. So the fleet costs one request per interval however many
 * screens are watching.
 *
 * A top-level object, not a field on [ApiService]: sixteen call sites construct their own
 * `ApiService()`, so per-instance state would not be shared by the screens that need to
 * share it.
 */
internal object SharedFleetLocations {

    private val mutex = Mutex()

    /** The last answer, successful or not, and who it was for. */
    private var cachedToken: String? = null
    private var cached: Result<DeviceResponse>? = null
    private var cachedAtMs = 0L

    /**
     * The fleet for [token], fetched with [fetch] unless a recent enough answer exists.
     *
     * Failures are cached as well as successes, and that is the point rather than a
     * shortcut: while the server is throttling, one 429 answers all three pollers
     * instead of each of them earning their own.
     */
    suspend fun get(
        token: String,
        freshForMs: Long,
        fetch: suspend () -> DeviceResponse,
    ): DeviceResponse {
        fresh(token, freshForMs)?.let { return it.getOrThrow() }

        return mutex.withLock {
            // Checked again under the lock: a caller that queued behind the request
            // below is here precisely because someone else was already fetching, and
            // its answer is what this caller came for.
            fresh(token, freshForMs)?.let { return@withLock it.getOrThrow() }

            // Cancellation is not an answer about the fleet — it means this caller's
            // screen went away. Caching it would hand the next screen a cancelled
            // result it has no way to act on.
            val result = try {
                Result.success(fetch())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e)
            }

            cachedToken = token
            cached = result
            cachedAtMs = getTimeMillis()

            result.getOrThrow()
        }
    }

    private fun fresh(token: String, freshForMs: Long): Result<DeviceResponse>? {
        val held = cached ?: return null
        // A different token is a different account — never serve one user's fleet to
        // the next one to sign in on this device.
        if (cachedToken != token) return null
        if (getTimeMillis() - cachedAtMs >= freshForMs) return null
        return held
    }

    /** Drops the held answer — on logout, and for tests. */
    fun clear() {
        cachedToken = null
        cached = null
        cachedAtMs = 0L
    }
}
