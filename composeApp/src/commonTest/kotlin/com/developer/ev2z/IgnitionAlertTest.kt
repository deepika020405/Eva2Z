package com.developer.ev2z

import com.developer.ev2z.network.ApiJson
import com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS
import com.developer.ev2z.response.DeviceAlarm
import com.developer.ev2z.response.DeviceAlarmsResponse
import com.developer.ev2z.response.IgnitionEvent
import com.developer.ev2z.response.clockTimeOf
import com.developer.ev2z.response.ignitionEventOf
import com.developer.ev2z.util.IGNITION_ALARM_POLL_INTERVAL_MS
import com.developer.ev2z.util.IGNITION_DEVICES_PER_TICK
import com.developer.ev2z.util.IgnitionAlertNotifier
import com.developer.ev2z.util.InMemoryStatusSpellStore
import com.developer.ev2z.util.rotatedSlice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Engine on/off notifications, read off the tracker's own event log.
 *
 * `alarm_type` is the only field on this account that states the ignition in words —
 * `latest-locations` calls an engine-on standstill "parked" — so this is what the alert
 * rests on. What has to hold: only ignition rows raise it, one change raises exactly one
 * banner however often the log is polled, and a log whose newest row is old does not
 * announce itself the first time the app sees the device.
 */
class IgnitionAlertTest {

    /** The payload as the server sends it, verbatim from the captured response. */
    private val samplePayload = """
        {
          "success": true,
          "data": [
            {
              "id": 3528316,
              "device_id": "0863656047646760",
              "event": "OFF",
              "alarm_type": "Ignition OFF",
              "latitude": 26.960119,
              "longitude": 81.000089,
              "speed": 0,
              "occurred_at_utc": "2026-10-28T09:16:07Z",
              "occurred_at_ist": "2026-10-28 14:46:07"
            }
          ],
          "meta": { "current_page": 1, "per_page": 3, "has_more": false }
        }
    """.trimIndent()

    private fun alarm(
        id: Long,
        alarmType: String? = "Ignition OFF",
        event: String? = "OFF",
        occurredAtIst: String? = "2026-10-28 14:46:07",
    ) = DeviceAlarm(
        id = id,
        deviceId = "0863656047646760",
        event = event,
        alarmType = alarmType,
        occurredAtIst = occurredAtIst,
    )

    /** Records what the notifier posted, in order. */
    private class Tray {
        val posted = mutableListOf<Triple<String, String, String>>()
        fun post(key: String, title: String, body: String) {
            posted += Triple(key, title, body)
        }
    }

    private fun notifier(tray: Tray, store: InMemoryStatusSpellStore) =
        IgnitionAlertNotifier(store = store, post = tray::post)

    // ── the payload ──────────────────────────────────────────────────────────────

    @Test
    fun theServersPayloadDecodes() {
        val decoded = ApiJson.decodeFromString(DeviceAlarmsResponse.serializer(), samplePayload)

        assertTrue(decoded.success)
        assertEquals(1, decoded.data.size)

        val event = decoded.data.first()
        assertEquals(3528316L, event.id)
        assertEquals("0863656047646760", event.deviceId)
        assertEquals("Ignition OFF", event.alarmType)
        assertEquals("OFF", event.event)
        // Numbers here, where latest-locations sends its coordinates as strings.
        assertEquals(26.960119, event.latitude)
        assertEquals(81.000089, event.longitude)
        assertEquals("2026-10-28 14:46:07", event.occurredAtIst)
        assertEquals(3, decoded.meta?.perPage)
        assertEquals(false, decoded.meta?.hasMore)
    }

    @Test
    fun perPageDecodesWhicheverTypeTheServerSendsIt() {
        // `ignition-log/{id}` sends this field as a number when it defaults it and as a
        // string when it echoes the query back — both seen on one account within a
        // minute. ApiJson is lenient, and this is the test that says so, because
        // tightening that configuration would otherwise break the whole response.
        val asNumber = """{"success":true,"data":[],"meta":{"current_page":1,"per_page":50,"has_more":false}}"""
        val asString = """{"success":true,"data":[],"meta":{"current_page":1,"per_page":"5","has_more":false}}"""

        assertEquals(
            50,
            ApiJson.decodeFromString(DeviceAlarmsResponse.serializer(), asNumber).meta?.perPage,
        )
        assertEquals(
            5,
            ApiJson.decodeFromString(DeviceAlarmsResponse.serializer(), asString).meta?.perPage,
        )
    }

