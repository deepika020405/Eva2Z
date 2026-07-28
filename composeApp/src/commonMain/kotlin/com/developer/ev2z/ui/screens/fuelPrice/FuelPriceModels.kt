package com.developer.ev2z.ui.screens.fuelPrice

/**
 * Fuel price snapshot for one state / union territory.
 *
 * The API returns today's rates only — there is no previous-day figure — so the
 * deltas stay null and the cards omit the change line rather than claiming
 * "No Change" for something we can't know.
 */
data class FuelPrice(
    val state: String,
    val petrolPrice: Double,
    val dieselPrice: Double,
    val petrolDeltaToday: Double? = null,
    val dieselDeltaToday: Double? = null,
    val updatedAt: String,
    val source: String = "Eva2z"
)

/**
 * Lightweight row used in the "Nearby states" list.
 */
data class NearbyStatePrice(
    val stateName: String,
    val petrolPrice: Double,
    val dieselPrice: Double
)

/**
 * UI state for the fuel price screen. Prices are state/UT-wide — the API's district
 * breakdown is deliberately not surfaced.
 */
data class FuelPriceUiState(
    val isLoading: Boolean = false,
    val selectedState: String = "",
    val availableStates: List<String> = emptyList(),
    val current: FuelPrice? = null,
    val nearbyStates: List<NearbyStatePrice> = emptyList(),
    val error: String? = null
) {
    val selectedLocationLabel: String
        get() = selectedState.ifBlank { "Select a state" }
}