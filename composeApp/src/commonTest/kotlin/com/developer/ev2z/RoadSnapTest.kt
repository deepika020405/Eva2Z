package com.developer.ev2z

import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.MAX_GAP_FILLS
import com.developer.ev2z.network.MAX_GAP_VIA_POINTS
import com.developer.ev2z.network.MAX_POINTS_PER_SNAP_REQUEST
import com.developer.ev2z.network.MAX_SNAP_GAP_METERS
import com.developer.ev2z.network.MAX_SNAP_POINTS
import com.developer.ev2z.network.RoadSnapPoint
import com.developer.ev2z.network.SnapPlan
import com.developer.ev2z.network.planSnapRequests
import com.developer.ev2z.network.SnappedRoute
import com.developer.ev2z.network.approxDistanceMeters
import com.developer.ev2z.network.assembleSnappedRoute
import com.developer.ev2z.network.buildSnapBatches
import com.developer.ev2z.network.decodePolyline
import com.developer.ev2z.network.GapFill
import com.developer.ev2z.network.findRouteGaps
import com.developer.ev2z.network.formatCoordinate
import com.developer.ev2z.network.parseDirectionsRoute
import com.developer.ev2z.network.parseSnapResponse
import com.developer.ev2z.network.selectPointsToSnap
import com.developer.ev2z.network.spliceGapFills
import com.developer.ev2z.network.thinEvenly
import com.developer.ev2z.network.viaPointsInside
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * History playback used to join the recorded fixes with straight lines, so the route
 * cut across blocks and buildings instead of following the road. The fix lays the
 * trace onto the road network with the Roads API; what has to hold is that the
 * stitched-together path stays in travel order and that every recorded fix still
 * points at the right vertex of it — that mapping is what keeps the playback marker on
 * the drawn line.
 */
class RoadSnapTest {

    @Test
    fun batchesOverlapByOnePointSoTheRoadGeometryJoinsUp() {
        val sent = (0 until 250).toList()

        val batches = buildSnapBatches(sent)

        assertTrue(batches.all { it.size <= MAX_POINTS_PER_SNAP_REQUEST })
        // Each batch picks up where the last one ended, or the two stretches of road
        // would not meet at the seam.
        batches.zipWithNext().forEach { (first, second) ->
            assertEquals(first.last(), second.first())
        }
        assertEquals(sent.first(), batches.first().first())
        assertEquals(sent.last(), batches.last().last())
    }

    @Test
    fun aTwoPointTraceIsOneRequest() {
        assertEquals(listOf(listOf(0, 1)), buildSnapBatches(listOf(0, 1)))
    }

    @Test
    fun fixesAroundAStandingVehicleAreNotWorthARequest() {
        // Drifting about 10 cm per fix — a parked tracker reporting the same spot.
        val noise = List(20) { GeoPoint(28.6139 + it * 0.000_001, 77.2090) }

        val sent = selectPointsToSnap(noise)

        // The first and the last, nothing in between.
        assertEquals(listOf(0, 19), sent)
    }

    @Test
    fun realMovementIsAllSent() {
        // ~110 m apart, well clear of the noise threshold.
        val driving = List(5) { GeoPoint(28.6139 + it * 0.001, 77.2090) }

        assertEquals(listOf(0, 1, 2, 3, 4), selectPointsToSnap(driving))
    }

    @Test
    fun theSeamPointIsNotDrawnTwice() {
        val points = listOf(
            GeoPoint(28.0, 77.0),
            GeoPoint(28.1, 77.1),
            GeoPoint(28.2, 77.2),
        )
        val batches = listOf(listOf(0, 1), listOf(1, 2))
        val responses = listOf(
            listOf(
                RoadSnapPoint(28.0, 77.0, originalIndex = 0),
                RoadSnapPoint(28.05, 77.05, originalIndex = null),   // interpolated
                RoadSnapPoint(28.1, 77.1, originalIndex = 1),
            ),
            listOf(
                RoadSnapPoint(28.1, 77.1, originalIndex = 0),        // the seam
                RoadSnapPoint(28.2, 77.2, originalIndex = 1),
            ),
        )

        val route = assembleSnappedRoute(points, batches, responses)

        assertEquals(
            listOf(
                GeoPoint(28.0, 77.0),
                GeoPoint(28.05, 77.05),
                GeoPoint(28.1, 77.1),
                GeoPoint(28.2, 77.2),
            ),
            route.path,
        )
        // Point 1 is the seam: one vertex, shared, not two.
        assertEquals(listOf(0, 2, 3), route.pathIndexOfPoint)
    }

