package com.developer.ev2z.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /api/fuel-prices — every state / union territory for [date].
 */
@Serializable
data class FuelPricesResponse(
    val status: Boolean = false,
    val message: String? = null,
    val date: String? = null,
    val data: List<FuelStateDto> = emptyList()
)

/**
 * GET /api/fuel-prices/{state} — a single state, same row shape as the list endpoint.
 */
@Serializable
data class FuelPriceStateResponse(
    val status: Boolean = false,
    val message: String? = null,
    val date: String? = null,
    val data: FuelStateDto? = null
)

/**
 * One row of the fuel-price table. The backend sends the rates as strings
 * ("117.51") and `is_default` as 0/1, so both are decoded raw and exposed
 * through the typed accessors below.
 */
@Serializable
data class FuelStateDto(
    val id: Int = 0,
    @SerialName("state_id") val stateId: Int = 0,
    @SerialName("state_name") val stateName: String = "",
    val country: String? = null,
    @SerialName("petrol_price") val petrolPriceRaw: String? = null,
    @SerialName("diesel_price") val dieselPriceRaw: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("is_default") val isDefaultRaw: Int = 0,
    @SerialName("price_date") val priceDate: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val petrolPrice: Double? get() = petrolPriceRaw?.trim()?.toDoubleOrNull()
    val dieselPrice: Double? get() = dieselPriceRaw?.trim()?.toDoubleOrNull()
    val isDefault: Boolean get() = isDefaultRaw != 0
}