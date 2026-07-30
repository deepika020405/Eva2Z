package com.developer.ev2z.ui.screens.fuelPrice

import com.developer.ev2z.data.local.UserPrefs
import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.LocationPermissionEvents
import com.developer.ev2z.fetchPhoneLocation
import com.developer.ev2z.hasLocationPermission
import com.developer.ev2z.isLocationEnabled
import com.developer.ev2z.openAppSettings
import com.developer.ev2z.openLocationSettings
import com.developer.ev2z.promptEnableLocationServices
import com.developer.ev2z.repository.FuelPriceRepository
import com.developer.ev2z.response.FuelStateDto
import com.developer.ev2z.util.getCityFromLatLng
import com.developer.ev2z.util.getStateFromLatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class FuelPriceViewModel(
    private val repository: FuelPriceRepository = FuelPriceRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val prefs = UserPrefs(createStorageManager())

    private val _uiState = MutableStateFlow(FuelPriceUiState())
    val uiState: StateFlow<FuelPriceUiState> = _uiState.asStateFlow()

    private var allStates: List<FuelStateDto> = emptyList()

    // True once the user picks a state by hand — GPS stops overriding it after that.
    private var userPickedState = false

    // The "Turn on location" system dialog is raised once per session, not on every
    // resume — an explicit request from the user resets it.
    private var servicesPromptShown = false

    init {
        start()
        // The startup permission prompt lives in MainActivity/AppDelegate, so pick up
        // its answer here instead of making the user tap again.
        scope.launch {
            LocationPermissionEvents.changed.collect {
                if (hasLocationPermission() && !isLocationResolved()) {
                    if (allStates.isEmpty() && !loadCatalogue()) return@collect
                    detectState(userInitiated = false)
                }
            }
        }
    }

    fun retry() = start()

    // Called when the app comes back to the foreground — catches the user returning
    // from the system settings screen after switching location on.
    fun onAppResumed() {
        if (_uiState.value.isLocating || isLocationResolved()) return
        if (!hasLocationPermission() || !isLocationEnabled()) return
        scope.launch {
            if (allStates.isEmpty() && !loadCatalogue()) return@launch
            detectState(userInitiated = false)
        }
    }

    // "Allow location" button — triggers the system prompt, then resolves the state.
    // An explicit tap also overrides an earlier manual pick.
    fun requestLocation() {
        if (_uiState.value.isLocating) return
        userPickedState = false
        // An explicit ask always gets a fresh prompt, even if one was already shown.
        servicesPromptShown = false
        scope.launch {
            if (allStates.isEmpty() && !loadCatalogue()) return@launch
            detectState(userInitiated = true)
        }
    }

    fun dismissLocationIssue() {
        _uiState.update { it.copy(locationIssue = null) }
    }

    // Sends the user where they can fix it: the phone's location toggle when GPS is
    // off, this app's settings page when the permission itself was refused.
    fun openSettingsForIssue() {
        when (_uiState.value.locationIssue) {
            LocationIssue.SERVICE_OFF -> openLocationSettings()
            LocationIssue.PERMISSION_DENIED -> openAppSettings()
            else -> Unit
        }
        dismissLocationIssue()
    }

    // Nothing auto-detects over a state the user chose themselves, or one we already
    // pinned down by GPS — they'd both get clobbered on every app resume otherwise.
    // A remembered state shown as a fallback doesn't count: that one should upgrade
    // to the real location as soon as permission arrives.
    private fun isLocationResolved(): Boolean =
        userPickedState || _uiState.value.locationStatus == FuelLocationStatus.DETECTED

    fun onStateSelected(stateName: String) {
        if (stateName == _uiState.value.selectedState) return
        userPickedState = true
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val refreshed = try {
                repository.getState(stateName)
            } catch (t: Throwable) {
                println("⚠️ Fuel: state refresh failed for $stateName — ${t.message}")
                null
            }
            // Fall back to the catalogue copy so a failed refresh still switches state.
            val state = refreshed ?: allStates.firstOrNull { it.stateName == stateName }
            if (state == null) {
                _uiState.update { it.copy(isLoading = false, error = "No prices for $stateName") }
                return@launch
            }
            if (refreshed != null) cacheState(state)
            rememberState(state.stateName)
            _uiState.update {
                it.showState(state, FuelLocationStatus.MANUAL, city = null)
                    .copy(locationIssue = null)
            }
        }
    }

    private fun start() {
        scope.launch {
            if (!loadCatalogue()) return@launch

            // Show the last state this user resolved while the fix is in flight, so the
            // home card isn't blank on a warm start. It upgrades to the live location as
            // soon as the fix lands. Nothing is shown on a first run — there is no default.
            rememberedState()?.let { remembered ->
                _uiState.update {
                    it.showState(remembered, FuelLocationStatus.UNAVAILABLE, city = null)
                }
            }

            // Attempt a fix whether or not permission is granted yet: fetchPhoneLocation
            // raises the system prompt on both platforms, so opening the app is what asks
            // for location. The 30s timeout covers the dialog sitting on screen.
            detectState(userInitiated = false)
        }
    }

    // Returns false when the catalogue couldn't be loaded (error already published).
    private suspend fun loadCatalogue(): Boolean {
        _uiState.update { it.copy(isLoading = true, error = null) }
        return try {
            allStates = repository.getAllStates()
            if (allStates.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, error = "No fuel prices available") }
                false
            } else {
                _uiState.update { it.copy(availableStates = stateNames()) }
                true
            }
        } catch (t: Throwable) {
            _uiState.update {
                it.copy(isLoading = false, error = t.message ?: "Couldn't load fuel prices")
            }
            false
        }
    }

    // [userInitiated] gates the popup: it only appears when the user actually asked
    // for their location, never as a surprise on app open.
    private suspend fun detectState(userInitiated: Boolean) {
        // The phone's location switch being off is the common cause, and no amount of
        // permission gets around it. Ask the system to switch it on first — on Android
        // that is Play Services' own "Turn on location" dialog, so the user fixes it
        // without leaving the app.
        if (!isLocationEnabled() && !promptLocationServices()) {
            showFallback(
                status = FuelLocationStatus.UNAVAILABLE,
                // Shown on app open too, not just when the user asked. Suppressing it
                // there left a phone with location off sitting on an empty screen with
                // no explanation of why there were no rates.
                issue = LocationIssue.SERVICE_OFF
            )
            return
        }

        _uiState.update {
            it.copy(
                isLoading = it.current == null,
                error = null,
                locationIssue = null,
                locationStatus = FuelLocationStatus.LOCATING
            )
        }

        val fix = currentLatLng()
        if (fix == null) {
            // No coordinates: either the user declined, or the fix timed out.
            val granted = hasLocationPermission()
            showFallback(
                status = if (granted) {
                    FuelLocationStatus.UNAVAILABLE
                } else {
                    FuelLocationStatus.PERMISSION_REQUIRED
                },
                issue = when {
                    !userInitiated -> null
                    granted -> LocationIssue.NO_FIX
                    else -> LocationIssue.PERMISSION_DENIED
                }
            )
            return
        }

        val (lat, lng) = fix
        val geoState = runCatching { getStateFromLatLng(lat, lng) }.getOrNull().orEmpty()
        val city = runCatching { getCityFromLatLng(lat, lng) }.getOrNull().orEmpty()

        // Prefer the geocoded name; fall back to the closest state centroid when the
        // geocoder is unavailable or spells the state differently than the API.
        val match = matchByName(geoState) ?: nearestState(lat, lng)
        if (match == null) {
            showFallback(
                status = FuelLocationStatus.UNAVAILABLE,
                issue = if (userInitiated) LocationIssue.NO_FIX else null
            )
            return
        }

        val fresh = try {
            repository.getState(match.stateName)
        } catch (t: Throwable) {
            println("⚠️ Fuel: state refresh failed for ${match.stateName} — ${t.message}")
            null
        }
        val state = fresh ?: match
        if (fresh != null) cacheState(state)
        rememberState(state.stateName)
        _uiState.update { it.showState(state, FuelLocationStatus.DETECTED, city) }
    }

    // Shows the last state this user actually resolved when GPS can't decide right now.
    //
    // There is deliberately no default state here. Falling back to the API's `is_default`
    // row (or a hardcoded one) meant a user who never granted location saw rates from a
    // state they may have never been in, presented as if they were local. When nothing
    // has been resolved for this user yet we show no rates at all and ask for location.
    private fun showFallback(status: FuelLocationStatus, issue: LocationIssue?) {
        val state = rememberedState()

        _uiState.update {
            if (state == null) {
                it.copy(
                    isLoading = false,
                    locationStatus = status,
                    locationIssue = issue,
                    // Not an error — the prices loaded fine, we just don't know where
                    // the user is. The screen asks for location instead.
                    error = null
                )
            } else {
                it.showState(state, status, city = null).copy(locationIssue = issue)
            }
        }
    }

    /**
     * Raises the system prompt to switch location services on and suspends for the answer.
     *
     * Asked at most once per session so returning to the screen doesn't nag; an explicit
     * tap on "Allow location" / "Try again" clears the flag and asks again.
     */
    private suspend fun promptLocationServices(): Boolean {
        if (servicesPromptShown) return isLocationEnabled()
        servicesPromptShown = true
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Boolean> { cont ->
                var delivered = false
                promptEnableLocationServices { enabled ->
                    if (!delivered) {
                        delivered = true
                        cont.resume(enabled)
                    }
                }
            }
        } ?: isLocationEnabled()
    }

    // Suspends until the platform delivers a fix. (0,0) means denied/unavailable.
    // The timeout covers the permission dialog still being on screen.
    private suspend fun currentLatLng(): Pair<Double, Double>? {
        val fix = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine<Pair<Double, Double>> { cont ->
                var delivered = false
                fetchPhoneLocation { lat, lng ->
                    if (!delivered) {
                        delivered = true
                        cont.resume(lat to lng)
                    }
                }
            }
        } ?: return null
        return fix.takeIf { it.first != 0.0 || it.second != 0.0 }
    }

    private fun matchByName(geoState: String): FuelStateDto? {
        val target = normalizeState(geoState)
        if (target.isBlank()) return null
        return allStates.firstOrNull { normalizeState(it.stateName) == target }
            ?: allStates.firstOrNull {
                val name = normalizeState(it.stateName)
                name.isNotBlank() && (name.startsWith(target) || target.startsWith(name))
            }
    }

    // The API spells some states differently than the geocoders ("Jammu Kashmir" vs
    // "Jammu and Kashmir", "Pondicherry" vs "Puducherry"), so compare on a reduced form.
    private fun normalizeState(raw: String): String {
        val cleaned = raw.lowercase()
            .replace("&", "and")
            .filter { it.isLetterOrDigit() || it == ' ' }
            .split(' ')
            .filter { it.isNotBlank() && it !in NOISE_WORDS }
            .joinToString("")
        return STATE_ALIASES[cleaned] ?: cleaned
    }

    private fun nearestState(lat: Double, lng: Double): FuelStateDto? = allStates
        .filter { it.latitude != null && it.longitude != null }
        .minByOrNull { distanceKm(lat, lng, it.latitude!!, it.longitude!!) }

    private fun cacheState(state: FuelStateDto) {
        allStates = allStates.map { if (it.stateName == state.stateName) state else it }
    }

    private fun rememberState(stateName: String) = prefs.saveString(KEY_LAST_STATE, stateName)

    private fun rememberedState(): FuelStateDto? {
        val name = prefs.getString(KEY_LAST_STATE, "").takeIf { it.isNotBlank() } ?: return null
        return allStates.firstOrNull { it.stateName.equals(name, ignoreCase = true) }
    }

    private fun stateNames(): List<String> = allStates.map { it.stateName }.sorted()

    private fun FuelPriceUiState.showState(
        state: FuelStateDto,
        status: FuelLocationStatus,
        city: String?
    ): FuelPriceUiState = copy(
        isLoading = false,
        error = null,
        selectedState = state.stateName,
        detectedCity = city?.takeIf { it.isNotBlank() },
        locationStatus = status,
        availableStates = stateNames(),
        current = FuelPrice(
            state = state.stateName,
            petrolPrice = state.petrolPrice ?: 0.0,
            dieselPrice = state.dieselPrice ?: 0.0,
            updatedAt = updatedLabel(state.priceDate)
        ),
        nearbyStates = nearbyTo(state)
    )


    // Every other state, closest first — the screen shows a few and expands on demand.
    // States the API gives no coordinates for are appended alphabetically so they
    // stay reachable in the expanded list.
    private fun nearbyTo(state: FuelStateDto): List<NearbyStatePrice> {
        val others = allStates.filter { it.stateName != state.stateName }
        val lat = state.latitude
        val long = state.longitude

        val (located, unlocated) = others.partition { it.latitude != null && it.longitude != null }
        val ordered = if (lat != null && long != null) {
            located.sortedBy { distanceKm(lat, long, it.latitude!!, it.longitude!!) }
        } else {
            located.sortedBy { it.stateName }
        }

        return (ordered + unlocated.sortedBy { it.stateName }).map {
            NearbyStatePrice(
                stateName = it.stateName,
                petrolPrice = it.petrolPrice ?: 0.0,
                dieselPrice = it.dieselPrice ?: 0.0
            )
        }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = (lat2 - lat1).toRadians()
        val dLon = (lon2 - lon1).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1.toRadians()) * cos(lat2.toRadians()) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun Double.toRadians(): Double = this * PI / 180.0

    /**
     * `price_date` is the day the rates are for. When that is today the read time is
     * the useful part; an older date is shown as-is rather than passed off as current.
     */
    @OptIn(ExperimentalTime::class)
    private fun updatedLabel(priceDate: String?): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val date = priceDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (date != null && date != now.date) {
            return "${date.day} ${MONTHS[date.month.ordinal]} ${date.year}"
        }
        val hour = if (now.hour % 12 == 0) 12 else now.hour % 12
        val minute = now.minute.toString().padStart(2, '0')
        val meridiem = if (now.hour < 12) "AM" else "PM"
        return "today, $hour:$minute $meridiem"
    }

    private companion object {
        const val KEY_LAST_STATE = "fuel_last_state"
        const val LOCATION_TIMEOUT_MS = 30_000L

        val MONTHS = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        val NOISE_WORDS = setOf(
            "and", "the", "of", "state", "province", "nct", "national", "capital",
            "territory", "union", "ut", "islands", "island"
        )

        // Left side is the normalized geocoder spelling, right side the normalized API name.
        val STATE_ALIASES = mapOf(
            "puducherry" to "pondicherry",
            "orissa" to "odisha",
            "uttaranchal" to "uttarakhand",
            "dadranagarhavelidamandiu" to "dadranagarhaveli",
            "damandiudadranagarhaveli" to "damandiu"
        )
    }
}