    @Test
    fun aThinnedOutFixHoldsTheMarkerWhereItIs() {
        val points = List(4) { GeoPoint(28.0 + it * 0.1, 77.0) }
        // Point 1 and 2 were noise and never sent.
        val batches = listOf(listOf(0, 3))
        val responses = listOf(
            listOf(
                RoadSnapPoint(28.0, 77.0, originalIndex = 0),
                RoadSnapPoint(28.15, 77.0, originalIndex = null),
                RoadSnapPoint(28.3, 77.0, originalIndex = 1),
            ),
        )

        val route = assembleSnappedRoute(points, batches, responses)

        // The unsent fixes stay on point 0's vertex rather than jumping to the start.
        assertEquals(listOf(0, 0, 0, 2), route.pathIndexOfPoint)
    }

    @Test
    fun aFailedBatchKeepsItsStretchOfTheRouteDrawn() {
        val points = listOf(
            GeoPoint(28.0, 77.0),
            GeoPoint(28.1, 77.1),
            GeoPoint(28.2, 77.2),
        )
        val batches = listOf(listOf(0, 1), listOf(1, 2))
        val responses = listOf(
            listOf(
                RoadSnapPoint(28.0, 77.0, originalIndex = 0),
                RoadSnapPoint(28.1, 77.1, originalIndex = 1),
            ),
            null,   // this request failed
        )

        val route = assembleSnappedRoute(points, batches, responses)

        // The failed stretch falls back to the recorded fix instead of vanishing.
        assertEquals(points, route.path)
        assertEquals(listOf(0, 1, 2), route.pathIndexOfPoint)
    }

    @Test
    fun thinningNeverOpensAGapTheRoadsApiWillNotInterpolateAcross() {
        // A whole day at 40 m per fix — far more points than one day's request budget.
        val longDay = List(12_000) { GeoPoint(28.0 + it * 0.000_36, 77.0) }

        val sent = selectPointsToSnap(longDay)

        assertTrue(sent.size <= MAX_SNAP_POINTS, "sent ${sent.size} points")
        // The old stride thinning cut one in six here, opening ~240 m gaps on a dense
        // trace and losing the road geometry across them. Spacing is what is capped
        // now, not the stride.
        sent.zipWithNext().forEach { (first, second) ->
            val gap = approxDistanceMeters(longDay[first], longDay[second])
            assertTrue(gap <= MAX_SNAP_GAP_METERS + 40.0, "opened a ${gap.toInt()} m gap")
        }
    }

    @Test
    fun aSparselyReportedStretchIsNeverSentToSnapping() {
        // Measured 2026-10-05: at 600 m spacing the Roads API does not just stop
        // interpolating, it snaps a fix onto a different road and draws confident
        // geometry down it — 201 m from where the vehicle actually drove. Dense runs are
        // snapped; the sparse stretch between them is left for Directions.
        val dense = List(4) { GeoPoint(28.6000 + it * 0.0009, 77.2000) }       // ~100 m
        val far = List(3) { GeoPoint(28.6200 + it * 0.0009, 77.2000) }         // ~2.2 km away
        val points = dense + far

        val plan = planSnapRequests(points.indices.toList(), points)

        assertEquals(2, plan.batches.size, "the sparse hop should split the runs")
        assertEquals(listOf(0, 1, 2, 3), plan.batches[0])
        assertEquals(listOf(4, 5, 6), plan.batches[1])
        // Both batches open a run, so neither shares a seam point with the other.
        assertEquals(setOf(0, 1), plan.runStartBatches)
    }

    @Test
    fun aDenseTraceIsStillOneRunOfBatches() {
        val dense = List(150) { GeoPoint(28.6 + it * 0.0009, 77.2) }

        val plan = planSnapRequests(dense.indices.toList(), dense)

        assertEquals(setOf(0), plan.runStartBatches)
        assertEquals(2, plan.batches.size)
        assertEquals(plan.batches[0].last(), plan.batches[1].first())
    }

