package com.developer.ev2z.data.local.model

/**
 * The `vehicle-types` catalogue, id to slug — 41 rows, ids 1..41, transcribed from the
 * live endpoint and re-checked on 2026-10-14.
 *
 * A plain transcription on purpose: the decision about which *shape* a vehicle gets
 * belongs in one place ([com.developer.ev2z.ui.components.vehicleTopViewFor]), not here.
 */
private val CATALOGUE: Map<Int, String> = mapOf(
    1 to "car",
    2 to "bike",
    3 to "scooter",
    4 to "truck",
    5 to "bus",
    6 to "bicycle",
    7 to "auto-rickshaw",
    8 to "van",
    9 to "pickup-truck",
    10 to "tractor",
    11 to "ambulance",
    12 to "fire-truck",
    13 to "police-car",
    14 to "taxi",
    15 to "electric-car",
    16 to "electric-bike",
    17 to "electric-scooter",
    18 to "electric-bus",
    19 to "electric-truck",
    20 to "suv",
    21 to "jeep",
    22 to "motorcycle",
    23 to "moped",
    24 to "delivery-van",
    25 to "mini-truck",
    26 to "heavy-truck",
    27 to "school-bus",
    28 to "minibus",
    29 to "tourist-bus",
    30 to "container-truck",
    31 to "dumper",
    32 to "jcb",
    33 to "crane",
    34 to "forklift",
    35 to "water-tanker",
    36 to "fuel-tanker",
    37 to "e-rickshaw",
    38 to "golf-cart",
    39 to "atv",
    40 to "other",
    41 to "three-wheeler",
)

/** Every slug the catalogue serves, for telling a real type from a guess. */
private val CATALOGUE_SLUGS: Set<String> = CATALOGUE.values.toSet()

/**
 * The `vehicle-types` slug for a `vehicle_id`, or null for an id the catalogue has no
 * row for.
 *
 * Null rather than "car" for an unknown id: an id past the catalogue means the server
 * grew a type this build has never heard of, and the caller still has the slug to fall
 * back on — which carries the new name, where a guess here would throw it away.
 */
internal fun vehicleSlugForId(vehicleId: Int?): String? =
    vehicleId?.let { CATALOGUE[it] }

/** Whether [slug] is a type the catalogue actually serves, rather than anything at all. */
internal fun isCatalogueSlug(slug: String?): Boolean =
    normalizeVehicleSlug(slug) in CATALOGUE_SLUGS

/**
 * One tracker's vehicle type, from whichever of the three fields the endpoint sent.
 *
 * ### Why `vehicle_id` decides
 *
 * It is the field the owner actually sets. `add-device` sends a `vehicle_id` and nothing
 * else about the vehicle, so that id *is* the assignment; `icon` and `icon_url` are
 * derived from it server-side, and when the two disagree it is the derivation that has
 * gone wrong, not the assignment.
 *
 * That disagreement is real, not hypothetical. Device `0869727076942661` comes back from
 * `latest-locations` as:
 *
 * ```
 * "vehicle_id": 1, "icon": "motorcycle", "icon_url": ".../motorcycle.svg"
 * ```
 *
 * Catalogue id 1 is **Car** and the vehicle is a car — so `icon` is the wrong field here
 * and reading it drew a bike on the map for a car. An integer also cannot arrive
 * misspelled, hyphenated one way on one endpoint and underscored on another, or wrapped
 * in a URL that 404s, all of which the slug has done.
 *
 * ### Order
 *
 *  1. `vehicle_id` through the catalogue.
 *  2. `icon`, if the catalogue serves it.
 *  3. The slug out of `icon_url`, if the catalogue serves it.
 *  4. Whatever slug there was, even unrecognised — the server has sent `"sedan"`, which
 *     is not in `vehicle-types` but which `vehicleTopViewFor` still draws as a car.
 *
 * Steps 2–4 are what `live-location/{id}` relies on: re-checked 2026-10-14, it sends no
 * `vehicle_id` at all, only `icon_url`.
 *
 * Null only when the endpoint sent none of the three, which means no vehicle is assigned
 * and the marker is drawn as the plain heading arrow.
 */
internal fun resolveVehicleSlug(
    vehicleId: Int?,
    icon: String? = null,
    iconUrl: String? = null,
): String? {
    vehicleSlugForId(vehicleId)?.let { return it }

    val fromIcon = normalizeVehicleSlug(icon)
    if (fromIcon != null && fromIcon in CATALOGUE_SLUGS) return fromIcon

    val fromUrl = normalizeVehicleSlug(
        iconUrl?.substringAfterLast('/')?.substringBeforeLast('.')
    )
    if (fromUrl != null && fromUrl in CATALOGUE_SLUGS) return fromUrl

    return fromIcon ?: fromUrl
}

/**
 * A slug in the one spelling the rest of the app compares against.
 *
 * `latest-locations` sends `electric-scooter` and the icon URLs spell the same type
 * `electric_scooter`, so the separator has to be settled before anything is matched.
 */
private fun normalizeVehicleSlug(raw: String?): String? =
    raw?.trim()?.lowercase()?.replace('_', '-')?.takeIf { it.isNotEmpty() }
