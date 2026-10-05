// composeApp/src/commonMain/kotlin/com/developer/ev2z/network/RouteGapFiller.kt
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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Fills in the stretches `snapToRoads` leaves as straight lines.
 *
 * The Roads API only draws road geometry between fixes that are close together. Past
 * roughly 300–500 m apart it answers `"Input path is too sparse"` and returns the two
 * endpoints snapped but nothing in between, so the drawn route cuts straight across
 * everything between them. Measured against the live API (2026-10-03):
 *
 * | spacing | vertices returned for a 2-point path |
 * |---------|--------------------------------------|
 * | 110 m   | 5                                    |
 * | 330 m   | 24                                   |
 * | 550 m   | 26 + "too sparse" warning            |
 * | 1100 m  | 2 — no road geometry at all          |
 *
 * That is why playback followed the roads in some places and not others: wherever the
 * tracker reported sparsely — a fast stretch, a weak-signal area, a gap in reporting —
 * snapping silently gave up on that stretch alone.
 *
 * The Directions API has no such limit: asked for a driving route between the two ends
 * of a gap it returns the full road path. Any recorded fixes that fall inside the gap
 * are sent along as `via:` waypoints so the fill follows the roads the vehicle actually
 * took rather than whatever route Directions would prefer on its own.
 */
