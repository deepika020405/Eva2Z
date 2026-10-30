package com.developer.ev2z.util

import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.data.local.model.isMotionKind
import com.developer.ev2z.data.local.model.statusKindOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The statuses worth a notification, keyed by a kind out of [statusKindOf]: the title
 * the banner carries, and how the vehicle's name reads in its body.
 *
 * "idle", "stopped" (ignition off) and "offline" are all absent — none of them carries a
 * banner — but they are not treated alike; see [VehicleStatusNotifier].
 */
internal val WATCHED_STATUS_KINDS = mapOf(
    "running" to ("Vehicle Moving" to "is now moving"),
    "parked" to ("Vehicle Parked" to "is now parked"),
)

/** One device as a single poll saw it. [status] is the server's raw word. */
data class VehicleStatusSample(
    val deviceId: String,
    val deviceName: String,
    val status: String?,
    /**
     * The engine, as this poll could tell it: `"on"`, `"off"`, or null when it could not
     * be read at all.
     *
     * Carried on the sample rather than looked up separately because it is already known
     * by the time one is built — [IdleStatusResolver] reads the ignition to decide
     * whether a standstill is idling, and a moving vehicle's engine needs no lookup to
     * establish. That makes the Engine On/Off alerts cost nothing beyond the requests the
     * idle rule was already making. See [IgnitionAlertNotifier].
     */
    val ignition: String? = null,
)

/** The last status this device was seen in — one entry per vehicle. */
@Serializable
internal data class StatusSpell(val kind: String)

/**
 * Where the per-vehicle statuses live between one poll and the next.
 *
 * An interface because the things that feed the notifier run in different processes:
 * the foreground poll lives as long as the app does, while the background check
 * (`VehicleStatusBackgroundCheck`) is a fresh process each time an alarm fires.
 * In-memory state would mean every background run saw every vehicle for the first time
 * and announced its status again — the duplicate the spec rules out. See
 * [StorageStatusSpellStore].
 */
interface StatusSpellStore {
    fun read(): String
    fun write(value: String)
}

/** For tests, and for anything that genuinely only cares about one app session. */
class InMemoryStatusSpellStore : StatusSpellStore {
    private var value: String = ""
    override fun read(): String = value
    override fun write(value: String) { this.value = value }
}

/** The real one: survives the process, so a check minutes later still knows the state. */
class StorageStatusSpellStore(
    private val storage: StorageManager,
    private val key: String = "vehicle_status_spells",
) : StatusSpellStore {
    override fun read(): String = storage.getString(key, "")
    override fun write(value: String) = storage.saveString(key, value)
}

/**
 * Turns each poll's device statuses into notifications.
 *
 * The rule is the whole of it: **a vehicle's status changing is a notification, a
 * vehicle's status staying put is silence.** No waiting period and no rate limit — the
 * banner goes out on the same poll that first reports the new status.
 *
 * * **Moving → "Vehicle Moving", parked → "Vehicle Parked"**, read through [statusKindOf]
 *   because the server spells these several ways ("running", casing of its own).
 * * **Idle has no banner.** It is still remembered as a state of its own, though, which
 *   is the point of keeping it out of [WATCHED_STATUS_KINDS] rather than out of the
 *   samples: a vehicle that stops with the engine running goes quiet instead of
 *   announcing a park it has not made, and the "Vehicle Parked" banner then lands when
 *   the engine actually goes off. Pulling away from an idle spell still says "Vehicle
 *   Moving". See [IdleStatusResolver], which is what puts the word on the sample.
 * * **One per change, not per poll.** The last status is remembered per vehicle, so the
 *   five-second foreground poll and the background check can both run over an unchanged
 *   fleet all day without posting anything.
 * * **Every vehicle on its own.** Two devices changing state in the same poll get a
 *   banner each; one device changing does not speak for the others.
 * * **"offline" is not a change.** It says the tracker stopped reporting, not that the
 *   vehicle did something — so a device that drops out mid-drive and comes back keeps
 *   the status it had, and does not announce it a second time. Ignition off
 *   ("stopped") *is* about the vehicle, so it is remembered as a different state even
 *   though it has no banner of its own: moving → off → moving says "Vehicle Moving"
 *   again, which is true, while moving → offline → moving does not.
 *
 * Worth knowing, since nothing throttles the output: if the server ever flips a device
 * between two statuses minute by minute — `status-details` does exactly that on this
 * account, though `latest-locations`, which this reads, has been steady — the user gets
 * a banner for each flip. The guard for that would be a minimum interval between
 * alerts for the same vehicle; say the word and it is a few lines.
 */
class VehicleStatusNotifier(
    private val store: StatusSpellStore = InMemoryStatusSpellStore(),
    private val post: (key: String, title: String, body: String) -> Unit =
        ::showVehicleStatusNotification,
) {

    /** Feeds one poll's worth of devices in and posts a banner for every change. */
    fun onPoll(samples: List<VehicleStatusSample>) {
        val loaded = load()
        val spells = loaded.toMutableMap()

        samples.forEach { sample ->
            if (sample.deviceId.isBlank()) return@forEach

            val kind = statusKindOf(sample.status)
            val previous = spells[sample.deviceId]?.kind

            // A connectivity answer — "offline", or a word this app does not know — says
            // nothing about the vehicle. Leave the device exactly as it was, so what it
            // was doing before the tracker went quiet is still what it is doing when the
            // fixes come back.
            if (!isMotionKind(kind)) return@forEach

            // The same status it already had: the duplicate every poll would otherwise
            // produce.
            if (previous == kind) return@forEach

            spells[sample.deviceId] = StatusSpell(kind)

            val announcement = WATCHED_STATUS_KINDS[kind] ?: return@forEach
            val (title, verb) = announcement
            post("vehicle-$kind-${sample.deviceId}", title, "${sample.deviceName} $verb")
        }

        // Only when something actually changed: the foreground poll runs every five
        // seconds, and an unchanged fleet would otherwise write the same JSON to disk
        // all day for nothing.
        if (spells != loaded) save(spells)
    }

    /** Forgets every vehicle — call on logout so the next session starts from silence. */
    fun reset() = store.write("")

    private fun load(): Map<String, StatusSpell> {
        val raw = store.read()
        if (raw.isBlank()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, StatusSpell>>(raw)
        } catch (e: Exception) {
            // A shape written by an older build, or a half-written value. Starting over
            // costs one repeated notification; propagating would break the poll that
            // called in.
            println("⚠️ Status spells unreadable, starting fresh: ${e.message}")
            emptyMap()
        }
    }

    private fun save(spells: Map<String, StatusSpell>) {
        try {
            store.write(json.encodeToString(spells))
        } catch (e: Exception) {
            println("⚠️ Status spells not saved: ${e.message}")
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
