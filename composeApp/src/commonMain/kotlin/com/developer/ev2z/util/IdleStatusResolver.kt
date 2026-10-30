package com.developer.ev2z.util

import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.data.local.model.isRunningKind
import com.developer.ev2z.data.local.model.statusKindOf
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How a device's ignition is looked up: "on", "off", or null when it cannot be told. */
typealias IgnitionLookup = suspend (deviceId: String) -> String?

/**
 * How the server's own most recent status for a device is looked up — the word out of
 * `status-details`, or null when there is none to read.
 *
 * The second opinion, for a tracker whose ignition line does not exist. See
 * [IdleStatusResolver].
 */
typealias StatusSessionLookup = suspend (deviceId: String) -> String?

/**
 * Puts the word "idle" back into the fleet poll, which the server leaves out of it.
 *
 * `user/devices/latest-locations` is a two-way classifier: a vehicle is either "moving"
 * or "parked" (and "offline" once its tracker goes quiet). It carries an `idle_count`
 * field, and on a live account that field reads `0` all day while the same device's
 * `device/status-details` records idle session after idle session — so an engine-on
 * standstill arrives at the app spelled "parked".
 *
 * Observed on device 0863656047639278 — standing with the ignition on for a minute while
 * the fleet payload said "parked" throughout, and `status-details` wrote three 30-second
 * idle sessions for that same minute.
 *
 * Idle itself carries no notification (see [WATCHED_STATUS_KINDS]); the word still has to
 * exist, because a standstill the app cannot tell from a park is announced as one. That
 * is what this stops: the Parked banner waits for the engine to go off rather than firing
 * at the first red light.
 *
 * The missing field is ignition, which `device-status/{id}` has and the fleet payload
 * does not, so idle is derived here:
 *
 * * **speed 0 and the feed says parked/stopped and the ignition is on** → idle.
 * * **Anything else** → the server's own word, untouched.
 *
 * ### The second source, for a tracker with no ignition wire
 *
 * `device-status` answers `{"success":false,"message":"No status found for this
 * device."}` for some trackers — the Yamaha on this account among them — and on those the
 * rule above can never fire, so a standstill that is really idling reads as a park on one
 * vehicle of an account and not the next. `status-details` is asked in that case: it keeps idle
 * sessions of its own, and its newest session is a second opinion on what the vehicle is
 * doing now.
 *
 * It can only ever *add* idle, never overrule the feed with another word, and that is
 * deliberate — the sessions are not always sane. The same Yamaha's day comes back as a
 * single fifteen-hour "moving" session while the fleet payload reports it parked and
 * standing; believing that would post a Vehicle Moving banner for a vehicle in a car
 * park. The feed decides moving against parked, as it always has; this decides only
 * whether a standstill is an idle one.
 *
 * ### What it costs, and why it is shaped this way
 *
 * The ignition is a second request per device, and the fleet poll runs every
 * [com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS] — 5 s — against a host that
 * rate-limits at 60 requests a minute (`X-RateLimit-Limit: 60`). Asking per device per
 * poll would spend 12 of those per vehicle and start returning 429s on a four-vehicle
 * account. Two things keep it cheap:
 *
 * * **Only at a standstill.** A moving vehicle needs no ignition lookup: it cannot be
 *   idle. Most polls therefore ask nothing.
 * * **Cached for [IGNITION_CACHE_TTL_MS].** An ignition state does not change faster
 *   than that, and it bounds a standing vehicle to two lookups a minute however often
 *   the screen polls.
 * * **The session lookup is the fallback, not a second request.** It runs only where the
 *   ignition answer was unreadable, so a vehicle with a working ignition line costs
 *   exactly what it did before, and it shares the same cache discipline.
 *
 * A stale fix is also left alone: a tracker that last spoke [MAX_FIX_AGE_MINUTES] ago is
 * reporting history, and "the ignition was on" from twenty minutes back is not grounds
 * for saying the vehicle is idling now.
 */
