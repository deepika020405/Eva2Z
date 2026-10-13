package com.developer.ev2z

import com.developer.ev2z.ui.components.MARKER_OUTLINE_FRACTION
import com.developer.ev2z.ui.components.MarkerPart
import com.developer.ev2z.ui.components.PartPaint
import com.developer.ev2z.ui.components.VehicleTopView
import com.developer.ev2z.ui.components.markerRotationDegrees
import com.developer.ev2z.data.local.model.vehicleSlugForId
import com.developer.ev2z.ui.components.vehicleTopViewFor
import com.developer.ev2z.ui.components.vehicleTopViewOf
import com.developer.ev2z.ui.components.vehicleTopViewParts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Which way a vehicle marker is turned, and what it is made of.
 *
 * Three bugs are buried in the history of this, all of them from the artwork being a
 * **side-profile** emoji that faced west. Marker rotation = compass heading had the
 * vehicle crabbing sideways down every road; offsetting for where its nose pointed sent
 * it upside down on every eastward heading; mirroring kept it upright but still stood
 * it on its back wheels at north and south, where a glyph that much wider than it is
 * tall reads as a sliver — the vehicle was on the map and could not be made out.
 *
 * Drawing the vehicle **from above** is what removed all three at once: there is no
 * up or down to get wrong, so the marker is turned by the heading and nothing else.
 * These tests hold that rule down, and keep the shared artwork inside the box both
 * platforms rasterise it into.
 */
class VehicleTopViewTest {

    // ── The rotation rule ────────────────────────────────────────────────────

    /** The whole rule: the heading, as it is. */
    @Test
    fun `a marker is turned to the heading itself`() {
        for (heading in 0..359) {
            assertEquals(
                heading.toDouble(),
                markerRotationDegrees(heading.toDouble()),
                "heading $heading"
            )
        }
    }

    /**
     * A marker that has not travelled yet has a NaN heading, and neither platform will
     * take it: a Google Maps `Marker` throws on it and a `CGAffineTransform` draws
     * nothing. It faces up instead.
     */
    @Test
    fun `an unknown heading draws facing up`() {
        assertEquals(0.0, markerRotationDegrees(Double.NaN))
    }

    /** Whatever the bearing arithmetic produced, the result is a usable angle. */
    @Test
    fun `the result stays inside one turn`() {
        for (heading in listOf(0.0, 45.0, 180.0, 359.9, -30.0, 540.0, -400.0)) {
            val rotation = markerRotationDegrees(heading)
            assertTrue(rotation in 0.0..<360.0, "heading $heading gave $rotation")
        }
        assertEquals(330.0, markerRotationDegrees(-30.0), "a negative bearing wraps")
        assertEquals(180.0, markerRotationDegrees(540.0), "more than one turn wraps")
    }

    // ── The catalogue ────────────────────────────────────────────────────────

    /** Null only when the server named no vehicle — the caller draws the arrow then. */
    @Test
    fun `no vehicle named draws no vehicle`() {
        assertNull(vehicleTopViewFor(null))
        assertNull(vehicleTopViewFor(""))
        assertNull(vehicleTopViewFor("   "))
    }

    /**
     * The slug arrives spelled either way — `icon_url` sends underscores while the icon
     * host serves hyphens — and casing has never been dependable on this field.
     */
    @Test
    fun `slugs match whatever their spelling`() {
        assertEquals(VehicleTopView.SCOOTER, vehicleTopViewFor("electric_scooter"))
        assertEquals(VehicleTopView.SCOOTER, vehicleTopViewFor("Electric-Scooter"))
        assertEquals(VehicleTopView.SCOOTER, vehicleTopViewFor("  ELECTRIC_SCOOTER  "))
    }

    @Test
    fun `each kind of vehicle gets its own artwork`() {
        assertEquals(VehicleTopView.CAR, vehicleTopViewFor("car"))
        assertEquals(VehicleTopView.BIKE, vehicleTopViewFor("motorcycle"))
        assertEquals(VehicleTopView.AUTO, vehicleTopViewFor("auto-rickshaw"))
        assertEquals(VehicleTopView.TRUCK, vehicleTopViewFor("container-truck"))
        assertEquals(VehicleTopView.BUS, vehicleTopViewFor("school-bus"))
    }

