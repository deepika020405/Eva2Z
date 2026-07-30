package com.developer.ev2z.ui.screens.fuelPrice


data class FuelPrice(
    val state: String,
    val petrolPrice: Double,
    val dieselPrice: Double,
    val petrolDeltaToday: Double? = null,
    val dieselDeltaToday: Double? = null,
    val updatedAt: String,
    val source: String = "Eva2z"
)


data class NearbyStatePrice(
    val stateName: String,
    val petrolPrice: Double,
    val dieselPrice: Double
)


// How the currently shown state was decided.
enum class FuelLocationStatus {
    IDLE,
    // Location not granted yet — the screen asks before showing any rate.
    PERMISSION_REQUIRED,
    // Waiting on the permission prompt or a GPS fix.
    LOCATING,
    // Rates belong to the state the device is actually in.
    DETECTED,
    // Granted, but no usable fix (GPS off, indoors, timed out). Falls back to the last
    // state this user resolved, or to nothing at all on a first run.
    UNAVAILABLE,
    // User picked a state by hand; GPS no longer overrides it.
    MANUAL
}


// Why a location request the user asked for couldn't be answered — drives the popup.
enum class LocationIssue {
    // Phone-wide location services (GPS) are switched off.
    SERVICE_OFF,
    // The app doesn't have permission and the OS won't prompt again.
    PERMISSION_DENIED,
    // Granted and on, but no fix arrived (indoors, timeout).
    NO_FIX
}


data class FuelPriceUiState(
    val isLoading: Boolean = false,
    val selectedState: String = "",
    val detectedCity: String? = null,
    val locationStatus: FuelLocationStatus = FuelLocationStatus.IDLE,
    val locationIssue: LocationIssue? = null,
    val availableStates: List<String> = emptyList(),
    val current: FuelPrice? = null,
    val nearbyStates: List<NearbyStatePrice> = emptyList(),
    val error: String? = null
) {
    // No rates to show and no location to base them on. There is no default state to
    // fall back to, so the screen asks for location rather than showing someone else's
    // prices. Covers both "never asked" and "asked but couldn't get a fix".
    val needsLocationPermission: Boolean
        get() = current == null && (
            locationStatus == FuelLocationStatus.PERMISSION_REQUIRED ||
            locationStatus == FuelLocationStatus.UNAVAILABLE
        )

    // True when the block is because location was refused, false when it was granted
    // but no fix arrived — the two need different wording and different buttons.
    val locationPermissionMissing: Boolean
        get() = locationStatus == FuelLocationStatus.PERMISSION_REQUIRED

    val isLocating: Boolean
        get() = locationStatus == FuelLocationStatus.LOCATING

    // City only means something while the state came from a GPS fix.
    private val cityForLabel: String?
        get() = detectedCity
            ?.takeIf { locationStatus == FuelLocationStatus.DETECTED }
            ?.takeIf { it.isNotBlank() && !it.equals(selectedState, ignoreCase = true) }

    val selectedLocationLabel: String
        get() = when {
            selectedState.isBlank() -> "Select a state"
            cityForLabel != null -> "$cityForLabel, $selectedState"
            else -> selectedState
        }

    // Compact label for the home-screen widget.
    val homeLocationLabel: String
        get() = when {
            selectedState.isNotBlank() -> cityForLabel ?: selectedState
            locationStatus == FuelLocationStatus.LOCATING -> "locating…"
            locationStatus == FuelLocationStatus.PERMISSION_REQUIRED -> "tap to allow location"
            locationStatus == FuelLocationStatus.UNAVAILABLE -> "tap to enable location"
            else -> "tap to choose a state"
        }
}