package com.developer.ev2z

import com.developer.ev2z.network.ApiJson
import com.developer.ev2z.response.DeviceStatusData
import com.developer.ev2z.response.DeviceStatusResponse
import com.developer.ev2z.response.StatusSession
import com.developer.ev2z.response.coveredGround
import com.developer.ev2z.response.latestSessionStatus
import com.developer.ev2z.response.sortKeyOf
import com.developer.ev2z.response.durationLabel
import com.developer.ev2z.response.endClock
import com.developer.ev2z.response.endPoint
import com.developer.ev2z.response.formatDurationSeconds
import com.developer.ev2z.response.LegStart
import com.developer.ev2z.response.asLegs
import com.developer.ev2z.response.trackedSeconds
import com.developer.ev2z.response.of
import com.developer.ev2z.response.sessionCount
import com.developer.ev2z.response.startClock
import com.developer.ev2z.response.startPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `device-status` describes a session's length five times over, only one of those
 * numbers is exact, and its stamps are already local.
 *
 * The crash that sent us here: `duration_minutes` was modelled as `Int`, the endpoint
 * sent `2.53` for a 152-second session, and the JsonDecodingException took down the
 * whole response — every session in it, not just the short one — on the main thread.
 */
class DeviceStatusSessionTest {

    /**
     * Verbatim from the real payload, 2026-10-15, device 0356218606631271 — trimmed to
     * four of its eleven sessions: the overnight one, the one-minute park, and a
     * twelve-second inferred offline gap between two moving legs.
     */
    private val day = """
        {
          "success": true,
          "data": {
            "status_count": { "parked": 2, "moving": 6, "idle": 0, "offline": 3 },
            "total_sessions": 11,
            "timezone": "Asia/Kolkata",
            "sessions": [
              {
                "status": "moving",
                "start_time": "2026-10-15 12:00:47 AM",
                "end_time": "2026-10-15 10:29:15 AM",
                "start_latitude": "28.671751",
                "start_longitude": "77.439052",
                "end_latitude": "28.683072",
                "end_longitude": "77.509252",
                "latitude": "28.683072",
                "longitude": "77.509252",
                "duration_seconds": 37708,
                "duration_minutes": 628.47,
                "duration_hours": 10.47,
                "duration_formatted": "10 hours 28 minutes 28 seconds",
                "duration_detail": {
                  "hours": 10, "minutes": 28, "seconds": 28,
                  "formatted": "10 hours 28 minutes 28 seconds"
                },
                "offline_inferred": false
              },
              {
                "status": "parked",
                "start_time": "2026-10-15 10:29:15 AM",
                "end_time": "2026-10-15 10:30:20 AM",
                "start_latitude": "28.683169",
                "start_longitude": "77.509847",
                "end_latitude": "28.683169",
                "end_longitude": "77.509847",
                "latitude": "28.683169",
                "longitude": "77.509847",
                "duration_seconds": 65,
                "duration_minutes": 1.08,
                "duration_hours": 0.02,
                "duration_formatted": "0 hours 1 minutes 5 seconds",
                "duration_detail": {
                  "hours": 0, "minutes": 1, "seconds": 5,
                  "formatted": "0 hours 1 minutes 5 seconds"
                },
                "offline_inferred": false
              },
              {
                "status": "offline",
                "start_time": "2026-10-15 10:43:28 AM",
                "end_time": "2026-10-15 10:43:40 AM",
                "start_latitude": "28.667341",
                "start_longitude": "77.483736",
                "end_latitude": "28.662076",
                "end_longitude": "77.478382",
                "latitude": "28.662076",
                "longitude": "77.478382",
                "duration_seconds": 12,
                "duration_minutes": 0.2,
                "duration_hours": 0,
                "duration_formatted": "0 hours 0 minutes 12 seconds",
                "duration_detail": {
                  "hours": 0, "minutes": 0, "seconds": 12,
                  "formatted": "0 hours 0 minutes 12 seconds"
                },
                "offline_inferred": true
              },
              {
                "status": "moving",
                "start_time": "2026-10-15 11:18:48 AM",
                "end_time": "2026-10-15 11:28:12 AM",
                "start_latitude": "28.652691",
                "start_longitude": "77.472332",
                "end_latitude": "28.665613",
                "end_longitude": "77.48099",
                "latitude": "28.665613",
                "longitude": "77.48099",
                "duration_seconds": 564,
                "duration_minutes": 9.4,
                "duration_hours": 0.16,
                "duration_formatted": "0 hours 9 minutes 24 seconds",
                "duration_detail": {
                  "hours": 0, "minutes": 9, "seconds": 24,
                  "formatted": "0 hours 9 minutes 24 seconds"
                },
                "offline_inferred": false
              }
            ]
          }
        }
    """.trimIndent()