    /**
     * The catalogue is server-side and grows without a release, so a slug the app has
     * not seen still has to draw a vehicle — an arrow says nothing about what it is.
     */
    @Test
    fun `an unknown slug still gets a vehicle`() {
        assertEquals(VehicleTopView.CAR, vehicleTopViewFor("hovercraft"))
    }

    /**
     * Every slug `vehicle-types` actually serves, as captured from the live endpoint on
     * 2026-10-13 — all 41 of them.
     *
     * The fallback makes a missing slug invisible: it draws a car and says nothing, so
     * "fire-truck" and "ambulance" were drawn as saloons for as long as the catalogue
     * had no line for them. Listing the real response is what catches the next one, and
     * a slug the server adds later shows up here as a test that was never updated
     * rather than as a car on someone's map.
     */
    @Test
    fun `every slug the server serves is mapped deliberately`() {
        val catalogue = mapOf(
            "car" to VehicleTopView.CAR,
            "bike" to VehicleTopView.BIKE,
            "scooter" to VehicleTopView.SCOOTER,
            "truck" to VehicleTopView.TRUCK,
            "bus" to VehicleTopView.BUS,
            "bicycle" to VehicleTopView.BICYCLE,
            "auto-rickshaw" to VehicleTopView.AUTO,
            "van" to VehicleTopView.VAN,
            "pickup-truck" to VehicleTopView.PICKUP,
            "tractor" to VehicleTopView.TRACTOR,
            "ambulance" to VehicleTopView.AMBULANCE,
            "fire-truck" to VehicleTopView.FIRE_TRUCK,
            "police-car" to VehicleTopView.POLICE_CAR,
            "taxi" to VehicleTopView.TAXI,
            "electric-car" to VehicleTopView.CAR,
            "electric-bike" to VehicleTopView.BIKE,
            "electric-scooter" to VehicleTopView.SCOOTER,
            "electric-bus" to VehicleTopView.BUS,
            "electric-truck" to VehicleTopView.TRUCK,
            "suv" to VehicleTopView.SUV,
            "jeep" to VehicleTopView.SUV,
            "motorcycle" to VehicleTopView.BIKE,
            "moped" to VehicleTopView.BIKE,
            "delivery-van" to VehicleTopView.VAN,
            "mini-truck" to VehicleTopView.PICKUP,
            "heavy-truck" to VehicleTopView.TRUCK,
            "school-bus" to VehicleTopView.BUS,
            "minibus" to VehicleTopView.VAN,
            "tourist-bus" to VehicleTopView.BUS,
            "container-truck" to VehicleTopView.TRUCK,
            "dumper" to VehicleTopView.TRUCK,
            "jcb" to VehicleTopView.CONSTRUCTION,
            "crane" to VehicleTopView.CONSTRUCTION,
            "forklift" to VehicleTopView.CONSTRUCTION,
            "water-tanker" to VehicleTopView.TANKER,
            "fuel-tanker" to VehicleTopView.TANKER,
            "e-rickshaw" to VehicleTopView.AUTO,
            "golf-cart" to VehicleTopView.GOLF_CART,
            "atv" to VehicleTopView.BIKE,
            "other" to VehicleTopView.CAR,
            "three-wheeler" to VehicleTopView.AUTO,
        )
        assertEquals(41, catalogue.size, "the server serves 41 vehicle types")
        catalogue.forEach { (slug, expected) ->
            assertEquals(expected, vehicleTopViewFor(slug), slug)
            // The icon URL is what the app really reads; both endpoints send the slug
            // inside one, and this endpoint sends it hyphenated.
            assertEquals(
                expected,
                vehicleTopViewFor("https://gps.eva2z.in/api/vehicle-icons/$slug.svg"
                    .substringAfterLast('/')
                    .substringBeforeLast('.')),
                "$slug via icon_url"
            )
        }
    }

