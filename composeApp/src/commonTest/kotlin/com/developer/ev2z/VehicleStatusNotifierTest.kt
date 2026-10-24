package com.developer.ev2z

import com.developer.ev2z.util.InMemoryStatusSpellStore
import com.developer.ev2z.util.StatusSpellStore
import com.developer.ev2z.util.VehicleStatusNotifier
import com.developer.ev2z.util.VehicleStatusSample
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Moving / Idle / Parked notifications the app raises for itself.
 *
 * The contract, in one line: a vehicle's status changing is a notification, a vehicle's
 * status staying put is silence — per vehicle, across polls, and across the processes
 * the checks run in.
 */
class VehicleStatusNotifierTest {

    private class Spy {
        val posted = mutableListOf<Triple<String, String, String>>()
        fun post(key: String, title: String, body: String) {
            posted += Triple(key, title, body)
        }
        fun titles() = posted.map { it.second }
    }

    private fun sample(status: String, id: String = "D1", name: String = "Bike") =
        listOf(VehicleStatusSample(deviceId = id, deviceName = name, status = status))

    // ── The three statuses ───────────────────────────────────────────────────

    @Test
    fun announcesEachStatusAsItArrives() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(sample("moving"))
        notifier.onPoll(sample("idle"))
        notifier.onPoll(sample("parked"))

