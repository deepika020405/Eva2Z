package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /api/fuel-prices  (optionally ?state=<state name>)
 *
 * The payload is double-wrapped: the outer envelope carries the app-level status,
 * the inner [FuelPricesPayload] carries the upstream provider's own success flag.
 */
@Serializable
data class FuelPricesResponse(
    val status: Boolean = false,
    val message: String? = null,
    @SerialName("api_status") val apiStatus: Int? = null,
    val data: FuelPricesPayload? = null
)

@Serializable
data class FuelPricesPayload(
    val success: Boolean = false,
    val data: List<FuelStateDto> = emptyList(),
    val summary: FuelPricesSummary? = null
)

@Serializable
data class FuelPricesSummary(
    @SerialName("total_states") val totalStates: Int = 0,
    @SerialName("total_districts") val totalDistricts: Int = 0,
    val countries: List<String> = emptyList(),
    @SerialName("fuel_types") val fuelTypes: List<String> = emptyList()
)

@Serializable
data class FuelStateDto(
    @SerialName("state_id") val stateId: Int = 0,
    @SerialName("state_name") val stateName: String = "",
    val country: String? = null,
    @SerialName("fuel_prices") val fuelPrices: FuelPricePairDto? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    // Absent for a few union territories (e.g. Daman Diu).
    val coordinates: FuelCoordinatesDto? = null
    // The payload also carries a per-state `districts` array; the app shows state/UT-wide
    // rates only, so it is left unparsed (the decoder ignores unknown keys).
)

@Serializable
data class FuelPricePairDto(
    @SerialName("petrol_price") val petrolPrice: Double? = null,
    @SerialName("diesel_price") val dieselPrice: Double? = null
)

@Serializable
data class FuelCoordinatesDto(
    val lat: Double? = null,
    @SerialName("long") val long: Double? = null
)