    /**
     * Every shape is reachable from some slug the server can send.
     *
     * A silhouette nothing maps to is dead weight that still has to be kept correct —
     * and the way this catalogue is written, a shape is far likelier to be added than
     * to be wired up.
     */
    @Test
    fun `every shape is reachable from the catalogue`() {
        val reached = listOf(
            "car", "suv", "taxi", "police-car", "bike", "scooter", "bicycle",
            "auto-rickshaw", "van", "ambulance", "pickup-truck", "truck", "fire-truck",
            "water-tanker", "bus", "tractor", "jcb", "golf-cart",
        ).mapNotNull { vehicleTopViewFor(it) }.toSet()
        assertEquals(
            VehicleTopView.entries.toSet(),
            reached,
            "a shape no slug reaches is drawn for nobody"
        )
    }

    // ── vehicle_id ───────────────────────────────────────────────────────────

    /**
     * The id table is a transcription of `vehicle-types`, so it is checked against the
     * same 41 rows the slug mapping is — id and slug must land on the same shape, or a
     * vehicle would change outline depending on which endpoint the screen read.
     */
    @Test
    fun `every catalogue id resolves to its own slug`() {
        val slugsById = (1..41).associateWith { vehicleSlugForId(it) }
        assertEquals(41, slugsById.values.filterNotNull().size, "ids 1..41 all resolve")
        assertEquals("car", vehicleSlugForId(1))
        assertEquals("three-wheeler", vehicleSlugForId(41))
        slugsById.forEach { (id, slug) ->
            assertEquals(
                vehicleTopViewFor(slug),
                vehicleTopViewOf(id, null),
                "id $id and slug $slug disagree about the shape"
            )
        }
    }

    /**
     * An id past the catalogue is **not** guessed at.
     *
     * The server grows types without a release, and when it does the slug carries the
     * new name while the id means nothing here — so falling back to the slug keeps the
     * better information, where defaulting to a car would throw it away.
     */
    @Test
    fun `an id this build has never seen falls back to the slug`() {
        assertNull(vehicleSlugForId(999))
        assertNull(vehicleSlugForId(0))
        assertNull(vehicleSlugForId(null))
        assertEquals(VehicleTopView.BUS, vehicleTopViewOf(999, "school-bus"))
        assertEquals(VehicleTopView.TANKER, vehicleTopViewOf(null, "fuel-tanker"))
    }

    /**
     * When both arrive, the id wins — it is the one that cannot be misspelled.
     *
     * Checked with a disagreeing pair on purpose: `latest-locations` sends id, name,
     * slug and URL together, and if they ever drift apart the integer is the field to
     * believe.
     */
    @Test
    fun `the id is preferred over the slug`() {
        // 5 is "bus" in the catalogue; the slug says otherwise and is ignored.
        assertEquals(VehicleTopView.BUS, vehicleTopViewOf(5, "motorcycle"))
        // And with no id at all, the slug is all there is.
        assertEquals(VehicleTopView.BIKE, vehicleTopViewOf(null, "motorcycle"))
        assertNull(vehicleTopViewOf(null, null))
    }

    // ── The artwork ──────────────────────────────────────────────────────────

    /**
     * Every part inside the unit box, with room left for the outline.
     *
     * Both renderers inset the artwork by [MARKER_OUTLINE_FRACTION] and stroke the
     * outline centred on each edge, so a part reaching past `0..1` would have its
     * outline clipped by the edge of the bitmap.
     */
    @Test
    fun `every part stays inside the unit box`() {
        VehicleTopView.entries.forEach { vehicle ->
            vehicleTopViewParts(vehicle).forEach { part ->
                assertTrue(part.left >= 0f && part.right <= 1f, "$vehicle across: $part")
                assertTrue(part.top >= 0f && part.bottom <= 1f, "$vehicle down: $part")
                assertTrue(part.left < part.right, "$vehicle is inside out: $part")
                assertTrue(part.top < part.bottom, "$vehicle is inside out: $part")
            }
        }
    }