    @Test
    fun aDeviceWithNoIgnitionHistoryIsAnEmptyLog() {
        // What this account's own devices answer today: the route works, the log is just
        // empty. Must read as "nothing to announce", not as a failure.
        val empty = """{"success":true,"data":[],"meta":{"current_page":1,"per_page":50,"has_more":false}}"""

        val decoded = ApiJson.decodeFromString(DeviceAlarmsResponse.serializer(), empty)

        assertTrue(decoded.success)
        assertTrue(decoded.data.isEmpty())
    }

    // ── which rows are about the engine ──────────────────────────────────────────

    @Test
    fun ignitionRowsAreRecognisedHoweverTheyAreSpelled() {
        assertEquals(IgnitionEvent.OFF, ignitionEventOf("Ignition OFF"))
        assertEquals(IgnitionEvent.ON, ignitionEventOf("Ignition ON"))
        assertEquals(IgnitionEvent.OFF, ignitionEventOf("IGNITION_OFF"))
        assertEquals(IgnitionEvent.ON, ignitionEventOf("ignition on"))
        assertEquals(IgnitionEvent.OFF, ignitionEventOf("  Ignition Off  "))
        // The vocabulary some trackers use for the same line.
        assertEquals(IgnitionEvent.ON, ignitionEventOf("ACC ON"))
        assertEquals(IgnitionEvent.OFF, ignitionEventOf("ACC OFF"))
    }

    @Test
    fun theBareEventDecidesAnIgnitionRowThatDoesNotSayWhichWay() {
        assertEquals(IgnitionEvent.OFF, ignitionEventOf("Ignition", event = "OFF"))
        assertEquals(IgnitionEvent.ON, ignitionEventOf("Ignition", event = "on"))
    }

    @Test
    fun everyOtherKindOfAlarmIsLeftAlone() {
        // An SOS must never post "engine on".
        assertNull(ignitionEventOf("SOS"))
        assertNull(ignitionEventOf("Overspeed"))
        assertNull(ignitionEventOf("Tow Alarm"))
        assertNull(ignitionEventOf("Low Battery", event = "ON"))
        assertNull(ignitionEventOf(null))
        assertNull(ignitionEventOf(""))
        // "ON" with no ignition wording anywhere is not necessarily the engine.
        assertNull(ignitionEventOf(null, event = "ON"))
    }

    // ── what the user is told, and how often ─────────────────────────────────────

    @Test
    fun theFirstSightOfADeviceIsSilent() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()

        // The sample log's newest row is from this morning. Announcing it on first sight
        // would say the engine stopped just now, hours after it did.
        notifier(tray, store).onAlarms("dev-1", "truck", listOf(alarm(id = 3528316)))

