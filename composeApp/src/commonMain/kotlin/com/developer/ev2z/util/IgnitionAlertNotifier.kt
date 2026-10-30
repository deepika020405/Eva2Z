package com.developer.ev2z.util

import com.developer.ev2z.response.DeviceAlarm
import com.developer.ev2z.response.IgnitionEvent
import com.developer.ev2z.response.clockTimeOf
import com.developer.ev2z.response.ignitionEventOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The banner each ignition event carries: the title, and how the vehicle's name reads in
 * the body.
 */
internal val IGNITION_ANNOUNCEMENTS = mapOf(
    IgnitionEvent.ON to ("Vehicle Engine On" to "Your vehicle's ignition has been turned ON"),
    IgnitionEvent.OFF to ("Vehicle Engine Off" to "Your vehicle's ignition has been turned OFF"),
)

/** What was last announced for one vehicle, so the same change is never announced twice. */
@Serializable
internal data class IgnitionSpell(
    /**
     * The newest event row seen for this device — announced or not — or null when the log
     * has never been read for it.
     *
     * Every row is remembered, not just the ignition ones: an SOS row arriving after an
     * "Ignition OFF" must still move the mark forward, or that OFF stays "new" and is
     * re-examined on every poll for as long as the log holds it.
     *
     * Nullable so the two sources can share one record: the state source knows a vehicle's
     * engine without having read its log, and a null here still means "this log has not
     * been baselined", so the first log read stays silent rather than announcing a row
     * that may be days old.
     */
    val lastAlarmId: Long? = null,
    /** "ON"/"OFF" as last announced, or null when nothing has been. */
    val kind: String? = null,
)

/**
 * Turns the tracker's event log into engine on/off notifications.
 *
 * The rule, as with [VehicleStatusNotifier]: **the engine changing state is a
 * notification; the engine staying put is silence.** What differs is the source. That
 * notifier reads the fleet's *current state* and has to infer the engine, because
 * `latest-locations` calls an engine-on standstill "parked". This one reads events the
 * tracker actually recorded, where `alarm_type` says "Ignition ON" / "Ignition OFF" in
 * so many words — so engine alerts no longer depend on an inference.
 *
 * * **Ignition only.** Everything else the log carries — SOS, tow, overspeed — is passed
 *   over by [ignitionEventOf] rather than guessed at. An SOS must never post "engine on".
 * * **One banner per change, not per poll.** [IgnitionSpell.lastAlarmId] marks how far
 *   the log has been read, so the same OFF cannot be announced by the next poll, by the
 *   background check, or by the next process to start.
 * * **Nothing announced for a device being seen for the first time.** The log's newest
 *   row may be days old — the sample payload is a single OFF from this morning — and
 *   posting it on first sight would tell the user their engine just stopped when it
 *   stopped yesterday. The first poll records where the log stands and stays quiet.
 * * **Only the newest ignition event of a batch.** If a vehicle was started and stopped
 *   between two polls, the user is told where it ended up rather than given a history
 *   lesson in two banners.
 * * **One notification per vehicle in the tray.** The key is `vehicle-ignition-<id>`, so
 *   an ON replaces the OFF already showing instead of stacking under it — see
 *   [showVehicleStatusNotification].
 */
