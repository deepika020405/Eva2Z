package com.developer.ev2z

import com.developer.ev2z.data.local.model.Device
import com.developer.ev2z.util.IdleStatusResolver
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Runs a suspending test body to completion without `kotlinx-coroutines-test`, which
 * this project does not depend on.
 *
 * Enough because nothing in these tests actually suspends — the ignition lookups are
 * plain map reads — and it says so out loud if that ever stops being true, rather than
 * passing a test whose assertions never ran.
 */
private fun runResolving(block: suspend () -> Unit) {
    var outcome: Result<Unit>? = null

    block.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            outcome = result
        }
    })

    val finished = outcome ?: fail(
        "the test body suspended — this runner only drives work that completes inline"
    )
    finished.getOrThrow()
}

/**
 * The idle derivation — the thing that was missing when the Vehicle Idle notification
 * never fired.
 *
 * The fleet payload labels an engine-on standstill "parked", so idle has to be worked
 * out from the ignition. What matters here is that the lookup is spent only where it
 * can change the answer, and that every way of not getting an answer leaves the
 * server's own word alone.
 */
class IdleStatusResolverTest {

    private class Ignitions(private val answers: Map<String, String?>) {
        var calls = 0
            private set
        val asked = mutableListOf<String>()

        suspend fun lookup(deviceId: String): String? {
            calls++
            asked += deviceId
            return answers[deviceId]
        }
    }

    private fun device(
        id: String = "D1",
        status: String = "parked",
        speed: Int = 0,
        name: String = "Bike",
        lastSeenMinutesAgo: Int? = 0,
    ) = Device(
        deviceId = id,
        deviceName = name,
        status = status,
        speed = speed,
        lastSeenMinutesAgo = lastSeenMinutesAgo,
    )

    /** A clock the test moves by hand, so the cache's TTL is testable without waiting. */
    private class Clock(var nowMs: Long = 0L) {
        fun read(): Long = nowMs
    }

    // ── The derivation ───────────────────────────────────────────────────────