        assertTrue(tray.posted.isEmpty(), "announced a log it was seeing for the first time")
    }

    @Test
    fun aNewIgnitionEventIsAnnouncedOnce() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 1)))
        notifier.onAlarms(
            "dev-1",
            "truck",
            listOf(alarm(id = 2, alarmType = "Ignition ON", event = "ON"), alarm(id = 1)),
        )

        assertEquals(1, tray.posted.size)
        val (key, title, body) = tray.posted.single()
        assertEquals("vehicle-ignition-dev-1", key)
        assertEquals("Vehicle Engine On", title)
        assertEquals("truck engine is now on · 02:46 PM", body)

        // Polled again with the same log: silence, not a second banner.
        notifier.onAlarms(
            "dev-1",
            "truck",
            listOf(alarm(id = 2, alarmType = "Ignition ON", event = "ON"), alarm(id = 1)),
        )
        assertEquals(1, tray.posted.size, "the same event was announced twice")
    }

    @Test
    fun engineOffReadsAsEngineOff() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 1, alarmType = "Ignition ON", event = "ON")))
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 2)))

        val (_, title, body) = tray.posted.single()
        assertEquals("Vehicle Engine Off", title)
        assertEquals("truck engine is now off · 02:46 PM", body)
    }

    @Test
    fun theSameStateTwiceIsNotAChange() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 1, alarmType = "Ignition ON", event = "ON")))
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 2)))
        assertEquals(1, tray.posted.size)

        // A tracker reconnecting can repeat the state it is already in.
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 3)))

        assertEquals(1, tray.posted.size, "a repeated OFF was announced as a change")
    }

    @Test
    fun aStartAndStopBetweenTwoPollsReportsWhereItEndedUp() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 1)))
        notifier.onAlarms(
            "dev-1",
            "truck",
            listOf(
                alarm(id = 3),
                alarm(id = 2, alarmType = "Ignition ON", event = "ON"),
            ),
        )

        // Two banners would be a history lesson; one says what is true now.
        assertEquals(1, tray.posted.size)
        assertEquals("Vehicle Engine Off", tray.posted.single().second)
    }

    @Test
    fun aNonIgnitionRowStillMovesTheMarkForward() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 1)))
        // An SOS arriving after the OFF: nothing announced, but the OFF must not be left
        // looking new or every later poll would re-examine it.
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 2, alarmType = "SOS", event = null)))

        assertTrue(tray.posted.isEmpty())

        // Proof the mark moved: an ON at id 3 is the next change, and it is announced.
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 3, alarmType = "Ignition ON", event = "ON")))
        assertEquals("Vehicle Engine On", tray.posted.single().second)
    }

    @Test
    fun twoVehiclesNeverSpeakForEachOther() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 1)))
        notifier.onAlarms("dev-2", "pickup", listOf(alarm(id = 1)))

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 2, alarmType = "Ignition ON", event = "ON")))

        assertEquals(1, tray.posted.size)
        assertEquals("vehicle-ignition-dev-1", tray.posted.single().first)
        assertTrue(tray.posted.single().third.startsWith("truck"))
    }

    @Test
    fun theMemorySurvivesARestart() {
        val store = InMemoryStatusSpellStore()
        val first = Tray()

        notifier(first, store).onAlarms("dev-1", "truck", listOf(alarm(id = 1)))
        notifier(first, store).onAlarms("dev-1", "truck", listOf(alarm(id = 2)))
        assertEquals(1, first.posted.size)

        // A fresh notifier over the same store — which is what the background check is,
        // in a process the user never opened.
        val second = Tray()
        notifier(second, store).onAlarms("dev-1", "truck", listOf(alarm(id = 2)))

        assertTrue(second.posted.isEmpty(), "a new process re-announced an old event")
    }

    @Test
    fun anEmptyLogSaysNothing() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()

        notifier(tray, store).onAlarms("dev-1", "truck", emptyList())

        assertTrue(tray.posted.isEmpty())
    }

    // ── the state source: same mechanism as Moving / Parked ──────────────────────

    @Test
    fun theEngineStateChangingIsAnnouncedLikeAStatusChange() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        // First poll records the state without a banner — opening the app must not
        // announce an engine that has been off since yesterday.
        notifier.onIgnitionState("dev-1", "truck", "off")
        assertTrue(tray.posted.isEmpty())

        notifier.onIgnitionState("dev-1", "truck", "on")

        val (key, title, body) = tray.posted.single()
        assertEquals("vehicle-ignition-dev-1", key)
        assertEquals("Vehicle Engine On", title)
        assertEquals("truck engine is now on", body)
    }

    @Test
    fun theSameStateOnEveryPollIsSilence() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onIgnitionState("dev-1", "truck", "on")
        repeat(12) { notifier.onIgnitionState("dev-1", "truck", "on") }

        assertTrue(tray.posted.isEmpty(), "an unchanged engine posted a banner")
    }

    @Test
    fun anUnreadableIgnitionLeavesTheVehicleAsItWas() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onIgnitionState("dev-1", "truck", "on")
        // A tracker with no ignition wire, or a fix too old to act on. Not "off" — the
        // app cannot see, which says nothing about the vehicle.
        notifier.onIgnitionState("dev-1", "truck", null)
        notifier.onIgnitionState("dev-1", "truck", "unknown")

        assertTrue(tray.posted.isEmpty())

        // And the remembered state is still ON, so a real OFF is still a change.
        notifier.onIgnitionState("dev-1", "truck", "off")
        assertEquals("Vehicle Engine Off", tray.posted.single().second)
    }

    @Test
    fun theTwoSourcesShareOneMemoryAndDoNotDoubleAnnounce() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        // The state source notices the engine start first — that is the normal case, since
        // it runs on every poll.
        notifier.onIgnitionState("dev-1", "truck", "off")
        notifier.onIgnitionState("dev-1", "truck", "on")
        assertEquals(1, tray.posted.size)

        // The event log then catches up with a row saying the same thing. One banner, not
        // two: the user is told the engine started once.
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 9, alarmType = "Ignition ON", event = "ON")))

        assertEquals(1, tray.posted.size, "both sources announced the same change")
    }

    @Test
    fun theLogIsStillBaselinedAfterTheStateSourceHasSpoken() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onIgnitionState("dev-1", "truck", "on")

        // The log's first read must stay silent even though this device is already known
        // to the state source — its newest row could be from this morning.
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 3528316)))
        assertTrue(tray.posted.isEmpty(), "the first log read announced an old row")

        // Once baselined, the next row is a change and is announced.
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 3528317)))
        assertEquals("Vehicle Engine Off", tray.posted.single().second)
    }

    @Test
    fun twoVehiclesKeepTheirOwnEngineState() {
        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onIgnitionState("dev-1", "truck", "off")
        notifier.onIgnitionState("dev-2", "pickup", "off")
        notifier.onIgnitionState("dev-1", "truck", "on")

        assertEquals(1, tray.posted.size)
        assertEquals("vehicle-ignition-dev-1", tray.posted.single().first)
        assertEquals("truck engine is now on", tray.posted.single().third)
    }

    // ── taking the vehicles in turn ──────────────────────────────────────────────

    @Test
    fun theTickTakesTheVehiclesInTurn() {
        val fleet = listOf("A", "B", "C", "D")

        // One per tick, wrapping: every vehicle is read at a steady rate while the call
        // rate stays at one per tick however large the fleet.
        val order = (0 until 6).map { tick -> rotatedSlice(fleet, tick, 1).single() }

        assertEquals(listOf("A", "B", "C", "D", "A", "B"), order)
    }

    @Test
    fun aCursorPastTheEndStillReadsSomething() {
        // The list is rebuilt from each fleet response, so a vehicle leaving the account
        // can leave the cursor pointing past the end. Without the modulo the tick would
        // read nothing at all for as long as that lasted.
        assertEquals(listOf("B"), rotatedSlice(listOf("A", "B"), start = 5, count = 1))
        assertEquals(listOf("A"), rotatedSlice(listOf("A", "B"), start = 4, count = 1))
    }

    @Test
    fun askingForMoreVehiclesThanExistIsNotAnError() {
        assertEquals(listOf("A", "B"), rotatedSlice(listOf("A", "B"), start = 0, count = 5))
        assertEquals(listOf("B", "A"), rotatedSlice(listOf("A", "B"), start = 1, count = 5))
    }

    @Test
    fun anEmptyFleetReadsNothing() {
        assertTrue(rotatedSlice(emptyList<String>(), start = 0, count = 1).isEmpty())
        assertTrue(rotatedSlice(listOf("A"), start = 0, count = 0).isEmpty())
    }

    @Test
    fun theCadenceMatchesTheFleetPollThatDrivesIt() {
        // The gate inside onFleetPoll must be *under* the interval, not equal to it: the
        // mark is stamped partway through the driving tick, so an exact gate measures a
        // gap a few milliseconds short, skips, and halves the rate to one call per 10 s.
        assertEquals(
            DEVICES_POLL_INTERVAL_MS,
            IGNITION_ALARM_POLL_INTERVAL_MS,
            "the ignition tick rides the fleet poll, so the two intervals have to agree",
        )
    }

    @Test
    fun theCadenceAndTheTurnTogetherStayInsideTheBudget() {
        // 5 s with one vehicle per tick is 12 requests a minute whatever the fleet size.
        // Reading every vehicle every tick is what the arithmetic cannot carry: four
        // vehicles would be 48, on top of the ~24 the fleet and live polls already spend
        // against a host that throttles around 60.
        val perMinute = 60_000 / IGNITION_ALARM_POLL_INTERVAL_MS * IGNITION_DEVICES_PER_TICK

        assertTrue(
            perMinute <= 12,
            "the ignition log would be called $perMinute times a minute",
        )
    }

    // ── the time under the banner ────────────────────────────────────────────────

    @Test
    fun theTimeIsReadStraightOffTheIstStamp() {
        assertEquals("02:46 PM", clockTimeOf("2026-10-28 14:46:07"))
        assertEquals("12:05 AM", clockTimeOf("2026-10-28 00:05:00"))
        assertEquals("12:30 PM", clockTimeOf("2026-10-28 12:30:00"))
        assertEquals("09:16 AM", clockTimeOf("2026-10-28 09:16:07"))
    }

    @Test
    fun aMissingOrOddStampCostsTheTimeAndNotTheSentence() {
        assertEquals("", clockTimeOf(null))
        assertEquals("", clockTimeOf(""))
        assertEquals("", clockTimeOf("2026-10-28"))
        assertEquals("", clockTimeOf("not a date"))

        val tray = Tray()
        val store = InMemoryStatusSpellStore()
        val notifier = notifier(tray, store)

        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 1, alarmType = "Ignition ON", event = "ON")))
        notifier.onAlarms("dev-1", "truck", listOf(alarm(id = 2, occurredAtIst = null)))

        assertEquals("truck engine is now off", tray.posted.single().third)
    }
}
