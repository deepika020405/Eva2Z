// composeApp/src/commonMain/kotlin/com/developer/ev2z/network/RoadSnapService.kt
package com.developer.ev2z.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

/** A plain lat/lng pair, so route geometry can live in commonMain. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * A recorded GPS trace redrawn along the roads it was actually driven on.
 *
 * [path] is the full road geometry — snapped samples plus the road vertices the Roads
 * API interpolates between them — and that is what the map draws, so the line bends
 * with the street instead of cutting straight from one sample to the next.
 *
 * [pathIndexOfPoint] maps each original route point to its position in [path]. The
 * playback marker needs it: without the mapping it would still hop sample-to-sample
 * across the corners the drawn line goes around, and visibly leave the line.
 */
data class SnappedRoute(
    val path: List<GeoPoint>,
    val pathIndexOfPoint: List<Int>,
    /**
     * Stretches of [path] that are recorded fixes rather than road geometry, because
     * the request covering them failed.
     *
     * Carried explicitly because their length says nothing about them: a city trace
     * reporting every 80 m falls back to 80 m hops, which look exactly like the short
     * straight runs a real road has — so [RouteGapFiller] cannot find them by measuring,
     * and used to leave them cutting across the blocks between the fixes.
     */
    val rawSpans: List<IntRange> = emptyList(),
) {
    companion object {
        val EMPTY = SnappedRoute(emptyList(), emptyList())

        /**
         * The trace exactly as recorded — straight hops between samples.
         *
         * Used while snapping is still in flight, and as the fallback whenever it
         * fails, so playback never waits on (or breaks because of) the Roads API.
         */
        fun straight(points: List<GeoPoint>): SnappedRoute =
            SnappedRoute(
                path = points,
                pathIndexOfPoint = points.indices.toList(),
                rawSpans = if (points.size >= 2) listOf(0..points.lastIndex) else emptyList(),
            )
    }
}

/** One point of a Roads API answer. */
internal data class RoadSnapPoint(
    val latitude: Double,
    val longitude: Double,
    /** Index into the requested path, or null for a vertex the API interpolated. */
    val originalIndex: Int?,
)

/**
 * Snaps a GPS trace onto the road network with Google's Roads API.
 *
 * The raw history points come in at whatever interval the tracker reports, so joining
 * them with straight lines draws a route that cuts across blocks and buildings.
 * `snapToRoads` with `interpolate=true` returns the road geometry between consecutive
 * samples instead, which is what makes the playback line follow the road.
 *
 * Costs one request per 100 points, so the trace is thinned before sending (see
 * [selectPointsToSnap]) — a parked vehicle otherwise burns requests reporting the same
 * spot hundreds of times.
 *
 * Snapping alone leaves two kinds of stretch straight, and both are repaired here
 * rather than left on the map:
 *
 *  - **Sparsely reported stretches.** The Roads API refuses to interpolate between
 *    fixes much more than ~300 m apart (`"Input path is too sparse"`), so a fast
 *    stretch or a reporting gap came out as a straight line while the rest of the same
 *    trip followed the roads. [RouteGapFiller] routes those stretches through the
 *    Directions API, which has no such limit.
 *  - **Requests that never landed.** Firing every batch at once got bursts
 *    rate-limited, and a failed batch falls back to raw fixes — a hundred points of
 *    straight line in the middle of an otherwise road-following route. Batches now go
 *    out a few at a time and a transient failure is retried.
 */