class IdleStatusResolver(
    private val now: () -> Long = { getTimeMillis() },
    private val cacheTtlMs: Long = IGNITION_CACHE_TTL_MS,
) {

    private data class CachedIgnition(val value: String?, val readAtMs: Long)

    private val cache = mutableMapOf<String, CachedIgnition>()

    /**
     * The session answers, kept apart from the ignition ones rather than sharing a map
     * keyed by device: the two lookups return different vocabularies for the same
     * device id ("on"/"off" against "idle"/"moving"/"parked"), and one map would have
     * each overwrite the other's answer on every poll.
     */
    private val sessionCache = mutableMapOf<String, CachedIgnition>()

    /**
     * The cache is reached from two threads of the same process: the fleet poll on the
     * screen's scope, and the alarm chain's check on `Dispatchers.IO` — `StatusAlarmReceiver`
     * is declared with no `android:process`, so it runs here rather than in a process of
     * its own. A bare map between them is a resize race, and the lock also means a
     * standing vehicle is looked up once when both arrive together instead of twice.
     */
    private val gate = Mutex()

    /**
     * One poll's devices as samples for [VehicleStatusNotifier], with standstills that
     * are really idling relabelled.
     *
     * [sessionKindOf] is optional so that a caller with nothing to offer — a test, or any
     * path that only wants the ignition rule — keeps working; a device whose ignition
     * cannot be read simply stays parked, which is what happened everywhere before.
     */
    suspend fun resolve(
        devices: List<Device>,
        ignitionOf: IgnitionLookup,
        sessionKindOf: StatusSessionLookup? = null,
    ): List<VehicleStatusSample> = devices.map { device ->
        resolveOne(device, ignitionOf, sessionKindOf)
    }

    /**
     * One device's sample: the status the notifier should see, and the engine state this
     * poll could establish.
     *
     * The ignition comes out of the same work the idle rule does, so the Engine On/Off
     * alerts ride along on it rather than adding requests of their own:
     *
     * * **A moving vehicle's engine is on**, and that needs no endpoint to know. Which
     *   matters more than it sounds: `device-status` is only asked about standstills (see
     *   [couldBeIdling]), so without this rule a vehicle that started and drove off would
     *   never report an engine, and the alert would be the one thing on this screen that
     *   missed a vehicle setting out.
     * * **A standstill's engine** is the ignition reading the idle rule already fetched
     *   and cached — free within [IGNITION_CACHE_TTL_MS].
     * * **Anything else is null**: a stale fix, or a tracker with no ignition wire. Null
     *   means "not known", and the notifier leaves such a device exactly as it was rather
     *   than announcing a guess.
     */
    private suspend fun resolveOne(
        device: Device,
        ignitionOf: IgnitionLookup,
        sessionKindOf: StatusSessionLookup?,
    ): VehicleStatusSample {
        val movingNow = isRunningKind(statusKindOf(device.status)) || device.speed > 0
        val freshEnough = (device.lastSeenMinutes() ?: 0) <= MAX_FIX_AGE_MINUTES

        val status = statusFor(device, ignitionOf, sessionKindOf)

        val ignition = when {
            !freshEnough -> null
            movingNow -> IGNITION_ON
            // Reads the cache the line above has just filled for a standstill; null here
            // is a tracker whose ignition line says nothing.
            couldBeIdling(device) -> cachedOr(cache, device.deviceId, now()) {
                ignitionOf(device.deviceId)
            }
            else -> null
        }

        return VehicleStatusSample(
            deviceId = device.deviceId,
            deviceName = device.getDisplayName(),
            status = status,
            ignition = ignition,
        )
    }

    /** Forgets the cached answers — called on logout alongside the notifier's reset. */
    fun reset() {
        cache.clear()
        sessionCache.clear()
    }

    private suspend fun cachedOr(
        into: MutableMap<String, CachedIgnition>,
        deviceId: String,
        nowMs: Long,
        lookup: suspend () -> String?,
    ): String? = gate.withLock {
        val cached = into[deviceId]
        if (cached != null && nowMs - cached.readAtMs < cacheTtlMs) return@withLock cached.value

        // A failed lookup is cached like any other answer: without that, a device whose
        // ignition endpoint is down would be asked again every 5 s.
        lookup().also { into[deviceId] = CachedIgnition(it, nowMs) }
    }

    private suspend fun statusFor(
        device: Device,
        ignitionOf: IgnitionLookup,
        sessionKindOf: StatusSessionLookup?,
    ): String? {
        if (!couldBeIdling(device)) return device.status

        val ignition = cachedOr(cache, device.deviceId, now()) { ignitionOf(device.deviceId) }

        // A readable ignition settles it both ways, and the engine being off is a real
        // answer: that standstill is a park, and asking status-details to say otherwise
        // would spend a request on overruling the better source.
        if (ignition != null) {
            if (!ignition.equals(IGNITION_ON, ignoreCase = true)) return device.status
            return idle(device, "ignition on")
        }

        // No ignition line on this tracker. What the server itself last recorded is the
        // only thing left that can tell an idling vehicle from a parked one.
        val session = sessionKindOf?.let {
            cachedOr(sessionCache, device.deviceId, now()) { it(device.deviceId) }
        } ?: return device.status

        if (statusKindOf(session) != IDLE_STATUS) return device.status
        return idle(device, "newest session idle")
    }

    /**
     * The one line that says this feature did something. Worth logging because the
     * relabelling is invisible otherwise: the fleet payload in the log above still reads
     * "parked", and the banner that follows says idle. [because] names which of the two
     * sources decided it, which is the first thing to know when one of them misbehaves.
     */
    private fun idle(device: Device, because: String): String {
        println("🟡 ${device.getDisplayName()}: server=${device.status} + $because → idle")
        return IDLE_STATUS
    }

    /**
     * Whether this device is worth an ignition lookup at all: standing still, said so by
     * the server, and recently enough for the answer to describe the present.
     */
    private fun couldBeIdling(device: Device): Boolean {
        if (device.deviceId.isBlank()) return false
        if (device.speed != 0) return false
        if (statusKindOf(device.status) !in STANDSTILL_KINDS) return false
        return (device.lastSeenMinutes() ?: 0) <= MAX_FIX_AGE_MINUTES
    }

    private companion object {
        /** Spelled the way [statusKindOf] already understands it. */
        const val IDLE_STATUS = "idle"

        /**
         * What `device-status` calls a live ignition line, and the word the samples carry
         * for it — [IgnitionAlertNotifier] reads it with the same spelling.
         */
        const val IGNITION_ON = "on"

        /** The kinds an idling vehicle arrives disguised as. */
        val STANDSTILL_KINDS = setOf("parked", "stopped")
    }
}

/**
 * How long an ignition reading is trusted.
 *
 * Thirty seconds against a 5-second poll is twelve lookups saved out of twelve, and it
 * is well inside what the alerts need: the idle sessions on this account run 30 s and
 * up, and the notification is about a vehicle's state rather than about the instant it
 * changed.
 */
const val IGNITION_CACHE_TTL_MS = 30_000L

/**
 * How old a fix may be and still describe where the vehicle is now.
 *
 * `Device.FRESH_WITHIN_MINUTES` is 2, which is the threshold for *showing* an age on the
 * map; this is the looser one for acting on a fix, because a parked tracker reporting
 * every minute or two must not fall out of the feature on one slow upload.
 */
const val MAX_FIX_AGE_MINUTES = 15