    /** A radius past half the shorter side draws a shape neither platform intended. */
    @Test
    fun `no corner radius is larger than the part it rounds`() {
        VehicleTopView.entries.forEach { vehicle ->
            vehicleTopViewParts(vehicle).forEach { part ->
                val shorter = minOf(part.right - part.left, part.bottom - part.top)
                assertTrue(
                    part.radius >= 0f && part.radius <= shorter / 2f + 0.0001f,
                    "$vehicle has a $part radius larger than the part"
                )
            }
        }
    }

    /**
     * Paint order, which both renderers depend on: the tyres go down first so the body
     * covers the half of each that sits under the vehicle, and the glass goes on last
     * so it is not painted over.
     */
    @Test
    fun `tyres are painted under the body and glass over it`() {
        VehicleTopView.entries.forEach { vehicle ->
            val parts = vehicleTopViewParts(vehicle)
            val lastTyre = parts.indexOfLast { it.paint == PartPaint.TYRE }
            val firstBody = parts.indexOfFirst { it.paint == PartPaint.BODY }
            val firstGlass = parts.indexOfFirst { it.paint == PartPaint.GLASS }
            val lastBody = parts.indexOfLast {
                it.paint == PartPaint.BODY || it.paint == PartPaint.BODY_DARK
            }
            assertTrue(lastTyre in 0..<firstBody, "$vehicle paints a tyre over its body")
            // An open vehicle can have no glass at all — the golf cart has none — but
            // whatever glass there is goes on after the body.
            if (firstGlass >= 0) {
                assertTrue(firstGlass > lastBody, "$vehicle paints its body over its glass")
            }
            // And a marking goes on after everything: a cross under the roof it is
            // painted on would simply not be there.
            val firstLight = parts.indexOfFirst { it.paint == PartPaint.LIGHT }
            if (firstLight >= 0) {
                assertTrue(firstLight > lastBody, "$vehicle paints its body over its marking")
            }
        }
    }

    /** Every vehicle is a vehicle: it has a body to colour and wheels to stand on. */
    @Test
    fun `every vehicle has a body and tyres`() {
        VehicleTopView.entries.forEach { vehicle ->
            val parts = vehicleTopViewParts(vehicle)
            assertTrue(
                parts.any { it.paint == PartPaint.BODY },
                "$vehicle has no body, so the status colour has nothing to paint"
            )
            assertTrue(
                parts.count { it.paint == PartPaint.TYRE } >= 2,
                "$vehicle has fewer than two tyres"
            )
        }
    }

    /**
     * Longer than it is wide, every one of them — which is what makes a top view read
     * as a vehicle pointing somewhere, and the reason the marker can be turned at all.
     */
    @Test
    fun `every vehicle points along its own length`() {
        VehicleTopView.entries.forEach { vehicle ->
            val body = vehicleTopViewParts(vehicle).filter {
                it.paint == PartPaint.BODY || it.paint == PartPaint.BODY_DARK
            }
            val width = body.maxOf { it.right } - body.minOf { it.left }
            val length = body.maxOf { it.bottom } - body.minOf { it.top }
            assertTrue(length > width, "$vehicle is $width wide and only $length long")
        }
    }

    /** The three-wheeler really has three. */
    @Test
    fun `an auto has one wheel at the front and two at the back`() {
        val tyres = vehicleTopViewParts(VehicleTopView.AUTO)
            .filter { it.paint == PartPaint.TYRE }
        assertEquals(3, tyres.size, "an auto-rickshaw has three wheels")
        val front = tyres.minByOrNull { it.top }!!
        assertEquals(
            1,
            tyres.count { it.overlapsAcross(front) },
            "the front wheel is on its own, down the middle"
        )
    }

    /** Whether two parts share any of the marker's width. */
    private fun MarkerPart.overlapsAcross(other: MarkerPart): Boolean =
        left < other.right && other.left < right
}
