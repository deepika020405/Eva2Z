package com.developer.ev2z.util

import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.network.DeviceForbiddenException
import com.developer.ev2z.network.RateLimitGate
import com.developer.ev2z.network.ServiceEndpointMissingException
import com.developer.ev2z.response.DeviceAlarm
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.CancellationException

/** How a device's event log is fetched: [com.developer.ev2z.network.ApiService.getDeviceAlarms]. */
typealias AlarmLookup = suspend (deviceId: String) -> List<DeviceAlarm>

/**
 * How often `ignition-log/{id}` is called while the app is open.
 *
 * Five seconds, as asked for — engine on/off should land as near to immediately as the
 * server will allow.
 */
const val IGNITION_ALARM_POLL_INTERVAL_MS = 1_000L

/**
 * How many devices one tick reads, which is what keeps a 5-second cadence affordable.
 *
 * This endpoint is **per device**, unlike everything else the app polls: the fleet costs
 * one request whatever the size of the account, and this costs one per vehicle. Asking
 * every vehicle every five seconds is therefore `devices × 12` requests a minute — 48 on
 * a four-vehicle account, on top of the ~24 the fleet and live-location polls already
 * spend, against a host that throttles the app at about 60 (see `RateLimit.kt`). That is
 * the arithmetic that blanked the map on 2026-10-26.
 *
 * So the call goes out every five seconds and takes the vehicles in turn. One vehicle on
 * the account is read every 5 s; four are read every 20 s each, for 12 requests a minute
 * in total rather than 48 — and an engine event is still noticed inside half a minute
 * with the app open, sooner than the user can look at the screen.
 *
 * Raising this multiplies the request rate by exactly the amount raised. If every vehicle
 * really must be read every tick, this is the one line to change — and `⏳ Rate limited`
 * in the log is what it will cost.
 */
const val IGNITION_DEVICES_PER_TICK = 1

/**
 * The one engine-alert notifier the app shares, foreground and background alike.
 *
 * Shared for the same reason [VehicleStatusAlerts] is: the memory of which events have
 * been announced lives in storage, so the screen poll and the background check cannot
 * each announce the same ignition — and a background run in a process the user never
 * opened still knows where the log was last read.
 */
object IgnitionAlerts {

    private val notifier by lazy {
        IgnitionAlertNotifier(
            store = StorageStatusSpellStore(createStorageManager(), key = "vehicle_ignition_spells"),
        )
    }

    /**
     * Set once the endpoint has answered 404, which stands the feature down for the rest
     * of the session.
     *
     * The route is deployed and confirmed, so this should not fire — it is here for the
     * build that outlives a server change. Without it a path the server has moved would
     * mean one 404 per device per tick for as long as the app is open, which is
     * log-spam-instead-of-a-feature.
     */
    private var endpointMissing = false

    /** True while the feature is standing itself down. */
    val isDisabled: Boolean get() = endpointMissing

    /**
     * Devices the endpoint has answered **403** for — "Unauthorized or device not found."
     *
     * Such a device can never answer with this session, so asking again is a request
     * spent on a guaranteed refusal, every round, for as long as the app is open. Dropped
     * instead, which also keeps the log readable. The set is per session: a tracker added
     * to the account properly is picked up again after the next launch or logout.
     */
    private val forbiddenDevices = mutableSetOf<String>()

    /**
     * Reads the log for every device in one round and posts whatever changed.
     *
     * Sequential, not parallel: four devices asked at once is a burst of four requests
     * against a host that throttles the app as a whole, and nothing here is in a hurry.
     * The [RateLimitGate] is checked between devices so a throttle that starts mid-round
     * stops the round rather than being pushed through.
     *
     * One device's failure never stops the others: a tracker whose log cannot be read is
     * worth a log line, not a round that gives up halfway down the fleet.
     */
    suspend fun onRound(devices: List<Device>, alarmsOf: AlarmLookup) = sweep(devices, alarmsOf)

    /**
     * Engine on/off from the fleet poll's own samples — the same path, and the same
     * cadence, as the Moving and Parked alerts.
     *
     * Costs nothing: the ignition on each sample is what [IdleStatusResolver] read to
     * decide whether a standstill was idling, plus the free knowledge that a moving
     * vehicle's engine is running. Every device, every poll, with no dependence on the
     * tracker having written to its event log — which is what makes these alerts as
     * reliable as the status ones.
     */
    fun onStates(samples: List<VehicleStatusSample>) {
        samples.forEach { sample ->
            notifier.onIgnitionState(sample.deviceId, sample.deviceName, sample.ignition)
        }
    }