internal class RouteGapFiller(
    private val client: HttpClient,
    private val apiKey: String,
) {

    /**
     * Returns [route] with its missing road geometry drawn in, or [route] unchanged if
     * there is nothing to fill or Directions cannot be reached.
     */
    suspend fun fill(route: SnappedRoute, points: List<GeoPoint>): SnappedRoute {
        if (apiKey.isBlank()) return route

        val gaps = findRouteGaps(route, points)
        if (gaps.isEmpty()) return route

        val fills = try {
            requestFills(gaps)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("⚠️ [RoadSnap] gap fill failed: ${e.message}")
            return route
        }

        println(
            "🛣️ [RoadSnap] ${gaps.size} straight gap(s) " +
                    "(longest ${gaps.maxOf { it.meters }.toInt()} m) → " +
                    "${fills.size} filled from Directions"
        )

        val spliced = spliceGapFills(route, fills)
        reportRemainingStraightLine(spliced)
        return spliced
    }

    /**
     * Logs whatever is still drawn as a straight line.
     *
     * A route that looks wrong on the map can only be chased with the numbers from the
     * day that produced it, so the leftovers are named rather than left to be guessed
     * at. Chords of a few hundred metres are usually real road — a Directions route of
     * its own contains plenty — but a kilometre of straight line is a stretch nothing
     * could be routed for.
     */
    private fun reportRemainingStraightLine(route: SnappedRoute) {
        val chords = (0 until route.path.size - 1)
            .map { index -> index to approxDistanceMeters(route.path[index], route.path[index + 1]) }
            .filter { (_, meters) -> meters >= MAX_ROAD_GAP_METERS }

        if (chords.isEmpty()) {
            println("🛣️ [RoadSnap] no straight line left in the drawn route")
            return
        }

        val worst = chords.maxBy { it.second }
        val from = route.path[worst.first]
        println(
            "🛣️ [RoadSnap] still straight: ${chords.size} chord(s), " +
                    "${chords.sumOf { it.second }.toInt()} m total, " +
                    "longest ${worst.second.toInt()} m at " +
                    "${formatCoordinate(from.latitude)},${formatCoordinate(from.longitude)}"
        )
    }

    /**
     * Routes every gap, a few at a time. The gaps are independent, but firing a dozen
     * requests at once is what gets a burst rate-limited — and a rate-limited fill is a
     * stretch of route that stays straight.
     */
    private suspend fun requestFills(gaps: List<RouteGap>): Map<Int, GapFill> {
        val fills = LinkedHashMap<Int, GapFill>(gaps.size)

        gaps.chunked(GAP_FILL_CONCURRENCY).forEach { window ->
            coroutineScope {
                window.map { gap -> async { gap to requestRoadRoute(gap) } }.awaitAll()
            }.forEach { (gap, interior) ->
                if (!interior.isNullOrEmpty()) {
                    fills[gap.fromPathIndex] = GapFill(gap.toPathIndex, interior)
                }
            }
        }

        return fills
    }

    /**
     * The road geometry between the two ends of [gap], without the ends themselves —
     * those are already vertices of the path being repaired.
     *
     * Null when Directions has no driving route to offer (`ZERO_RESULTS` across a
     * ferry, a private road, a bad fix in the middle of a field), in which case the
     * stretch keeps the straight line it has.
     */
    private suspend fun requestRoadRoute(gap: RouteGap): List<GeoPoint>? {
        // With the waypoints first — they hold the fill on the roads actually driven.
        // Then without them: one bad fix inside the span (a fix in the middle of a
        // field, a point the vehicle reported from a yard) makes Directions answer
        // ZERO_RESULTS for the whole span, and road geometry down the wrong-but-real
        // road still beats a line across the buildings.
        return routeBetween(gap.from, gap.to, gap.via)
            ?: gap.via.takeIf { it.isNotEmpty() }?.let { routeBetween(gap.from, gap.to) }
    }

    /**
     * One Directions call, retried through transient failures.
     *
     * Now that a fill covers a whole run of sparse reporting rather than one chord, a
     * dropped request leaves that entire run straight on the map, so it is worth asking
     * again. A refusal — ZERO_RESULTS, a key problem — is answered the same way every
     * time and is not retried.
     */
    private suspend fun routeBetween(
        from: GeoPoint,
        to: GeoPoint,
        via: List<GeoPoint> = emptyList(),
    ): List<GeoPoint>? {
        repeat(GAP_FILL_ATTEMPTS) { attempt ->
            val retryReason = try {
                val response = client.get(DIRECTIONS_URL) {
                    parameter("origin", from.toParameter())
                    parameter("destination", to.toParameter())
                    if (via.isNotEmpty()) {
                        parameter("waypoints", via.joinToString("|") { "via:${it.toParameter()}" })
                    }
                    parameter("mode", "driving")
                    parameter("key", apiKey)
                }

                val status = response.status.value
                if (status == HTTP_TOO_MANY_REQUESTS || status >= HTTP_SERVER_ERROR) {
                    "HTTP $status"
                } else {
                    return parseDirectionsRoute(response.bodyAsText())
                        ?.let { dropEnds(it) }
                        ?.takeIf { it.isNotEmpty() }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: e::class.simpleName ?: "request failed"
            }

            println("⚠️ [RoadSnap] retrying gap fill after $retryReason")
            if (attempt < GAP_FILL_ATTEMPTS - 1) {
                delay(GAP_FILL_RETRY_DELAY_MS * (attempt + 1))
            }
        }

        return null
    }

    private companion object {
        const val DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json"
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVER_ERROR = 500
    }
}

/**
 * Two consecutive vertices of a snapped path further apart than this were not joined by
 * road geometry — interpolated road vertices land tens of metres apart at most, so a
 * jump this long is a straight line across whatever lies between.
 */
internal const val MAX_ROAD_GAP_METERS = 150.0

/**
 * Past this a gap is not a stretch of missing road but a jump: the tracker was off for
 * an hour, or the day holds two unrelated trips. Routing across one would invent a
 * journey, so it keeps its straight line.
 */
internal const val MAX_GAP_FILL_METERS = 40_000.0

/** A day's playback is worth at most this many Directions requests. */
internal const val MAX_GAP_FILLS = 24

/**
 * Raw stretches shorter than this keep their straight line.
 *
 * A stretch with no road geometry is worth routing however short it is — 80 m of city
 * reporting cuts the corner of a block just as visibly as a kilometre does — but below
 * this it is a standing vehicle's fixes drifting, where there is no road to follow.
 */
internal const val MIN_RAW_GAP_METERS = 25.0

/** Attempts per fill, counting the first. A rate-limited fill is a straight stretch. */
internal const val GAP_FILL_ATTEMPTS = 3

internal const val GAP_FILL_RETRY_DELAY_MS = 400L

/**
 * Directions accepts 25 waypoints including the two ends, so a fill can be pinned by
 * this many recorded fixes. Measured against the live API (2026-10-05): a 6.3 km city
 * drive reported at 600 m spacing came back from Directions as the full road geometry
 * in a single request this way, while `snapToRoads` on the same fixes left six chords
 * of 400 m–1 km cutting across the blocks between them.
 */
internal const val MAX_GAP_VIA_POINTS = 23

/**
 * Two gaps separated by less than this much drawn road are repaired in one request.
 *
 * Sparse reporting comes in runs — a highway stretch, a weak-signal area — so a day of
 * it used to produce dozens of individual gaps, of which only [MAX_GAP_FILLS] were ever
 * routed and the rest stayed straight on the map. One request spanning a whole run
 * covers the same distance as a dozen separate ones, which is what makes the cap
 * enough for a real day's driving rather than just its worst few stretches.
 *
 * Kept short so a long, correctly snapped stretch between two distant gaps is never
 * thrown away and re-routed: inside a run the snapped geometry it replaces is a few
 * hundred metres at most.
 */
internal const val MERGE_BRIDGE_METERS = 400.0

/**
 * The widest the bridge is allowed to grow while fitting a day into [MAX_GAP_FILLS].
 *
 * Past this, merging would hand Directions a span pinned by via points kilometres apart
 * and let it choose its own way between them — road geometry, but not necessarily the
 * roads driven. A day still too fragmented at this width keeps straight lines on its
 * shorter stretches rather than inventing routes across its longer ones.
 */
internal const val MAX_MERGE_BRIDGE_METERS = 10_000.0

/** Gaps routed at once — enough to stay quick, few enough not to trip the rate limit. */
internal const val GAP_FILL_CONCURRENCY = 4

/**
 * One stretch of a snapped path that carries no road geometry.
 *
 * A gap spans from [fromPathIndex] to [toPathIndex]. Usually those are consecutive
 * vertices, but a run of sparsely reported stretches is merged into one span (see
 * [MERGE_BRIDGE_METERS]) and then the few snapped vertices in between are replaced by
 * the fill rather than kept.
 */
internal data class RouteGap(
    val fromPathIndex: Int,
    val toPathIndex: Int,
    val from: GeoPoint,
    val to: GeoPoint,
    /** Recorded fixes inside the gap, to hold the fill on the roads actually driven. */
    val via: List<GeoPoint>,
    /** Straight-line distance carried by this span, used to rank it against the others. */
    val meters: Double,
)

/**
 * Finds the stretches of [route] that need road geometry drawn in, worst first but
 * returned in travel order so they can be spliced back in one pass.
 *
 * Neighbouring gaps are merged first, so one request repairs a whole sparsely reported
 * run. Only the [MAX_GAP_FILLS] longest of what remains are kept: those are the ones
 * that read as wrong on the map, and the cap keeps a day's playback to a bounded number
 * of requests.
 */
internal fun findRouteGaps(route: SnappedRoute, points: List<GeoPoint>): List<RouteGap> {
    if (route.path.size < 2) return emptyList()

    // Segments with no road geometry behind them, by either of the two ways that
    // happens: a chord long enough that nothing was interpolated into it, or a stretch
    // the Roads API never answered for, whatever its length.
    val raw = route.rawSpans.flatMapTo(HashSet()) { span ->
        span.first until span.last
    }

    val straight = ArrayList<Int>()   // index of each vertex a straight chord starts at

    for (index in 0 until route.path.size - 1) {
        val meters = approxDistanceMeters(route.path[index], route.path[index + 1])
        if (meters > MAX_GAP_FILL_METERS) continue
        if (meters < MAX_ROAD_GAP_METERS && index !in raw) continue
        // A standing vehicle's fixes are not worth a request even when they are raw.
        if (meters < MIN_RAW_GAP_METERS) continue
        straight += index
    }

    if (straight.isEmpty()) return emptyList()

    // Merge neighbouring gaps, widening the bridge until a day's worth of them fits the
    // request budget. A long sparse day used to produce hundreds of individual gaps, of
    // which only the longest [MAX_GAP_FILLS] were routed — measured on a 190 km trace
    // reported at 700 m (2026-10-05), that repaired 12 km of 170 km and left the rest
    // cutting across country. Widening to one request per run covered all of it in 8.
    var bridge = MERGE_BRIDGE_METERS
    var spans = mergeGapRuns(route.path, straight, bridge, points)

    while (spans.size > MAX_GAP_FILLS && bridge < MAX_MERGE_BRIDGE_METERS) {
        bridge = minOf(bridge * 2, MAX_MERGE_BRIDGE_METERS)
        spans = mergeGapRuns(route.path, straight, bridge, points)
    }

    return spans
        .map { span ->
            val from = route.path[span.first]
            val to = route.path[span.last]
            RouteGap(
                fromPathIndex = span.first,
                toPathIndex = span.last,
                from = from,
                to = to,
                via = viaPointsInside(points, from, to),
                meters = pathLengthMeters(route.path, span.first, span.last),
            )
        }
        .sortedByDescending { it.meters }
        .take(MAX_GAP_FILLS)
        .sortedBy { it.fromPathIndex }
}

/**
 * Groups the straight chords into spans, joining any two separated by less than
 * [bridgeMeters] of drawn road.
 *
 * Returned as `first..last` vertex spans; a lone chord stays a span of one segment.
 */
internal fun mergeGapRuns(
    path: List<GeoPoint>,
    chordStarts: List<Int>,
    bridgeMeters: Double = MERGE_BRIDGE_METERS,
    points: List<GeoPoint> = emptyList(),
): List<IntRange> {
    val spans = ArrayList<IntRange>()
    var spanStart = chordStarts.first()
    var spanEnd = spanStart + 1

    for (start in chordStarts.drop(1)) {
        val bridge = pathLengthMeters(path, spanEnd, start)
        val merged = bridge <= bridgeMeters &&
                pathLengthMeters(path, spanStart, start + 1) <= MAX_GAP_FILL_METERS &&
                // One request carries at most MAX_GAP_VIA_POINTS waypoints, so a span is
                // only allowed to grow while the fixes inside it still fit. Merging past
                // that would hand Directions a span pinned kilometres apart and let it
                // pick its own roads between the pins.
                countFixesInside(points, path[spanStart], path[start + 1]) <=
                MAX_GAP_VIA_POINTS

        if (merged) {
            spanEnd = start + 1
        } else {
            spans += spanStart..spanEnd
            spanStart = start
            spanEnd = start + 1
        }
    }

    spans += spanStart..spanEnd
    return spans
}

/**
 * How many recorded fixes lie between two path vertices.
 *
 * Counted on the fixes rather than the drawn path because that is what a fill can be
 * pinned to: 23 of them is one request's worth of `via:` waypoints.
 */
internal fun countFixesInside(points: List<GeoPoint>, from: GeoPoint, to: GeoPoint): Int {
    if (points.size < 3) return 0

    val start = points.indices.minBy { approxDistanceMeters(points[it], from) }
    val end = (start until points.size).minBy { approxDistanceMeters(points[it], to) }
    return (end - start - 1).coerceAtLeast(0)
}

/** Distance walked along [path] from vertex [from] to vertex [to]. */
internal fun pathLengthMeters(path: List<GeoPoint>, from: Int, to: Int): Double {
    var total = 0.0
    for (index in from until to) {
        total += approxDistanceMeters(path[index], path[index + 1])
    }
    return total
}

/**
 * The recorded fixes that fall between the two ends of a gap, thinned to
 * [MAX_GAP_VIA_POINTS].
 *
 * A gap exists because those fixes were either thinned out before snapping or too far
 * apart for the Roads API to join — but they still say which way the vehicle went, and
 * without them Directions is free to fill a 2 km gap by whichever route it likes best.
 *
 * The ends are located by proximity rather than through the point → path mapping,
 * because a gap often starts on a vertex the Roads API interpolated, which has no
 * recorded fix behind it at all.
 */
internal fun viaPointsInside(
    points: List<GeoPoint>,
    from: GeoPoint,
    to: GeoPoint,
): List<GeoPoint> {
    if (points.size < 3) return emptyList()

    val start = points.indices.minBy { approxDistanceMeters(points[it], from) }
    // Searched forward from `start`: a route that passes the same place twice would
    // otherwise match the end of the gap to the wrong lap.
    val end = (start until points.size).minBy { approxDistanceMeters(points[it], to) }

    if (end <= start + 1) return emptyList()

    return thinEvenly(points.subList(start + 1, end), MAX_GAP_VIA_POINTS)
}

/** At most [limit] points of [points], evenly spaced, ends included. */
internal fun thinEvenly(points: List<GeoPoint>, limit: Int): List<GeoPoint> {
    if (points.size <= limit) return points

    // limit - 1 steps across the list, so both ends are kept.
    return List(limit) { slot ->
        points[(slot.toLong() * (points.size - 1) / (limit - 1)).toInt()]
    }
}

/** Road geometry for one span of a path, without the span's own end vertices. */
internal data class GapFill(
    /** The vertex the span ends at; everything strictly between is replaced. */
    val toPathIndex: Int,
    val interior: List<GeoPoint>,
)

/**
 * Rebuilds [route] with each fill spliced over the span it belongs to, carrying the
 * fix → vertex mapping across to the new vertex numbering.
 *
 * Without that remapping the playback marker would keep walking the old indices and
 * drift further behind the line with every gap it passed. A fix whose vertex was inside
 * a replaced span is moved to the matching fraction of the fill, so the marker still
 * advances along the new geometry at roughly the right place.
 */
internal fun spliceGapFills(
    route: SnappedRoute,
    fills: Map<Int, GapFill>,
): SnappedRoute {
    if (fills.isEmpty()) return route

    val path = ArrayList<GeoPoint>(route.path.size + fills.values.sumOf { it.interior.size })
    val movedTo = IntArray(route.path.size) { -1 }

    var index = 0
    while (index < route.path.size) {
        path += route.path[index]
        movedTo[index] = path.lastIndex

        val fill = fills[index]
        if (fill == null || fill.toPathIndex <= index + 1) {
            // No span to replace — a plain insert after this vertex, if there is one.
            if (fill != null) path += fill.interior
            index++
            continue
        }

        val firstFillVertex = path.size
        path += fill.interior

        // Vertices strictly inside the span are gone; point them at the fill instead.
        val spanSteps = fill.toPathIndex - index
        for (replaced in (index + 1) until fill.toPathIndex) {
            val fraction = (replaced - index).toDouble() / spanSteps
            // Rounded, not truncated: at a third and two thirds of a four-vertex fill,
            // truncation puts both on the same vertex and the marker stalls there.
            val offset = ((fill.interior.size - 1) * fraction)
                .roundToInt()
                .coerceIn(0, fill.interior.lastIndex)
            movedTo[replaced] = firstFillVertex + offset
        }

        index = fill.toPathIndex
    }

    return SnappedRoute(
        path = path,
        pathIndexOfPoint = route.pathIndexOfPoint.map { old ->
            movedTo.getOrElse(old) { -1 }.coerceAtLeast(0)
        },
        // Whatever was raw and got filled is road geometry now; whatever was raw and is
        // still raw was already offered a request. Either way the marks have been spent.
        rawSpans = emptyList(),
    )
}

/**
 * Reads the road geometry out of a Directions answer, or null if it carried no route.
 *
 * The step polylines are used rather than `overview_polyline`: the overview is
 * simplified for drawing at low zoom and loses the bends the whole point here is to
 * draw. `via:` waypoints keep the answer to a single leg, but legs are walked anyway so
 * a stopover-shaped answer would still read correctly.
 */
internal fun parseDirectionsRoute(body: String): List<GeoPoint>? {
    val root = directionsJson.parseToJsonElement(body).jsonObject

    val status = root["status"]?.jsonPrimitive?.contentOrNull
    if (status != "OK") {
        println(
            "⚠️ [RoadSnap] Directions status $status " +
                    (root["error_message"]?.jsonPrimitive?.contentOrNull ?: "")
        )
        return null
    }

    val legs = root["routes"]?.jsonArray?.firstOrNull()
        ?.jsonObject?.get("legs")?.jsonArray
        ?: return null

    val path = ArrayList<GeoPoint>()

    legs.forEach { leg ->
        leg.jsonObject["steps"]?.jsonArray?.forEach { step ->
            val encoded = step.jsonObject["polyline"]
                ?.jsonObject?.get("points")
                ?.jsonPrimitive?.contentOrNull
                ?: return@forEach

            decodePolyline(encoded).forEach { point ->
                // Consecutive steps share their joining vertex.
                if (path.lastOrNull() != point) path += point
            }
        }
    }

    // A route with no step geometry falls back to the straight-line answer, which is
    // what the gap already draws — report nothing rather than splice in a duplicate.
    return path.takeIf { it.size >= 2 }
        ?: root["routes"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("overview_polyline")
            ?.jsonObject?.get("points")
            ?.jsonPrimitive?.contentOrNull
            ?.let { decodePolyline(it) }
            ?.takeIf { it.size >= 2 }
}

/**
 * Drops the first and last vertex of a fill: Directions snaps them onto the gap's own
 * endpoints, which are already on the path.
 */
private fun dropEnds(path: List<GeoPoint>): List<GeoPoint> =
    if (path.size <= 2) emptyList() else path.subList(1, path.size - 1)

/** Six decimals is ~10 cm — more precision than a GPS fix carries. */
private fun GeoPoint.toParameter(): String =
    "${formatCoordinate(latitude)},${formatCoordinate(longitude)}"

/**
 * Formats a coordinate for a query string without scientific notation, which
 * `toString()` produces for small values and the Maps web services reject.
 */
internal fun formatCoordinate(value: Double): String {
    val scaled = (value * 1_000_000).toLong()
    val sign = if (scaled < 0) "-" else ""
    val magnitude = if (scaled < 0) -scaled else scaled
    val fraction = (magnitude % 1_000_000).toString().padStart(6, '0')
    return "$sign${magnitude / 1_000_000}.$fraction"
}

private val directionsJson = Json { ignoreUnknownKeys = true; isLenient = true }