        assertEquals(
            listOf("Vehicle Moving", "Vehicle Idle", "Vehicle Parked"),
            spy.titles(),
        )
        assertEquals(
            listOf(
                Triple("vehicle-running-D1", "Vehicle Moving", "Bike is now moving"),
                Triple("vehicle-idle-D1", "Vehicle Idle", "Bike is now idle"),
                Triple("vehicle-parked-D1", "Vehicle Parked", "Bike is now parked"),
            ),
            spy.posted,
        )
    }

    /** No waiting period: the poll that first reports the status posts the banner. */
    @Test
    fun postsOnTheVeryFirstPollThatReportsTheStatus() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(sample("moving"))

        assertEquals(1, spy.posted.size)
    }

    @Test
    fun announcesEveryGenuineChangeBackAndForth() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        // A drive through town: pulls away, stops at lights, drives on, parks, leaves.
        listOf("moving", "idle", "moving", "parked", "moving").forEach {
            notifier.onPoll(sample(it))
        }

        assertEquals(
            listOf(
                "Vehicle Moving", "Vehicle Idle", "Vehicle Moving",
                "Vehicle Parked", "Vehicle Moving",
            ),
            spy.titles(),
        )
    }

    // ── No duplicates ────────────────────────────────────────────────────────

    @Test
    fun saysNothingWhileTheStatusStaysTheSame() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        // One change, then the five-second poll running over an unchanged fleet.
        repeat(200) { notifier.onPoll(sample("parked")) }

        assertEquals(1, spy.posted.size, "one change is one notification")
    }

    /** "ideal" is how this endpoint spells idle, "running" how it spells moving. */
    @Test
    fun treatsTheServersSpellingsAsTheSameStatus() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(sample("moving"))
        notifier.onPoll(sample("RUNNING"))
        notifier.onPoll(sample("running"))
        assertEquals(listOf("Vehicle Moving"), spy.titles())

        notifier.onPoll(sample("Ideal"))
        notifier.onPoll(sample("ideal"))
        notifier.onPoll(sample("IDLE"))
        assertEquals(listOf("Vehicle Moving", "Vehicle Idle"), spy.titles())
    }

    /**
     * A tracker that goes quiet says nothing about the vehicle, so coming back is not a
     * change — otherwise every tunnel on the route would re-announce the drive.
     */
    @Test
    fun aSignalDropIsNotAStatusChange() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(sample("moving"))
        notifier.onPoll(sample("offline"))
        notifier.onPoll(sample("moving"))

        assertEquals(listOf("Vehicle Moving"), spy.titles())
    }

    /**
     * Ignition off is about the vehicle rather than the signal. It has no banner of its
     * own — it is not one of the three — but it is a different state, so driving away
     * afterwards is a change worth announcing.
     */
    @Test
    fun ignitionOffHasNoBannerButIsStillAStateOfItsOwn() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(sample("moving"))
        notifier.onPoll(sample("off"))
        assertEquals(listOf("Vehicle Moving"), spy.titles(), "ignition off is not one of the three")

        notifier.onPoll(sample("moving"))
        assertEquals(listOf("Vehicle Moving", "Vehicle Moving"), spy.titles())
    }

    // ── Several vehicles ─────────────────────────────────────────────────────

    @Test
    fun tracksEachVehicleSeparately() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(
            listOf(
                VehicleStatusSample("D1", "Bike", "moving"),
                VehicleStatusSample("D2", "truck", "parked"),
            )
        )
        assertEquals(2, spy.posted.size, "two vehicles, two first statuses")

        // Only the truck moves off; the bike is still driving and must stay quiet.
        notifier.onPoll(
            listOf(
                VehicleStatusSample("D1", "Bike", "moving"),
                VehicleStatusSample("D2", "truck", "moving"),
            )
        )

        assertEquals(3, spy.posted.size)
        assertEquals(
            Triple("vehicle-running-D2", "Vehicle Moving", "truck is now moving"),
            spy.posted.last(),
        )
    }

    @Test
    fun oneVehiclesChangeDoesNotSilenceAnother() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(
            listOf(
                VehicleStatusSample("D1", "Bike", "parked"),
                VehicleStatusSample("D2", "truck", "parked"),
            )
        )
        spy.posted.clear()

        // Both change in the same poll — both are announced.
        notifier.onPoll(
            listOf(
                VehicleStatusSample("D1", "Bike", "moving"),
                VehicleStatusSample("D2", "truck", "idle"),
            )
        )

        assertEquals(listOf("Vehicle Moving", "Vehicle Idle"), spy.titles())
    }

    // ── Across processes ─────────────────────────────────────────────────────

    /**
     * The background check gets a fresh process each time the alarm fires, so the last
     * status has to come out of storage. In memory alone, every check would see every
     * vehicle for the first time and announce it again — a banner a minute, forever.
     */
    @Test
    fun theLastStatusSurvivesTheProcessThatRecordedIt() {
        val spy = Spy()
        val store = InMemoryStatusSpellStore()

        VehicleStatusNotifier(store = store, post = spy::post).onPoll(sample("moving"))
        assertEquals(1, spy.posted.size)

        // A later check, in a process that has only the stored state to go on.
        VehicleStatusNotifier(store = store, post = spy::post).onPoll(sample("moving"))
        assertEquals(1, spy.posted.size, "still moving — nothing to say")

        VehicleStatusNotifier(store = store, post = spy::post).onPoll(sample("parked"))
        assertEquals(listOf("Vehicle Moving", "Vehicle Parked"), spy.titles())
    }

    /** The foreground poll runs every five seconds; an unchanged fleet must not write. */
    @Test
    fun storageIsWrittenOnlyWhenSomethingChanged() {
        val counting = object : StatusSpellStore {
            var writes = 0
            private var value = ""
            override fun read() = value
            override fun write(value: String) {
                writes++
                this.value = value
            }
        }
        val notifier = VehicleStatusNotifier(store = counting, post = Spy()::post)

        notifier.onPoll(sample("moving"))
        val afterFirst = counting.writes
        repeat(10) { notifier.onPoll(sample("moving")) }

        assertEquals(afterFirst, counting.writes, "an unchanged status is not a write")
    }

    @Test
    fun resetForgetsEveryVehicle() {
        val spy = Spy()
        val notifier = VehicleStatusNotifier(post = spy::post)

        notifier.onPoll(sample("moving"))
        notifier.reset()
        notifier.onPoll(sample("moving"))

        assertEquals(2, spy.posted.size, "after a reset the vehicle is newly seen")
    }

    @Test
    fun ignoresASampleWithNoDeviceId() {
        val spy = Spy()
        VehicleStatusNotifier(post = spy::post)
            .onPoll(listOf(VehicleStatusSample("", "Bike", "moving")))

        assertEquals(0, spy.posted.size)
    }
}