class RoadSnapService(
    private val client: HttpClient = KtorClient.client,
    private val apiKey: String = MapsConfig.WEB_API_KEY,
) {

    private val gapFiller = RouteGapFiller(client, apiKey)

    /**
     * Returns [points] redrawn along the roads, or [SnappedRoute.straight] if the
     * Roads API cannot be reached — this never throws, because a missing road overlay
     * must not take the playback screen down with it.
     *
     * [onPartial] receives the snapped route before the sparse stretches are routed, so
     * the map can show road-following geometry straight away instead of holding the
     * straight trace for the length of the gap-filling requests.
     */
    suspend fun snapToRoads(
        points: List<GeoPoint>,
        onPartial: (SnappedRoute) -> Unit = {},
    ): SnappedRoute {
        if (points.size < 2 || apiKey.isBlank()) return SnappedRoute.straight(points)

        val sent = selectPointsToSnap(points)
        if (sent.size < 2) return SnappedRoute.straight(points)

        val plan = planSnapRequests(sent, points)

        val responses = try {
            requestSnapBatches(plan.batches.map { batch -> batch.map { points[it] } })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("⚠️ [RoadSnap] snapToRoads failed: ${e.message}")
            // Even a total snapping failure is worth routing: the straight trace is
            // all gaps, and filling the longest of them still draws real roads.
            return gapFiller.fill(SnappedRoute.straight(points), points)
        }

        val snapped = assembleSnappedRoute(points, plan, responses)

        println(
            "🛣️ [RoadSnap] ${points.size} points → ${sent.size} sent in " +
                    "${plan.batches.size} request(s) across " +
                    "${plan.runStartBatches.size} dense run(s) → " +
                    "${snapped.path.size} road vertices"
        )

        onPartial(snapped)

        return gapFiller.fill(snapped, points)
    }

    /**
     * Sends the batches a few at a time.
     *
     * All of them used to go out together, which is what got whole batches
     * rate-limited on a long day — twenty concurrent requests — and every rejected
     * batch became a straight stretch in the drawn route.
     */
    private suspend fun requestSnapBatches(
        batches: List<List<GeoPoint>>,
    ): List<List<RoadSnapPoint>?> =
        batches.chunked(SNAP_CONCURRENCY).flatMap { window ->
            coroutineScope {
                window.map { batch -> async { requestSnap(batch) } }.awaitAll()
            }
        }

    /**
     * One `snapToRoads` call, retried through transient failures.
     *
     * A rate-limited or dropped request is worth asking again — the alternative is a
     * visibly straight stretch of route. A refusal (bad key, API not enabled) is not
     * retried: it would fail the same way for every batch.
     */
    private suspend fun requestSnap(points: List<GeoPoint>): List<RoadSnapPoint>? {
        // A run of one fix has nothing to snap against; it falls through as raw and the
        // gap filler routes the stretches either side of it.
        if (points.size < 2) return null

        repeat(SNAP_ATTEMPTS) { attempt ->
            val retryReason = try {
                val response = client.get(SNAP_TO_ROADS_URL) {
                    parameter(
                        "path",
                        points.joinToString("|") {
                            "${formatCoordinate(it.latitude)},${formatCoordinate(it.longitude)}"
                        },
                    )
                    parameter("interpolate", "true")
                    parameter("key", apiKey)
                }

                val status = response.status.value
                if (status == HTTP_TOO_MANY_REQUESTS || status >= HTTP_SERVER_ERROR) {
                    "HTTP $status"
                } else {
                    return parseSnapResponse(response.bodyAsText())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: e::class.simpleName ?: "request failed"
            }

            println("⚠️ [RoadSnap] retrying batch after $retryReason")
            if (attempt < SNAP_ATTEMPTS - 1) delay(SNAP_RETRY_DELAY_MS * (attempt + 1))
        }

        return null
    }

    private companion object {
        const val SNAP_TO_ROADS_URL = "https://roads.googleapis.com/v1/snapToRoads"
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVER_ERROR = 500
    }
}

/** Hard limit of the Roads API. */
internal const val MAX_POINTS_PER_SNAP_REQUEST = 100

/** Keeps one day's snapping inside ~20 requests. */
internal const val MAX_SNAP_POINTS = 2000

/** Below this a fix is noise around the same spot, not movement. */
internal const val MIN_SNAP_GAP_METERS = 10.0

/**
 * The widest spacing the thinning will aim for.
 *
 * The Roads API stops interpolating somewhere past 300 m between consecutive points, so
 * thinning beyond this would be the app creating the very gaps it then has to pay
 * Directions to fill back in.
 */
internal const val MAX_SNAP_GAP_METERS = 250.0

/** Batches in flight at once. */
internal const val SNAP_CONCURRENCY = 4

/** Attempts per batch, counting the first. */
internal const val SNAP_ATTEMPTS = 3

internal const val SNAP_RETRY_DELAY_MS = 400L

/**
 * Picks which recorded points are worth spending a request on, as indices into [points].
 *
 * Consecutive fixes closer together than [MIN_SNAP_GAP_METERS] are GPS noise around a
 * standing vehicle — they add nothing to the road geometry and would fill whole
 * batches. If the day is still too long after that, the spacing is widened — up to
 * [MAX_SNAP_GAP_METERS] — until the trace fits in [MAX_SNAP_POINTS].
 *
 * Widening the spacing replaced a plain stride over the kept points. A stride drops
 * every n-th fix wherever it falls, so a trace that already reported sparsely on the
 * highway got thinned there too and fell past the distance the Roads API will
 * interpolate across — straight lines in exactly the places that needed the road
 * geometry most. Thinning by distance instead spends the budget evenly along the route
 * and leaves the dense, slow, turn-heavy stretches intact.
 */
internal fun selectPointsToSnap(points: List<GeoPoint>): List<Int> {
    var gap = MIN_SNAP_GAP_METERS
    var kept = thinByDistance(points, gap)

    while (kept.size > MAX_SNAP_POINTS && gap < MAX_SNAP_GAP_METERS) {
        gap = minOf(gap * 2, MAX_SNAP_GAP_METERS)
        kept = thinByDistance(points, gap)
    }

    if (kept.size <= MAX_SNAP_POINTS) return kept

    // A trace still too long at the widest useful spacing is a very long day. Striding
    // it down is the last resort; the gaps this opens get routed by [RouteGapFiller].
    val stride = (kept.size + MAX_SNAP_POINTS - 1) / MAX_SNAP_POINTS
    return kept.filterIndexed { index, _ -> index % stride == 0 || index == kept.lastIndex }
}

/** The indices of [points] left once no two consecutive ones are closer than [gapMeters]. */
internal fun thinByDistance(points: List<GeoPoint>, gapMeters: Double): List<Int> {
    val kept = ArrayList<Int>(points.size)
    var previous: GeoPoint? = null

    points.forEachIndexed { index, point ->
        val moved = previous?.let { approxDistanceMeters(it, point) >= gapMeters } ?: true
        // The last fix is always kept so the drawn route reaches the end of the trip.
        if (moved || index == points.lastIndex) {
            kept += index
            previous = point
        }
    }

    return kept
}

/**
 * The widest spacing worth snapping at all.
 *
 * Measured against the live APIs (2026-10-05) by sampling a known road route and
 * comparing the drawn line back against it, segment by segment:
 *
 * | fix spacing | worst distance from the real road, after snapping |
 * |-------------|--------------------------------------------------|
 * | 110 m       | 1 m                                              |
 * | 250 m       | 1 m                                              |
 * | 450 m       | 174 m                                            |
 * | 600 m       | 201 m, and 3.9 km of the line off-road           |
 *
 * Past this spacing the Roads API does not merely stop interpolating: with that little
 * context it snaps a fix onto a *different* road and draws interpolated geometry along
 * it, so the line runs confidently down the wrong street. Routing the stretch through
 * Directions with the recorded fixes as waypoints instead measured 1 m and no off-road
 * segments at all, which is why sparse stretches are never sent to snapping now.
 */
internal const val SNAPPABLE_MAX_GAP_METERS = 350.0

/**
 * The Roads API requests for one trace, with the sparse stretches held back.
 *
 * [batches] are in travel order across every run; [runStartBatches] holds the index of
 * each batch that begins a new run, meaning the stretch before it was too sparse to
 * snap and has to be routed instead.
 */
internal data class SnapPlan(
    val batches: List<List<Int>>,
    val runStartBatches: Set<Int>,
)

/**
 * Groups the sent points into runs dense enough to snap, then batches each run.
 *
 * A run ends wherever the next fix is further than [SNAPPABLE_MAX_GAP_METERS] away: that
 * stretch goes to [RouteGapFiller] rather than into a snap request, and the fixes on
 * either side of it are snapped as part of their own runs, where there is enough context
 * to land them on the right road.
 */
internal fun planSnapRequests(sent: List<Int>, points: List<GeoPoint>): SnapPlan {
    val batches = ArrayList<List<Int>>()
    val runStarts = HashSet<Int>()

    var run = ArrayList<Int>()

    fun flushRun() {
        if (run.isEmpty()) return
        runStarts += batches.size
        // A lone fix has nothing to snap against; it goes in as itself.
        batches += if (run.size < 2) listOf(run) else buildSnapBatches(run)
        run = ArrayList()
    }

    sent.forEachIndexed { position, index ->
        if (position > 0) {
            val previous = sent[position - 1]
            val gap = approxDistanceMeters(points[previous], points[index])
            if (gap > SNAPPABLE_MAX_GAP_METERS) flushRun()
        }
        run += index
    }
    flushRun()

    return SnapPlan(batches, runStarts)
}

/**
 * Splits one run into Roads API requests of at most [MAX_POINTS_PER_SNAP_REQUEST],
 * repeating each batch's last point as the next batch's first so the two stretches of
 * road geometry meet instead of leaving a gap at the seam.
 */
internal fun buildSnapBatches(sent: List<Int>): List<List<Int>> {
    val batches = ArrayList<List<Int>>()
    var start = 0
    while (start < sent.size - 1) {
        val end = minOf(start + MAX_POINTS_PER_SNAP_REQUEST, sent.size)
        batches += sent.subList(start, end)
        start = end - 1   // overlap by one point
    }
    return batches
}

/**
 * Stitches the per-batch answers into one road path, plus the sample → path-vertex
 * mapping the playback marker walks along.
 *
 * A batch that came back null failed on its own; its recorded points are used for that
 * stretch rather than dropping it out of the drawn line altogether.
 */
internal fun assembleSnappedRoute(
    points: List<GeoPoint>,
    batches: List<List<Int>>,
    responses: List<List<RoadSnapPoint>?>,
): SnappedRoute = assembleSnappedRoute(points, SnapPlan(batches, setOf(0)), responses)

internal fun assembleSnappedRoute(
    points: List<GeoPoint>,
    plan: SnapPlan,
    responses: List<List<RoadSnapPoint>?>,
): SnappedRoute {
    val batches = plan.batches
    val path = ArrayList<GeoPoint>(points.size * 2)
    // Original point index → its index in `path`. Only sent points land here; the ones
    // thinned out are filled in afterwards.
    val pathIndexOfSentPoint = HashMap<Int, Int>(batches.sumOf { it.size })
    // Stretches that came out as recorded fixes because their request failed.
    val rawSpans = ArrayList<IntRange>()

    batches.forEachIndexed { batchNumber, batch ->
        val snapped = responses.getOrNull(batchNumber)
        val startsRun = batchNumber in plan.runStartBatches

        if (snapped.isNullOrEmpty()) {
            // Starts at the last vertex already drawn, so the span covers the hop into
            // this stretch as well as the stretch itself.
            val spanStart = (path.size - 1).coerceAtLeast(0)
            batch.forEachIndexed { indexInBatch, originalIndex ->
                // The seam point already went in with the previous batch — unless this
                // batch opens a new run, where there is no shared point.
                if (batchNumber > 0 && indexInBatch == 0 && !startsRun) return@forEachIndexed
                path += points[originalIndex]
                pathIndexOfSentPoint[originalIndex] = path.lastIndex
            }
            if (path.lastIndex > spanStart) rawSpans += spanStart..path.lastIndex
            return@forEachIndexed
        }

        // The stretch between two runs was too sparse to snap, so nothing was drawn
        // along it: the hop from the previous run's last vertex into this one carries no
        // road geometry and is handed to the gap filler.
        val runJoinStart = if (startsRun && path.isNotEmpty()) path.lastIndex else -1

        snapped.forEach { snappedPoint ->
            // Batches overlap by one point so the road geometry joins up across the
            // seam. That shared point is already in `path` from the previous batch, so
            // it is mapped but not appended again. A batch that opens a run shares
            // nothing with what came before it.
            if (batchNumber > 0 && !startsRun && snappedPoint.originalIndex == 0) {
                val seam = batch.firstOrNull()
                if (seam != null && path.isNotEmpty()) {
                    pathIndexOfSentPoint[seam] = path.lastIndex
                }
                return@forEach
            }

            path += GeoPoint(snappedPoint.latitude, snappedPoint.longitude)

            // Interpolated vertices carry no originalIndex — they are pure road
            // geometry with no sample behind them.
            val originalIndex = snappedPoint.originalIndex
                ?.let { batch.getOrNull(it) }
                ?: return@forEach
            pathIndexOfSentPoint[originalIndex] = path.lastIndex
        }

        if (runJoinStart >= 0 && path.lastIndex > runJoinStart) {
            rawSpans += runJoinStart..(runJoinStart + 1)
        }
    }

    if (path.size < 2) return SnappedRoute.straight(points)

    return SnappedRoute(
        path = path,
        pathIndexOfPoint = fillPathIndices(points.size, pathIndexOfSentPoint),
        rawSpans = rawSpans,
    )
}

/**
 * Every original point needs a place on the drawn line, including the ones that were
 * thinned out before sending and the ones the Roads API could not match to a road.
 * Both carry forward the last known position: the marker then holds still over a
 * stationary stretch instead of jumping back to the start of the path.
 */
private fun fillPathIndices(
    pointCount: Int,
    pathIndexOfSentPoint: Map<Int, Int>,
): List<Int> {
    var last = 0
    return List(pointCount) { index ->
        last = pathIndexOfSentPoint[index] ?: last
        last
    }
}

/** Reads one `snapToRoads` answer, or null if it carried an error instead of points. */
internal fun parseSnapResponse(body: String): List<RoadSnapPoint>? {
    val root = snapJson.parseToJsonElement(body).jsonObject

    root["error"]?.let { error ->
        println("⚠️ [RoadSnap] Roads API error: $error")
        return null
    }

    return root["snappedPoints"]
        ?.jsonArray
        ?.mapNotNull { element ->
            val point = element.jsonObject
            val location = point["location"]?.jsonObject ?: return@mapNotNull null
            val lat = location["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val lng = location["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            RoadSnapPoint(
                latitude = lat,
                longitude = lng,
                originalIndex = point["originalIndex"]?.jsonPrimitive?.intOrNull,
            )
        }
        ?.takeIf { it.isNotEmpty() }
}

private val snapJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Flat-earth distance, good to a fraction of a percent over the few hundred metres
 * between two GPS fixes — this only has to decide "same spot or moved", and weight
 * interpolation along a segment.
 */
internal fun approxDistanceMeters(from: GeoPoint, to: GeoPoint): Double {
    val metresPerDegree = 111_320.0
    val dLat = (to.latitude - from.latitude) * metresPerDegree
    val dLng = (to.longitude - from.longitude) * metresPerDegree *
            cos(from.latitude * PI / 180.0)
    return sqrt(dLat * dLat + dLng * dLng)
}