    @Test
    fun theHopBetweenTwoRunsIsLeftForTheGapFiller() {
        val points = listOf(
            GeoPoint(28.6000, 77.2000),
            GeoPoint(28.6009, 77.2000),
            GeoPoint(28.6200, 77.2000),   // 2.1 km on — a run of its own
            GeoPoint(28.6209, 77.2000),
        )
        val plan = SnapPlan(
            batches = listOf(listOf(0, 1), listOf(2, 3)),
            runStartBatches = setOf(0, 1),
        )
        val responses = listOf(
            listOf(
                RoadSnapPoint(28.6000, 77.2000, originalIndex = 0),
                RoadSnapPoint(28.6009, 77.2000, originalIndex = 1),
            ),
            listOf(
                RoadSnapPoint(28.6200, 77.2000, originalIndex = 0),
                RoadSnapPoint(28.6209, 77.2000, originalIndex = 1),
            ),
        )

        val route = assembleSnappedRoute(points, plan, responses)

        // Nothing is dropped and nothing is treated as a shared seam.
        assertEquals(4, route.path.size)
        assertEquals(listOf(0, 1, 2, 3), route.pathIndexOfPoint)
        // The hop across the sparse stretch is marked, so the filler routes it.
        assertEquals(listOf(1..2), route.rawSpans)
    }

    @Test
    fun aSparseStretchIsFoundAsAGapToRoute() {
        val points = listOf(
            GeoPoint(28.6139, 77.2090),
            GeoPoint(28.6142, 77.2093),   // ~45 m — road geometry, not a gap
            GeoPoint(28.6270, 77.2190),   // ~1.7 km — the stretch snapping gave up on
        )
        // A route that came back snapped: the short hop carries road geometry, so only
        // the sparse stretch is missing.
        val route = SnappedRoute(points, points.indices.toList())

        val gaps = findRouteGaps(route, points)

        assertEquals(1, gaps.size)
        assertEquals(1, gaps.first().fromPathIndex)
        assertEquals(2, gaps.first().toPathIndex)
        assertEquals(points[1], gaps.first().from)
        assertEquals(points[2], gaps.first().to)
    }

    @Test
    fun aStretchTheRoadsApiNeverAnsweredForIsRoutedWhateverItsLength() {
        // A failed snap batch falls back to the recorded fixes. In a city those are 80 m
        // apart — short enough to look like the straight runs a real road has, so there
        // is no measuring them apart, and they used to stay cutting across the blocks.
        val points = List(5) { GeoPoint(28.6139 + it * 0.000_72, 77.2090) }
        val route = SnappedRoute(
            path = points,
            pathIndexOfPoint = points.indices.toList(),
            rawSpans = listOf(1..3),   // the stretch whose request failed
        )

        val gaps = findRouteGaps(route, points)

        assertEquals(1, gaps.size)
        assertEquals(1, gaps.first().fromPathIndex)
        assertEquals(3, gaps.first().toPathIndex)
    }

    @Test
    fun fixesDriftingAroundAStandingVehicleAreNotRouted() {
        // Raw, but there is no road to follow between fixes a few metres apart.
        val points = List(4) { GeoPoint(28.6139 + it * 0.000_05, 77.2090) }   // ~5 m
        val route = SnappedRoute(points, points.indices.toList(), rawSpans = listOf(0..3))

        assertEquals(emptyList(), findRouteGaps(route, points))
    }

    @Test
    fun aRunOfSparseStretchesIsRoutedAsOneRequest() {
        // Three sparse stretches with short snapped hops between them — a highway run
        // reported every kilometre. Routed one by one these used to eat three of the
        // twelve requests a day was allowed, which is why a long day stayed straight
        // past its worst dozen stretches.
        val points = listOf(
            GeoPoint(28.6000, 77.2000),
            GeoPoint(28.6090, 77.2000),   // ~1 km gap
            GeoPoint(28.6093, 77.2000),   // ~33 m — drawn road, inside the run
            GeoPoint(28.6183, 77.2000),   // ~1 km gap
            GeoPoint(28.6186, 77.2000),   // ~33 m
            GeoPoint(28.6276, 77.2000),   // ~1 km gap
        )

        val gaps = findRouteGaps(SnappedRoute.straight(points), points)

        assertEquals(1, gaps.size, "the run should be one request, not three")
        assertEquals(0, gaps.first().fromPathIndex)
        assertEquals(5, gaps.first().toPathIndex)
    }

    @Test
    fun gapsFurtherApartThanTheBridgeStaySeparateRequests() {
        // Two sparse stretches with 2 km of correctly snapped road between them: merging
        // would throw that good geometry away and re-route it.
        val path = ArrayList<GeoPoint>()
        path += GeoPoint(28.6000, 77.2000)
        path += GeoPoint(28.6090, 77.2000)              // ~1 km gap
        repeat(40) { path += GeoPoint(28.6095 + it * 0.0005, 77.2000) }   // ~55 m hops
        path += GeoPoint(28.6400, 77.2000)              // another ~1 km gap

        val gaps = findRouteGaps(SnappedRoute.straight(path), path)

        assertEquals(2, gaps.size)
    }

