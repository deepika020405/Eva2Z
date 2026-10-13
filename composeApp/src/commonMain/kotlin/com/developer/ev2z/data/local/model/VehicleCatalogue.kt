package com.developer.ev2z.data.local.model

/**
 * The `vehicle-types` slug for a `vehicle_id`, or null for an id the catalogue has no
 * row for.
 *
 * **The id is the better key and should be tried first.** An integer cannot arrive
 * misspelled, hyphenated one way on one endpoint and underscored on another, or wrapped
 * in a URL that 404s — all of which the slug has actually done. `latest-locations` and
 * `route-history` both send `vehicle_id` as of 2026-10-13; `live-location/{id}` does
 * not yet, which is exactly why the slug stays as the fallback rather than being
 * deleted.
 *
 * It resolves to a *slug* rather than straight to a [VehicleTopView] on purpose: the
 * decision about which shape a vehicle gets belongs in one place
 * ([vehicleTopViewFor]), and this table stays a plain transcription of the catalogue
 * endpoint — 41 rows, ids 1..41, checked against the live response on 2026-10-13.
 */
internal fun vehicleSlugForId(vehicleId: Int?): String? = when (vehicleId) {
    1 -> "car"
    2 -> "bike"
    3 -> "scooter"
    4 -> "truck"
    5 -> "bus"
    6 -> "bicycle"
    7 -> "auto-rickshaw"
    8 -> "van"
    9 -> "pickup-truck"
    10 -> "tractor"
    11 -> "ambulance"
    12 -> "fire-truck"
    13 -> "police-car"
    14 -> "taxi"
    15 -> "electric-car"
    16 -> "electric-bike"
    17 -> "electric-scooter"
    18 -> "electric-bus"
    19 -> "electric-truck"
    20 -> "suv"
    21 -> "jeep"
    22 -> "motorcycle"
    23 -> "moped"
    24 -> "delivery-van"
    25 -> "mini-truck"
    26 -> "heavy-truck"
    27 -> "school-bus"
    28 -> "minibus"
    29 -> "tourist-bus"
    30 -> "container-truck"
    31 -> "dumper"
    32 -> "jcb"
    33 -> "crane"
    34 -> "forklift"
    35 -> "water-tanker"
    36 -> "fuel-tanker"
    37 -> "e-rickshaw"
    38 -> "golf-cart"
    39 -> "atv"
    40 -> "other"
    41 -> "three-wheeler"
    // Null, not "car": an id past the catalogue means the server grew a type this
    // build has never heard of, and the caller still has the slug to fall back on —
    // which carries the new name, where a guess here would throw it away.
    else -> null
}