    private val data = ApiJson
        .decodeFromString(DeviceStatusResponse.serializer(), day)
        .data!!

    @Test
    fun theWholeDayDecodesIncludingTheSubMinuteSessions() {
        assertEquals(4, data.sessions.size)
        assertEquals(628.47, data.sessions[0].duration_minutes)
        assertEquals(0.2, data.sessions[2].duration_minutes)
        assertEquals("Asia/Kolkata", data.timezone)
    }

    /** The server's own count, not the page's length — eleven sessions, four sent here. */
    @Test
    fun theHeaderCountComesFromTheServer() {
        assertEquals(11, data.sessionCount)
        assertEquals(6, data.status_count.of("moving"))
        assertEquals(0, data.status_count.of("idle"))
        assertEquals(3, data.status_count.of("offline"))
    }

    /**
     * `duration_formatted` is "0 hours 1 minutes 5 seconds" whatever the length. The
     * row shows only the units that are not zero, and a twelve-second signal gap has
     * to stay visible rather than rounding to "0 min".
     */
    @Test
    fun durationsDropTheUnitsThatRoundToNothing() {
        assertEquals("10h 28m", data.sessions[0].durationLabel())
        assertEquals("1m 5s", data.sessions[1].durationLabel())
        assertEquals("12s", data.sessions[2].durationLabel())
        assertEquals("9m 24s", data.sessions[3].durationLabel())

        assertEquals("0s", formatDurationSeconds(0))
        assertEquals("12m", formatDurationSeconds(720))
        assertEquals("2h", formatDurationSeconds(7200))
    }

    /**
     * The stamps arrive in the response's own timezone and in twelve-hour form. Running
     * them through the UTC→IST converter the screen used to call added five and a half
     * hours to a local time.
     */
    @Test
    fun clocksAreShownAsTheServerSentThem() {
        assertEquals("12:00 AM", data.sessions[0].startClock())
        assertEquals("10:29 AM", data.sessions[0].endClock())
        assertEquals("11:28 AM", data.sessions[3].endClock())
    }

    /**
     * Some days of this endpoint send twenty-four-hour stamps with no meridiem at all.
     * The morning or evening is worked out rather than dropped: "09:36" beside
     * "04:59" leaves the reader no way to tell which half of the day either belongs to,
     * and the overnight session is half of most reports.
     */
    @Test
    fun aTwentyFourHourStampStillGetsItsAmOrPm() {
        fun clockOf(stamp: String) = ApiJson
            .decodeFromString(
                DeviceStatusResponse.serializer(),
                """
                {
                  "success": true,
                  "data": {
                    "status_count": { "moving": 1, "parked": 0, "idle": 0, "offline": 0 },
                    "sessions": [
                      { "status": "moving", "start_time": "$stamp", "end_time": "$stamp" }
                    ]
                  }
                }
                """.trimIndent()
            )
            .data!!.sessions.single().startClock()

        assertEquals("09:36 AM", clockOf("2026-10-15 09:36:57"))
        assertEquals("12:05 AM", clockOf("2026-10-15 00:05:00"))
        assertEquals("12:30 PM", clockOf("2026-10-15 12:30:00"))
        assertEquals("01:05 PM", clockOf("2026-10-15 13:05:44"))
        assertEquals("11:59 PM", clockOf("2026-10-15 23:59:01"))

        // A meridiem the server did send is never second-guessed.
        assertEquals("04:59 AM", clockOf("2026-10-15 04:59:15 AM"))
        assertEquals("08:37 PM", clockOf("2026-10-14 08:37:16 PM"))
    }