class IgnitionAlertNotifier(
    private val store: StatusSpellStore = InMemoryStatusSpellStore(),
    private val post: (key: String, title: String, body: String) -> Unit =
        ::showVehicleStatusNotification,
) {

    /**
     * Feeds one device's event log in, posting a banner if the engine changed state.
     *
     * [alarms] is whatever the endpoint returned — any order, ignition rows or not.
     */
    fun onAlarms(deviceId: String, deviceName: String, alarms: List<DeviceAlarm>) {
        if (deviceId.isBlank() || alarms.isEmpty()) return

        val loaded = load()
        val previous = loaded[deviceId]

        // Sorted rather than trusted: the sample arrives newest-first, and the whole
        // mechanism below depends on knowing which row is actually the latest.
        val ordered = alarms.sortedBy { it.id }
        val newestId = ordered.last().id

        // First time this device's log is read: note where it stands, say nothing. The
        // log may be hours old, and announcing its newest row on sight would report an
        // engine that stopped this morning as having stopped just now.
        val readTo = previous?.lastAlarmId
        if (readTo == null) {
            val baseline = (previous ?: IgnitionSpell()).copy(lastAlarmId = newestId)
            save(loaded + (deviceId to baseline))
            return
        }

        if (newestId <= readTo) return

        val newestIgnition = ordered
            .filter { it.id > readTo }
            .mapNotNull { alarm ->
                ignitionEventOf(alarm.alarmType, alarm.event)?.let { alarm to it }
            }
            .lastOrNull()

        // New rows, but none of them about the engine. The mark still moves forward.
        if (newestIgnition == null) {
            save(loaded + (deviceId to previous.copy(lastAlarmId = newestId)))
            return
        }

        val (alarm, event) = newestIgnition

        // The state it was already in — two OFF rows in a row, which a tracker
        // reconnecting can produce — is not a change.
        if (previous.kind == event.name) {
            save(loaded + (deviceId to previous.copy(lastAlarmId = newestId)))
            return
        }

        save(loaded + (deviceId to IgnitionSpell(lastAlarmId = newestId, kind = event.name)))

        val (title, verb) = IGNITION_ANNOUNCEMENTS.getValue(event)
        val at = clockTimeOf(alarm.occurredAtIst)

        post(
            "vehicle-ignition-$deviceId",
            title,
            if (at.isEmpty()) "$deviceName $verb" else "$deviceName $verb · $at",
        )
    }

    /**
     * The second source: the engine as a **state**, compared poll to poll — which is
     * exactly how the Moving and Parked alerts work.
     *
     * The event log is the better source when it has rows: it carries the minute the
     * ignition turned, and it cannot miss a change that happened between two polls. But it
     * only speaks when the tracker writes to it, and on this account the logs are empty —
     * so on their own, engine alerts would be the one notification that never arrives
     * while Moving and Parked keep working. This closes that: `device-status` reports the
     * ignition line as "on"/"off", the idle rule already reads it, and a change in it is a
     * change in the engine.
     *
     * [ignition] is the word from that endpoint, or null when it cannot be told — a
     * tracker with no ignition wire, or a fix too old to describe the present. Null is
     * left alone rather than treated as "off", for the same reason [VehicleStatusNotifier]
     * passes over "offline": it says the app cannot see, not that the vehicle stopped.
     *
     * Shares one memory with [onAlarms], so whichever source notices a change first
     * announces it and the other stays quiet — the user gets one banner, not two.
     */
    fun onIgnitionState(deviceId: String, deviceName: String, ignition: String?) {
        if (deviceId.isBlank()) return

        val event = when (ignition?.trim()?.lowercase()) {
            "on", "1", "true" -> IgnitionEvent.ON
            "off", "0", "false" -> IgnitionEvent.OFF
            else -> return
        }

        val loaded = load()
        val previous = loaded[deviceId]

        // Already in this state — the ordinary case on every poll — or seeing this device
        // for the first time, where the state is recorded without a banner so opening the
        // app does not announce an engine that has been off since yesterday.
        if (previous?.kind == event.name) return

        val baselining = previous?.kind == null
        save(loaded + (deviceId to (previous ?: IgnitionSpell()).copy(kind = event.name)))
        if (baselining) return

        val (title, verb) = IGNITION_ANNOUNCEMENTS.getValue(event)
        post("vehicle-ignition-$deviceId", title, "$deviceName $verb")
    }

    /** Forgets every vehicle — called on logout, so the next session starts from silence. */
    fun reset() = store.write("")

    private fun load(): Map<String, IgnitionSpell> {
        val raw = store.read()
        if (raw.isBlank()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, IgnitionSpell>>(raw)
        } catch (e: Exception) {
            // A shape written by an older build, or a half-written value. Starting over
            // costs one quiet poll per device, not a wrong notification: an unknown
            // device is baselined rather than announced.
            println("⚠️ Ignition spells unreadable, starting fresh: ${e.message}")
            emptyMap()
        }
    }

    private fun save(spells: Map<String, IgnitionSpell>) {
        try {
            store.write(json.encodeToString(spells))
        } catch (e: Exception) {
            println("⚠️ Ignition spells not saved: ${e.message}")
        }
    }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