    private suspend fun sweep(devices: List<Device>, alarmsOf: AlarmLookup) {
        if (endpointMissing) return

        for (device in devices) {
            if (device.deviceId.isBlank()) continue
            if (device.deviceId in forbiddenDevices) continue
            if (RateLimitGate.isClosed) {
                println("⏳ Ignition round stopped early — server is throttling")
                return
            }

            try {
                notifier.onAlarms(
                    deviceId = device.deviceId,
                    deviceName = device.getDisplayName(),
                    alarms = alarmsOf(device.deviceId),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ServiceEndpointMissingException) {
                endpointMissing = true
                println(
                    "ℹ️ Engine on/off alerts off for this session — ${e.path} is not on the " +
                            "server. Check DEVICE_ALARMS_PATH against the deployed route."
                )
                return
            } catch (e: DeviceForbiddenException) {
                // Not our device — the session is fine. Stop asking about this one and
                // carry on down the fleet.
                forbiddenDevices += e.deviceId
                println("ℹ️ Ignition log not readable for ${e.deviceId} — skipping it this session")
            } catch (e: Exception) {
                println("⚠️ Ignition log unread for ${device.deviceId}: ${e.message}")
            }
        }
    }

    /**
     * One tick: every [IGNITION_ALARM_POLL_INTERVAL_MS], read the next
     * [IGNITION_DEVICES_PER_TICK] vehicles' logs and post whatever changed.
     *
     * Driven by the home map's fleet poll, which is the only steady tick the app has
     * while it is open. The interval is enforced here rather than trusted from the
     * caller: that poll also fires on a manual refresh, and a user pulling to refresh
     * should not be able to spend this feature's whole budget by doing it quickly.
     *
     * The vehicles are taken in turn — see [IGNITION_DEVICES_PER_TICK] for why, and
     * [rotatedSlice] for how — so the call rate does not grow with the size of the fleet.
     */
    suspend fun onFleetPoll(devices: List<Device>, alarmsOf: AlarmLookup) {
        val now = getTimeMillis()
        if (now - lastRoundAtMs < MIN_GAP_MS) return
        lastRoundAtMs = now

        val readable = devices.filter {
            it.deviceId.isNotBlank() && it.deviceId !in forbiddenDevices
        }
        if (readable.isEmpty()) return

        val turn = rotatedSlice(readable, cursor, IGNITION_DEVICES_PER_TICK)
        // Advanced by what was actually taken, modulo the list as it stands now: a device
        // added or removed between ticks must shift the turn along, not reset it to the
        // top of the fleet and leave the last vehicles never read.
        cursor = (cursor + turn.size) % readable.size

        sweep(turn, alarmsOf)
    }

    private var lastRoundAtMs = 0L

    /**
     * The gate the tick is held behind — a shade under
     * [IGNITION_ALARM_POLL_INTERVAL_MS], and the margin is not cosmetic.
     *
     * The fleet poll that drives this runs on the same five seconds, and the mark is
     * stamped partway through its tick rather than at the top of it. Gated on the full
     * interval, the gap measured on the next tick comes out a few milliseconds *short*,
     * the tick is skipped, and the ignition log is then read every ten seconds — half the
     * rate this is supposed to run at, for no visible reason.
     */
    private const val MIN_GAP_MS = IGNITION_ALARM_POLL_INTERVAL_MS * 9 / 10

    /** Which vehicle the next tick starts from. */
    private var cursor = 0

    /** Forgets every vehicle's last event — called on logout. */
    fun reset() {
        notifier.reset()
        lastRoundAtMs = 0L
        cursor = 0
        // The next account may well own the devices this one could not read.
        forbiddenDevices.clear()
        // A 404 belongs to the build, not the session — but a logout is also the moment a
        // corrected path would first be exercised, so let it try again.
        endpointMissing = false
    }
}

/**
 * Up to [count] items starting at [start], wrapping round the end of [items].
 *
 * The turn-taking behind [IgnitionAlerts.onFleetPoll]: with four vehicles and one per
 * tick it yields A, B, C, D, A, … so every vehicle is read at a steady rate and the
 * request count does not grow with the fleet.
 *
 * [start] is taken modulo the size rather than trusted, because the list it indexes is
 * rebuilt from each fleet response: a vehicle leaving the account would otherwise leave
 * the cursor pointing past the end, and the tick would read nothing for as long as that
 * lasted.
 */
internal fun <T> rotatedSlice(items: List<T>, start: Int, count: Int): List<T> {
    if (items.isEmpty() || count <= 0) return emptyList()

    val size = items.size
    val take = minOf(count, size)
    val from = ((start % size) + size) % size

    return List(take) { items[(from + it) % size] }
}