    @Test
    fun aStandstillWithTheIgnitionOnIsIdle() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))

        val samples = IdleStatusResolver().resolve(listOf(device()), ignitions::lookup)

        assertEquals(listOf("idle"), samples.map { it.status })
    }

    @Test
    fun aStandstillWithTheIgnitionOffStaysParked() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "off"))

        val samples = IdleStatusResolver().resolve(listOf(device()), ignitions::lookup)

        assertEquals(listOf("parked"), samples.map { it.status })
    }

    @Test
    fun ignitionCasingDoesNotMatter() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "ON"))

        val samples = IdleStatusResolver().resolve(listOf(device()), ignitions::lookup)

        assertEquals(listOf("idle"), samples.map { it.status })
    }

    @Test
    fun aMovingVehicleIsNeverAskedAboutItsIgnition() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device(status = "moving", speed = 24)), ignitions::lookup)

        assertEquals(listOf("moving"), samples.map { it.status })
        assertEquals(0, ignitions.calls)
    }

    /**
     * The state that says the tracker has gone quiet, not that the vehicle has stopped.
     * An ignition reading cannot make it a motion state, and asking for one is a request
     * spent on an answer that is thrown away.
     */
    @Test
    fun anOfflineDeviceIsLeftAlone() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device(status = "offline")), ignitions::lookup)

        assertEquals(listOf("offline"), samples.map { it.status })
        assertEquals(0, ignitions.calls)
    }

    /** `status: "parked"` with a non-zero speed is a payload disagreeing with itself. */
    @Test
    fun aParkedDeviceStillReportingSpeedIsNotIdle() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device(speed = 12)), ignitions::lookup)

        assertEquals(listOf("parked"), samples.map { it.status })
        assertEquals(0, ignitions.calls)
    }

    @Test
    fun aStaleFixIsNotCalledIdling() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device(lastSeenMinutesAgo = 90)), ignitions::lookup)

        assertEquals(listOf("parked"), samples.map { it.status })
        assertEquals(0, ignitions.calls)
    }

    // ── Not getting an answer ────────────────────────────────────────────────

    /**
     * A tracker with no ignition wire — the Yamaha on the test account answers
     * `{"success":false,"message":"No status found for this device."}`, which the API
     * layer turns into null. With no session lookup behind it there is nothing left to
     * ask, so it keeps the server's word.
     */
    @Test
    fun aDeviceWithNoIgnitionToReportStaysParked() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))

        val samples = IdleStatusResolver().resolve(listOf(device()), ignitions::lookup)

        assertEquals(listOf("parked"), samples.map { it.status })
    }

    // ── The session fallback ─────────────────────────────────────────────────

    /**
     * The reason this exists: on a tracker with no ignition line the rule above can never
     * fire, and the Idle notification that works on one vehicle of an account is simply
     * missing on the next. `status-details` keeps idle sessions for exactly those
     * devices.
     */
    @Test
    fun anUnreadableIgnitionFallsBackToTheNewestSession() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to "idle"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device()), ignitions::lookup, sessions::lookup)

        assertEquals(listOf("idle"), samples.map { it.status })
    }

    /** "ideal" is how this endpoint has been seen spelling it. */
    @Test
    fun theSessionSpellingGoesThroughStatusKindOf() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to "Ideal"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device()), ignitions::lookup, sessions::lookup)

        assertEquals(listOf("idle"), samples.map { it.status })
    }

    /**
     * The guard that keeps the fallback honest. The Yamaha's whole day comes back as one
     * fifteen-hour "moving" session while the fleet payload reports it parked and
     * standing still; a fallback that believed the session would post a Vehicle Moving
     * banner for a vehicle in a car park.
     */
    @Test
    fun aSessionThatIsNotIdleNeverOverrulesTheFeed() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to "moving"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device(status = "parked")), ignitions::lookup, sessions::lookup)

        assertEquals(listOf("parked"), samples.map { it.status })
    }

    /** A readable ignition is the better source, and settles it without a second call. */
    @Test
    fun aWorkingIgnitionIsNeverSecondGuessedBySessions() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "off", "D2" to "on"))
        val sessions = Ignitions(mapOf("D1" to "idle", "D2" to "parked"))

        val samples = IdleStatusResolver().resolve(
            listOf(device(id = "D1"), device(id = "D2")),
            ignitions::lookup,
            sessions::lookup,
        )

        assertEquals(listOf("parked", "idle"), samples.map { it.status })
        assertEquals(0, sessions.calls)
    }

    @Test
    fun aMovingVehicleIsNotAskedAboutItsSessionsEither() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to "idle"))

        val samples = IdleStatusResolver().resolve(
            listOf(device(status = "moving", speed = 31)),
            ignitions::lookup,
            sessions::lookup,
        )

        assertEquals(listOf("moving"), samples.map { it.status })
        assertEquals(0, sessions.calls)
    }

    @Test
    fun theSessionLookupIsCachedLikeTheIgnitionOne() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to "idle"))
        val clock = Clock()
        val resolver = IdleStatusResolver(now = clock::read, cacheTtlMs = 30_000L)

        repeat(6) {
            resolver.resolve(listOf(device()), ignitions::lookup, sessions::lookup)
            clock.nowMs += 5_000L
        }

        assertEquals(1, sessions.calls)
    }

    /**
     * The two answers describe the same device in different vocabularies, so one cache
     * keyed by device id would have each overwrite the other — and "on" read back as a
     * session would not be idle, nor "idle" read back as an ignition.
     */
    @Test
    fun theTwoCachesDoNotStandOnEachOther() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to "idle"))
        val clock = Clock()
        val resolver = IdleStatusResolver(now = clock::read, cacheTtlMs = 30_000L)

        val first = resolver.resolve(listOf(device()), ignitions::lookup, sessions::lookup)
        clock.nowMs += 5_000L
        val second = resolver.resolve(listOf(device()), ignitions::lookup, sessions::lookup)

        assertEquals(listOf("idle"), first.map { it.status })
        assertEquals(listOf("idle"), second.map { it.status })
        assertEquals(1, ignitions.calls)
        assertEquals(1, sessions.calls)
    }

    @Test
    fun resetForgetsTheCachedSessionsToo() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to "idle"))
        val clock = Clock()
        val resolver = IdleStatusResolver(now = clock::read, cacheTtlMs = 30_000L)

        resolver.resolve(listOf(device()), ignitions::lookup, sessions::lookup)
        resolver.reset()
        resolver.resolve(listOf(device()), ignitions::lookup, sessions::lookup)

        assertEquals(2, sessions.calls)
    }

    /** A day with no sessions at all, and a dropped request, are the same answer. */
    @Test
    fun noSessionToReadLeavesTheServersWordAlone() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val sessions = Ignitions(mapOf("D1" to null))

        val samples = IdleStatusResolver()
            .resolve(listOf(device()), ignitions::lookup, sessions::lookup)

        assertEquals(listOf("parked"), samples.map { it.status })
    }

    // ── What keeps it inside the rate limit ──────────────────────────────────

    @Test
    fun theIgnitionIsLookedUpOncePerCacheWindow() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))
        val clock = Clock()
        val resolver = IdleStatusResolver(now = clock::read, cacheTtlMs = 30_000L)

        // Six polls across 25 seconds — the 5-second fleet poll.
        repeat(6) {
            resolver.resolve(listOf(device()), ignitions::lookup)
            clock.nowMs += 5_000L
        }

        assertEquals(1, ignitions.calls)
    }

    @Test
    fun theIgnitionIsLookedUpAgainOnceTheWindowPasses() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))
        val clock = Clock()
        val resolver = IdleStatusResolver(now = clock::read, cacheTtlMs = 30_000L)

        resolver.resolve(listOf(device()), ignitions::lookup)
        clock.nowMs += 31_000L
        resolver.resolve(listOf(device()), ignitions::lookup)

        assertEquals(2, ignitions.calls)
    }

    /** Otherwise a device whose ignition endpoint is down is asked every five seconds. */
    @Test
    fun aFailedLookupIsCachedTooSoItIsNotRetriedEveryPoll() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to null))
        val clock = Clock()
        val resolver = IdleStatusResolver(now = clock::read, cacheTtlMs = 30_000L)

        repeat(4) {
            resolver.resolve(listOf(device()), ignitions::lookup)
            clock.nowMs += 5_000L
        }

        assertEquals(1, ignitions.calls)
    }

    @Test
    fun resetForgetsTheCachedIgnitions() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))
        val clock = Clock()
        val resolver = IdleStatusResolver(now = clock::read, cacheTtlMs = 30_000L)

        resolver.resolve(listOf(device()), ignitions::lookup)
        resolver.reset()
        resolver.resolve(listOf(device()), ignitions::lookup)

        assertEquals(2, ignitions.calls)
    }

    // ── A fleet, not a vehicle ───────────────────────────────────────────────

    @Test
    fun eachVehicleIsResolvedOnItsOwn() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on", "D2" to "off", "D3" to null))

        val samples = IdleStatusResolver().resolve(
            listOf(
                device(id = "D1", name = "Idling"),
                device(id = "D2", name = "Parked"),
                device(id = "D3", name = "No wire"),
                device(id = "D4", name = "Driving", status = "moving", speed = 40),
            ),
            ignitions::lookup,
        )

        assertEquals(listOf("idle", "parked", "parked", "moving"), samples.map { it.status })
        assertEquals(listOf("D1", "D2", "D3"), ignitions.asked)
    }

    @Test
    fun theDisplayNameRidesAlongForTheNotificationBody() = runResolving {
        val ignitions = Ignitions(mapOf("D1" to "on"))

        val samples = IdleStatusResolver()
            .resolve(listOf(device(name = "vegus (Eva2z)")), ignitions::lookup)

        assertEquals("vegus (Eva2z)", samples.single().deviceName)
    }

    @Test
    fun aDeviceWithNoIdIsNotLookedUp() = runResolving {
        val ignitions = Ignitions(mapOf("" to "on"))

        IdleStatusResolver().resolve(listOf(device(id = "")), ignitions::lookup)

        assertEquals(0, ignitions.calls)
    }
}
