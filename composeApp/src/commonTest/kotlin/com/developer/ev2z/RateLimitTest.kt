package com.developer.ev2z

import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.ApiJson
import com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS
import com.developer.ev2z.network.DEVICES_SHARE_WINDOW_MS
import com.developer.ev2z.network.LIVE_LOCATION_POLL_INTERVAL_MS
import com.developer.ev2z.network.RateLimitGate
import com.developer.ev2z.network.RateLimitedException
import com.developer.ev2z.network.SharedFleetLocations
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Runs a suspending test body inline, without `kotlinx-coroutines-test` — which this
 * project does not depend on.
 *
 * Enough here because nothing in these tests actually suspends: the fetch lambdas answer
 * immediately and an uncontended `Mutex` takes its fast path. It says so out loud if that
 * stops being true, rather than passing a test whose assertions never ran.
 */
private fun runSharing(block: suspend () -> Unit) {
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
 * The 429 storm of 2026-10-26 17:54, which looked to the user like GPS tracking turning
 * itself off.
 *
 * Both polled endpoints were at 1 s, which is twice the server's budget on its own. Past
 * the budget Laravel answered 429 on every route of the host, and because
 * [DeviceResponse] defaults every field, the throttle page decoded into a valid-looking
 * fleet reply carrying zero devices — which the home map then drew. Every marker
 * vanished, nothing logged an error, and the pollers kept asking once a second so the
 * window never cleared.
 *
 * What has to hold: a throttle body is never mistaken for a fleet, the intervals stay
 * inside the budget, and screens wanting the same fleet share one request.
 */
class RateLimitTest {

    @BeforeTest
    fun reset() {
        RateLimitGate.open()
        SharedFleetLocations.clear()
    }

    @AfterTest
    fun tidy() {
        RateLimitGate.open()
        SharedFleetLocations.clear()
    }

    /** Laravel's throttle page, as `gps.eva2z.in` actually sends it. */
    private val throttleBody = """
        {
          "message": "Too Many Attempts.",
          "exception": "Illuminate\\Http\\Exceptions\\ThrottleRequestsException",
          "file": "/var/www/evatoz_gps_server/vendor/laravel/framework/src/Illuminate/Routing/Middleware/ThrottleRequests.php",
          "line": 232
        }
    """.trimIndent()

    @Test
    fun theThrottlePageDecodesIntoSomethingThatLooksLikeAnEmptyFleet() {
        // This is the trap, and it is why the status has to be checked before the body is
        // decoded: the throttle page parses, so nothing downstream can tell it from an
        // account with no vehicles. Asserted rather than fixed — the defaults are
        // deliberate and are what stops one missing key from blanking the map.
        val decoded = ApiJson.decodeFromString(DeviceResponse.serializer(), throttleBody)

        assertFalse(decoded.success, "a throttle body has no success field")
        assertEquals(0, decoded.totalDevices)
        assertTrue(decoded.data.isEmpty())
    }

    @Test
    fun aThrottledFleetCallFailsInsteadOfReturningNoDevices() = runSharing {
        var thrown: Throwable? = null

        try {
            SharedFleetLocations.get("token", DEVICES_SHARE_WINDOW_MS) {
                RateLimitGate.close(5_000)
                throw RateLimitedException(5_000)
            }
        } catch (e: RateLimitedException) {
            thrown = e
        }

        assertEquals(5_000, (thrown as? RateLimitedException)?.retryAfterMs)
        // And the pollers are held off rather than left hammering the window.
        assertTrue(RateLimitGate.isClosed)
    }

    @Test
    fun theGateKeepsTheLongestHold() {
        RateLimitGate.close(10_000)

        // A second screen taking its own 429 a moment later must not shorten the wait.
        RateLimitGate.close(1_000)

        assertTrue(
            RateLimitGate.remainingMs() > 1_000,
            "a shorter hold shortened a longer one (${RateLimitGate.remainingMs()} ms left)",
        )
    }

    @Test
    fun aSuccessfulResponseOpensTheGate() {
        RateLimitGate.close(10_000)
        assertTrue(RateLimitGate.isClosed)

        RateLimitGate.open()

        assertFalse(RateLimitGate.isClosed)
        assertEquals(0, RateLimitGate.remainingMs())
    }

    @Test
    fun screensAskingInsideTheWindowCostOneRequest() = runSharing {
        var requests = 0
        val fleet = DeviceResponse(success = true, totalDevices = 4)

        // The home map, the Track & Trace dropdown and the Device Report dropdown, each
        // on its own timer: the log has two of them 80 ms apart, asking for identical
        // data and paying for it twice.
        val answers = List(3) {
            SharedFleetLocations.get("token", DEVICES_SHARE_WINDOW_MS) {
                requests++
                fleet
            }
        }

        assertEquals(1, requests, "three callers inside one window spent $requests requests")
        assertTrue(answers.all { it.totalDevices == 4 })
    }

    @Test
    fun oneThrottledResponseAnswersEveryScreenInTheWindow() = runSharing {
        var requests = 0
        var failures = 0

        // Each caller still sees the failure — none of them is handed an empty fleet —
        // but the throttled server is only asked once.
        repeat(3) {
            try {
                SharedFleetLocations.get("token", DEVICES_SHARE_WINDOW_MS) {
                    requests++
                    throw RateLimitedException(5_000)
                }
            } catch (_: RateLimitedException) {
                failures++
            }
        }

        assertEquals(1, requests, "a throttled window cost $requests requests")
        assertEquals(3, failures, "a caller was handed something other than the failure")
    }

    @Test
    fun aStaleAnswerIsNotServed() = runSharing {
        var requests = 0

        // freshForMs = 0: every caller is past the window, so every caller pays. This is
        // what keeps the fleet moving rather than freezing on one cached response.
        repeat(2) {
            SharedFleetLocations.get("token", freshForMs = 0) {
                requests++
                DeviceResponse(success = true, totalDevices = 4)
            }
        }

        assertEquals(2, requests)
    }

    @Test
    fun anotherAccountIsNeverServedThisOnesFleet() = runSharing {
        val first = SharedFleetLocations.get("token-a", DEVICES_SHARE_WINDOW_MS) {
            DeviceResponse(success = true, totalDevices = 4)
        }
        val second = SharedFleetLocations.get("token-b", DEVICES_SHARE_WINDOW_MS) {
            DeviceResponse(success = true, totalDevices = 1)
        }

        assertEquals(4, first.totalDevices)
        assertEquals(
            1,
            second.totalDevices,
            "a second account was handed the first one's vehicles",
        )
    }

    @Test
    fun theIntervalsStayInsideTheServersBudget() {
        // The fleet poll is shared across screens, so these two endpoints are the floor
        // of what the app spends. At 1 s each they asked 120 times a minute against a
        // host that throttles at 60 — which is what started the storm.
        val perMinute =
            60_000 / DEVICES_POLL_INTERVAL_MS + 60_000 / LIVE_LOCATION_POLL_INTERVAL_MS

        assertTrue(
            perMinute <= 30,
            "polling would spend $perMinute requests a minute, leaving nothing for the rest of the app",
        )
    }

    @Test
    fun theShareWindowIsShorterThanTheInterval() {
        // At or past the interval the first poller of every round would be served the
        // previous round's answer, and the fleet would update at half its stated rate.
        assertTrue(
            DEVICES_SHARE_WINDOW_MS < DEVICES_POLL_INTERVAL_MS,
            "share window $DEVICES_SHARE_WINDOW_MS is not shorter than interval $DEVICES_POLL_INTERVAL_MS",
        )
    }
}