    @Test
    fun aJumpTooLongToBeMissingRoadIsLeftAlone() {
        // Two unrelated trips in one day, 300 km apart: routing between them would
        // draw a journey that never happened.
        val points = listOf(GeoPoint(28.6, 77.2), GeoPoint(26.0, 75.8))

        assertEquals(emptyList(), findRouteGaps(SnappedRoute.straight(points), points))
    }

    @Test
    fun aDayOfSparseReportingStillFitsTheRequestBudget() {
        // A long highway day reported every ~800 m: every stretch is a gap, separated by
        // the short snapped hop around each junction. Routed one by one this is hundreds
        // of requests, so the old cap repaired the worst dozen and left the rest of the
        // day cutting across country. The bridge widens until the runs fit the budget.
        val path = ArrayList<GeoPoint>()
        repeat(120) {
            path += GeoPoint(28.0 + it * 0.0075, 77.0)        // ~830 m gap
            path += GeoPoint(28.0 + it * 0.0075 + 0.0020, 77.0)  // ~220 m hop
        }

        val gaps = findRouteGaps(SnappedRoute.straight(path), path)

        assertTrue(gaps.isNotEmpty())
        assertTrue(gaps.size <= MAX_GAP_FILLS, "would send ${gaps.size} requests")
        // And the whole day is covered, not just its worst stretches.
        assertEquals(0, gaps.first().fromPathIndex)
        assertEquals(path.lastIndex, gaps.last().toPathIndex)
    }

    @Test
    fun theFixesInsideAGapAreSentAsWaypoints() {
        val from = GeoPoint(28.600, 77.200)
        val to = GeoPoint(28.610, 77.200)
        // Fixes that bulge east: the vehicle went round, not straight up the map.
        val points = listOf(
            from,
            GeoPoint(28.603, 77.206),
            GeoPoint(28.606, 77.206),
            to,
            GeoPoint(28.620, 77.200),   // after the gap — not a waypoint inside it
        )

        val via = viaPointsInside(points, from, to)

        assertEquals(listOf(points[1], points[2]), via)
    }

    @Test
    fun aGapWithNothingRecordedInsideItSendsNoWaypoints() {
        val points = listOf(GeoPoint(28.600, 77.200), GeoPoint(28.610, 77.200))

        assertEquals(emptyList(), viaPointsInside(points, points[0], points[1]))
    }

    @Test
    fun waypointsAreThinnedToWhatDirectionsAccepts() {
        val many = List(100) { GeoPoint(28.0 + it * 0.001, 77.0) }

        val thinned = thinEvenly(many, MAX_GAP_VIA_POINTS)

        assertEquals(MAX_GAP_VIA_POINTS, thinned.size)
        // Evenly spaced, both ends kept, still in travel order.
        assertEquals(many.first(), thinned.first())
        assertEquals(many.last(), thinned.last())
        assertEquals(thinned, thinned.sortedBy { it.latitude })
    }

    @Test
    fun aFilledGapKeepsEveryFixOnTheLine() {
        val points = List(3) { GeoPoint(28.0 + it * 0.01, 77.0) }
        val route = SnappedRoute.straight(points)
        // The road between fix 1 and fix 2 bends east.
        val fill = listOf(GeoPoint(28.012, 77.004), GeoPoint(28.016, 77.004))

        val spliced = spliceGapFills(route, mapOf(1 to GapFill(2, fill)))

        assertEquals(
            listOf(points[0], points[1], fill[0], fill[1], points[2]),
            spliced.path,
        )
        // Fix 2 moved from vertex 2 to vertex 4 — without this the playback marker
        // would fall behind the line by the length of every gap it passed.
        assertEquals(listOf(0, 1, 4), spliced.pathIndexOfPoint)
    }

    @Test
    fun aMergedSpanReplacesTheVerticesInsideIt() {
        // Vertices 1 and 2 were snapped inside the run, but the fill now carries that
        // stretch: keeping them would draw the old straight chords alongside the fill.
        val route = SnappedRoute.straight(List(4) { GeoPoint(28.0 + it * 0.01, 77.0) })
        val fill = List(4) { GeoPoint(28.005 + it * 0.0075, 77.004) }

        val spliced = spliceGapFills(route, mapOf(0 to GapFill(3, fill)))

        assertEquals(listOf(route.path[0]) + fill + route.path[3], spliced.path)
        // Every fix still has a vertex, in travel order, and the replaced ones sit at
        // their matching fraction of the fill rather than all collapsing onto its start.
        assertEquals(listOf(0, 2, 3, 5), spliced.pathIndexOfPoint)
        assertEquals(spliced.pathIndexOfPoint, spliced.pathIndexOfPoint.sorted())
    }

