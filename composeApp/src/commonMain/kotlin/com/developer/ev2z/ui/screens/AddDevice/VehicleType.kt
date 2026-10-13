package com.developer.ev2z.ui.screens.AddDevice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.ElectricMoped
import androidx.compose.material.icons.filled.ElectricRickshaw
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.FireTruck
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `GET vehicle-types` — the catalogue the add-device form picks from.
 *
 * [vehicleId] runs 1..41 — "Car" is 1, "Three Wheeler" is 41, checked against the live
 * endpoint on 2026-10-13. (This said ids started at 0; they do not.) Absence is the
 * nullable [VehicleType] itself, never a sentinel id, so the range does not matter to
 * the caller either way.
 *
 * [icon] is the slug that comes back later on `live-location/{id}`'s `icon_url` — which
 * is what actually decides the map marker, through
 * [com.developer.ev2z.ui.components.vehicleTopViewFor].
 */
@Serializable
data class VehicleType(
    @SerialName("vehicle_id") val vehicleId: Int,
    val name: String,
    /** Slug like "auto-rickshaw" — what [icon] is chosen from. */
    val icon: String = "",
    /**
     * The server's own artwork for this type. Not drawn: it is an SVG wrapping a
     * single emoji glyph, and the app has no SVG decoder, so it would render as a
     * grey box or tofu. Kept because the catalogue sends it and a later, real icon
     * set would arrive here.
     */
    @SerialName("icon_url") val iconUrl: String = ""
) {
    /**
     * A drawn icon for this type, chosen from the slug.
     *
     * Local rather than downloaded so the picker works offline and at any size, and
     * because several slugs share one sensible glyph — a mini truck, a container
     * truck and a dumper are all a lorry.
     */
    val displayIcon: ImageVector get() = iconFor(icon.ifBlank { name })
}

@Serializable
data class VehicleTypesResponse(
    val success: Boolean = false,
    val message: String = "",
    val total: Int = 0,
    val data: List<VehicleType> = emptyList()
)

/**
 * Slug (or name) to glyph. Unmapped slugs fall back to a generic vehicle rather than
 * to nothing: the catalogue is server-side and grows without the app being rebuilt.
 */
private fun iconFor(raw: String): ImageVector =
    when (raw.trim().lowercase().replace(' ', '-')) {
        "car", "suv", "jeep" -> Icons.Filled.DirectionsCar
        "electric-car" -> Icons.Filled.ElectricCar
        "taxi" -> Icons.Filled.LocalTaxi
        "police-car" -> Icons.Filled.LocalPolice
        "ambulance" -> Icons.Filled.LocalHospital
        "fire-truck" -> Icons.Filled.FireTruck

        "bike", "motorcycle", "scooter" -> Icons.Filled.TwoWheeler
        "electric-bike" -> Icons.Filled.ElectricBike
        "electric-scooter" -> Icons.Filled.ElectricScooter
        "moped" -> Icons.Filled.ElectricMoped
        "bicycle" -> Icons.Filled.DirectionsBike

        "truck", "mini-truck", "heavy-truck", "container-truck",
        "pickup-truck", "electric-truck", "dumper" -> Icons.Filled.LocalShipping
        "van", "delivery-van" -> Icons.Filled.AirportShuttle

        "bus", "school-bus", "minibus", "tourist-bus", "electric-bus" -> Icons.Filled.DirectionsBus

        "auto-rickshaw", "e-rickshaw", "three-wheeler" -> Icons.Filled.ElectricRickshaw

        "tractor" -> Icons.Filled.Agriculture
        "jcb", "crane" -> Icons.Filled.Construction
        "forklift" -> Icons.Filled.PrecisionManufacturing

        "water-tanker" -> Icons.Filled.WaterDrop
        "fuel-tanker" -> Icons.Filled.LocalGasStation
        "golf-cart" -> Icons.Filled.GolfCourse
        "atv" -> Icons.Filled.Terrain

        else -> Icons.Filled.Commute
    }