    /**
     * A moving session covers ground and gets both ends; a parked one ends where it
     * began, and two identical address lines read as a bug.
     */
    @Test
    fun movingSessionsHaveTwoEndsAndParkedOnesHaveOne() {
        assertTrue(data.sessions[0].coveredGround())
        assertEquals(28.671751 to 77.439052, data.sessions[0].startPoint())
        assertEquals(28.683072 to 77.509252, data.sessions[0].endPoint())

        assertFalse(data.sessions[1].coveredGround())
        assertEquals(data.sessions[1].startPoint(), data.sessions[1].endPoint())
    }

    @Test
    fun theInferredFlagSurvivesTheDecode() {
        assertTrue(data.sessions[2].offline_inferred)
        assertFalse(data.sessions[0].offline_inferred)
    }

    /**
     * The same endpoint, a day later: `status_duration` added, and the per-session
     * `start_`/`end_` coordinates and `offline_inferred` gone again. Both shapes have
     * to render, which is why every field is defaulted and the card falls back to the
     * single coordinate.
     */
    @Test
    fun theShapeWithPerStatusTotalsAndNoSessionEndpointsDecodes() {
        val withTotals = """
            {
              "success": true,
              "data": {
                "status_count": { "moving": 4, "parked": 3, "idle": 0, "offline": 0 },
                "status_duration": {
                  "parked": {
                    "count": 3, "total_seconds": 1, "total_minutes": 0.02,
                    "total_hours": 0, "formatted": "0 hours 0 minutes 1 seconds"
                  },
                  "moving": {
                    "count": 4, "total_seconds": 24596, "total_minutes": 409.93,
                    "total_hours": 6.83, "formatted": "6 hours 49 minutes 56 seconds"
                  },
                  "idle": {
                    "count": 0, "total_seconds": 0, "total_minutes": 0,
                    "total_hours": 0, "formatted": "0 hours 0 minutes 0 seconds"
                  },
                  "offline": {
                    "count": 0, "total_seconds": 0, "total_minutes": 0,
                    "total_hours": 0, "formatted": "0 hours 0 minutes 0 seconds"
                  }
                },
                "total_sessions": 7,
                "timezone": "Asia/Kolkata",
                "sessions": [
                  {
                    "status": "moving",
                    "start_time": "2026-10-15 12:00:50 AM",
                    "end_time": "2026-10-15 04:59:14 AM",
                    "latitude": "28.683072",
                    "longitude": "77.509252",
                    "duration_seconds": 17904,
                    "duration_minutes": 298.4,
                    "duration_hours": 4.97,
                    "duration_detail": {
                      "hours": 4, "minutes": 58, "seconds": 24,
                      "formatted": "4 hours 58 minutes 24 seconds"
                    }
                  },
                  {
                    "status": "parked",
                    "start_time": "2026-10-15 04:59:15 AM",
                    "end_time": "2026-10-15 04:59:15 AM",
                    "latitude": "28.683169",
                    "longitude": "77.509847",
                    "duration_seconds": 0,
                    "duration_minutes": 0,
                    "duration_hours": 0,
                    "duration_detail": {
                      "hours": 0, "minutes": 0, "seconds": 0,
                      "formatted": "0 hours 0 minutes 0 seconds"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val day = ApiJson
            .decodeFromString(DeviceStatusResponse.serializer(), withTotals)
            .data!!

        // The totals the summary adds up for the user.
        val totals = day.status_duration!!
        assertEquals(24596, totals.of("moving").total_seconds)
        assertEquals("6h 49m", formatDurationSeconds(totals.of("moving").total_seconds))
        assertEquals(24597, totals.trackedSeconds)
        assertEquals(0, totals.of("idle").total_seconds)

        // No start_/end_ pair: both ends fall back to the one coordinate, so the card
        // draws a single "At" line rather than two identical ones.
        val drive = day.sessions[0]
        assertEquals(28.683072 to 77.509252, drive.startPoint())
        assertEquals(drive.startPoint(), drive.endPoint())
        assertFalse(drive.coveredGround())
        assertEquals("4h 58m", drive.durationLabel())

        // A park of no duration at all still shows both ends — the same place twice,
        // which is the fact that the vehicle did not move.
        val park = day.sessions[1]
        assertEquals("04:59 AM", park.startClock())
        assertEquals("04:59 AM", park.endClock())
        assertEquals("0s", park.durationLabel())
    }

    /**
     * The chain that supplies a start the payload left out.
     *
     * A session carries one coordinate — its end — so a journey's start comes from the
     * session before it: the drive that follows a park begins at that park. A parked
     * session is not filled in this way; handing it the previous end would have it
     * drift eighty metres while standing still.
     */
    @Test
    fun aJourneyStartsWhereThePreviousSessionEnded() {
        val chain = """
            {
              "success": true,
              "data": {
                "status_count": { "moving": 2, "parked": 1, "idle": 0, "offline": 0 },
                "sessions": [
                  {
                    "status": "moving",
                    "start_time": "2026-10-15 12:00:50 AM",
                    "end_time": "2026-10-15 04:59:14 AM",
                    "latitude": "28.683072", "longitude": "77.509252",
                    "duration_seconds": 17904
                  },
                  {
                    "status": "parked",
                    "start_time": "2026-10-15 04:59:15 AM",
                    "end_time": "2026-10-15 04:59:15 AM",
                    "latitude": "28.683169", "longitude": "77.509847",
                    "duration_seconds": 0
                  },
                  {
                    "status": "moving",
                    "start_time": "2026-10-15 05:00:20 AM",
                    "end_time": "2026-10-15 05:48:31 AM",
                    "latitude": "28.658558", "longitude": "77.476092",
                    "duration_seconds": 2891
                  }
                ]
              }
            }
        """.trimIndent()

        val legs = ApiJson
            .decodeFromString(DeviceStatusResponse.serializer(), chain)
            .data!!.sessions.asLegs()

        // The first journey of the day has nothing in front of it: its start is
        // genuinely unknown, and must not be filled in with its own end — that labelled
        // five hours of driving as having gone nowhere.
        assertEquals(LegStart.UNKNOWN, legs[0].startSource)
        assertNull(legs[0].start)
        assertEquals(28.683072 to 77.509252, legs[0].end)
        assertFalse(legs[0].coveredGround)
        assertFalse(legs[0].stayedPut)

        // The park stays in one place — its own fix at both ends, not the drive's.
        assertEquals(LegStart.SAME_PLACE, legs[1].startSource)
        assertEquals(28.683169 to 77.509847, legs[1].start)
        assertEquals(legs[1].start, legs[1].end)
        assertFalse(legs[1].coveredGround)
        assertTrue(legs[1].stayedPut)

        // The second drive begins where that park was, and ends at its own fix.
        assertEquals(LegStart.PREVIOUS_SESSION, legs[2].startSource)
        assertEquals(28.683169 to 77.509847, legs[2].start)
        assertEquals(28.658558 to 77.476092, legs[2].end)
        assertTrue(legs[2].coveredGround)
    }

    /** A payload that states both ends is taken at its word, chain or no chain. */
    @Test
    fun declaredEndpointsBeatTheDerivedOnes() {
        val legs = data.sessions.asLegs()

        assertEquals(LegStart.DECLARED, legs[0].startSource)
        assertEquals(28.671751 to 77.439052, legs[0].start)
        assertEquals(28.683072 to 77.509252, legs[0].end)
        assertTrue(legs[0].coveredGround)

        // The parked session declares the same pair twice: one place, no movement.
        assertEquals(legs[1].start, legs[1].end)
        assertFalse(legs[1].coveredGround)
    }

    /**
     * The endpoint nulls fields per device rather than omitting them — the reason every
     * field carries a default. A session with no fix must cost its own coordinates, not
     * the whole response, and must not be geocoded: "0"/"0" names a spot in the Gulf of
     * Guinea.
     */
    @Test
    fun aSessionWithNoFixKeepsTheRestOfTheResponse() {
        val nulled = """
            {
              "success": true,
              "data": {
                "status_count": { "offline": 1, "moving": 0, "idle": 0, "parked": 0 },
                "sessions": [
                  {
                    "status": "offline",
                    "start_time": "2026-10-14 08:37:16 PM",
                    "end_time": null,
                    "start_latitude": null,
                    "start_longitude": null,
                    "latitude": "0",
                    "longitude": "0",
                    "duration_minutes": null
                  }
                ]
              }
            }
        """.trimIndent()

        val session = ApiJson
            .decodeFromString(DeviceStatusResponse.serializer(), nulled)
            .data?.sessions?.single()!!

        assertEquals("offline", session.status)
        assertEquals("08:37 PM", session.startClock())
        assertEquals(0.0, session.duration_minutes)
        assertEquals("0s", session.durationLabel())
        assertNull(session.startPoint())
        assertNull(session.endPoint())
        assertFalse(session.coveredGround())
    }

    // use to gemini code
    @Test
    fun missingSuccessFieldDefaultsToTrue() {
        val missingSuccess = """
            {
              "data": {
                "status_count": { "offline": 0, "moving": 1, "idle": 0, "parked": 0 },
                "sessions": []
              }
            }
        """.trimIndent()

        val parsed = ApiJson.decodeFromString(DeviceStatusResponse.serializer(), missingSuccess)
        assertTrue(parsed.success)
        assertTrue(parsed.data != null)
    }

    // ── Picking the newest session, for the idle fallback ────────────────────

    private fun session(status: String, endTime: String) =
        StatusSession(status = status, start_time = endTime, end_time = endTime)

    /**
     * Why the list cannot simply be read from the end, and why the stamp cannot be sorted
     * as sent: these four are in the order the real payload sent them, the last row is
     * not the latest one, and "02:40:19 PM" is below "11:19:48 AM" as a string.
     */
    @Test
    fun theNewestSessionIsTheOneWithTheLatestEndTime() {
        val data = DeviceStatusData(
            sessions = listOf(
                session("parked", "2026-10-22 11:19:48 AM"),
                session("idle", "2026-10-22 02:40:19 PM"),
                session("moving", "2026-10-22 02:39:49 PM"),
            )
        )

        assertEquals("idle", data.latestSessionStatus())
    }

    /** The twenty-four-hour form this endpoint sends on some days. */
    @Test
    fun aTwentyFourHourStampIsTakenAsAlreadyInThatForm() {
        val data = DeviceStatusData(
            sessions = listOf(
                session("idle", "2026-10-22 09:36:57"),
                session("parked", "2026-10-22 14:02:11"),
            )
        )

        assertEquals("parked", data.latestSessionStatus())
    }

    @Test
    fun midnightAndNoonCrossTheMeridiemCorrectly() {
        assertEquals("2026-10-22T000047", sortKeyOf("2026-10-22 12:00:47 AM"))
        assertEquals("2026-10-22T120047", sortKeyOf("2026-10-22 12:00:47 PM"))
        assertEquals("2026-10-22T233000", sortKeyOf("2026-10-22 11:30:00 PM"))
    }

    /** A session that ran past midnight sorts above one that ended earlier the same day. */
    @Test
    fun theDateIsPartOfTheComparison() {
        val data = DeviceStatusData(
            sessions = listOf(
                session("moving", "2026-10-23 12:04:00 AM"),
                session("idle", "2026-10-22 11:58:00 PM"),
            )
        )

        assertEquals("moving", data.latestSessionStatus())
    }

    /**
     * A malformed stamp is dropped from the comparison rather than sorting first — null
     * would otherwise be the smallest key and could win by default.
     */
    @Test
    fun anUnparseableStampIsIgnoredRatherThanWinning() {
        assertNull(sortKeyOf(""))
        assertNull(sortKeyOf("not a stamp"))
        assertNull(sortKeyOf("2026-10-22 25:00:00"))

        val data = DeviceStatusData(
            sessions = listOf(
                session("moving", "nonsense"),
                session("idle", "2026-10-22 02:40:19 PM"),
            )
        )

        assertEquals("idle", data.latestSessionStatus())
    }

    @Test
    fun aDayWithNoSessionsHasNoNewestOne() {
        assertNull(DeviceStatusData(sessions = emptyList()).latestSessionStatus())
    }
}