    @Test
    fun everyFixStillPointsInsideThePathAfterSplicing() {
        val route = SnappedRoute.straight(List(6) { GeoPoint(28.0 + it * 0.01, 77.0) })
        val fills = mapOf(
            0 to GapFill(2, List(3) { GeoPoint(28.004 + it * 0.002, 77.003) }),
            3 to GapFill(5, List(2) { GeoPoint(28.034 + it * 0.004, 77.003) }),
        )

        val spliced = spliceGapFills(route, fills)

        spliced.pathIndexOfPoint.forEach { index ->
            assertTrue(index in spliced.path.indices, "vertex $index is off the path")
        }
    }

    @Test
    fun aRouteWithNoGapsIsNotRebuilt() {
        val route = SnappedRoute.straight(List(3) { GeoPoint(28.0 + it * 0.0001, 77.0) })

        assertEquals(route, spliceGapFills(route, emptyMap()))
    }

    @Test
    fun theRoadGeometryIsReadFromTheDirectionsSteps() {
        // Two steps whose geometry joins at one shared vertex.
        val body = """
            {
              "status": "OK",
              "routes": [
                {
                  "overview_polyline": {"points": "_p~iF~ps|U"},
                  "legs": [
                    {
                      "steps": [
                        {"polyline": {"points": "_p~iF~ps|U_ulLnnqC"}},
                        {"polyline": {"points": "_flwFn`faV_mqNvxq`@"}}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val path = parseDirectionsRoute(body)

        assertEquals(
            listOf(
                GeoPoint(38.5, -120.2),
                GeoPoint(40.7, -120.95),
                GeoPoint(43.252, -126.453),
            ),
            path,
        )
    }

    @Test
    fun aDirectionsRefusalIsNotReadAsGeometry() {
        assertNull(parseDirectionsRoute("""{"status": "ZERO_RESULTS", "routes": []}"""))
        assertNull(
            parseDirectionsRoute(
                """{"status": "REQUEST_DENIED", "error_message": "API not enabled"}"""
            )
        )
    }

    @Test
    fun anEncodedPolylineRoundTripsToItsCoordinates() {
        // The example from Google's polyline algorithm documentation.
        assertEquals(
            listOf(
                GeoPoint(38.5, -120.2),
                GeoPoint(40.7, -120.95),
                GeoPoint(43.252, -126.453),
            ),
            decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@"),
        )
    }

    @Test
    fun atruncatedPolylineIsNotAnException() {
        // A body cut off mid-flight yields what was readable, not a crash.
        assertTrue(decodePolyline("_p~iF~ps|U_ul").size == 1)
        assertEquals(emptyList(), decodePolyline("~"))
    }

    @Test
    fun coordinatesGoOutAsPlainDecimalsNotScientificNotation() {
        // Double.toString() gives "7.7E-5", which the Maps web services reject.
        assertEquals("0.000077", formatCoordinate(0.000077))
        assertEquals("28.613900", formatCoordinate(28.6139))
        assertEquals("-77.209000", formatCoordinate(-77.209))
        assertEquals("0.000000", formatCoordinate(0.0))
    }

    @Test
    fun aRoadsApiErrorIsNotReadAsPoints() {
        val body = """
            {
              "error": {
                "code": 403,
                "message": "Roads API has not been used in project ... before or it is disabled",
                "status": "PERMISSION_DENIED"
              }
            }
        """.trimIndent()

        assertNull(parseSnapResponse(body))
    }

    @Test
    fun aSnappedAnswerIsReadInPathOrderWithItsOriginalIndices() {
        // Shape of a real snapToRoads answer: interpolated vertices carry no
        // originalIndex, and the matched ones come back in ascending order.
        val body = """
            {
              "snappedPoints": [
                {"location": {"latitude": 28.6140812, "longitude": 77.2092019},
                 "originalIndex": 0, "placeId": "A"},
                {"location": {"latitude": 28.6141222, "longitude": 77.2095},
                 "placeId": "B"},
                {"location": {"latitude": 28.618, "longitude": 77.2135},
                 "originalIndex": 1, "placeId": "C"}
              ]
            }
        """.trimIndent()

        val snapped = parseSnapResponse(body)

        assertEquals(3, snapped?.size)
        assertEquals(listOf(0, null, 1), snapped?.map { it.originalIndex })
        assertEquals(28.6140812, snapped?.first()?.latitude)
    }
}