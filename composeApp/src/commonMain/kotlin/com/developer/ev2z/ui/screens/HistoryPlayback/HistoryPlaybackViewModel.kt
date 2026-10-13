


package com.developer.ev2z.ui.screens.HistoryPlayback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.StorageManager
import com.developer.ev2z.network.ApiService
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.RoadSnapService
import com.developer.ev2z.network.SnappedRoute
import com.developer.ev2z.repository.HistoryPlaybackRepository
import com.developer.ev2z.response.Device
import com.developer.ev2z.response.RoutePoint
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


sealed class HistoryPlaybackUiState {
    object Idle : HistoryPlaybackUiState()
    object Loading : HistoryPlaybackUiState()
    object Success : HistoryPlaybackUiState()
    data class Error(val message: String) : HistoryPlaybackUiState()
}


@OptIn(kotlin.time.ExperimentalTime::class)
class HistoryPlaybackViewModel(
    private val apiService: ApiService,
    private val storageManager: StorageManager
) : ViewModel() {

    private val repository =
        HistoryPlaybackRepository(apiService)


    private val roadSnapService =
        RoadSnapService()


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow<HistoryPlaybackUiState>(
            HistoryPlaybackUiState.Idle
        )

    val uiState: StateFlow<HistoryPlaybackUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // DEVICES
    // =========================================================

    private val _devices =
        MutableStateFlow<List<Device>>(emptyList())

    val devices: StateFlow<List<Device>> =
        _devices.asStateFlow()


    private val _selectedDevice =
        MutableStateFlow<Device?>(null)

    val selectedDevice: StateFlow<Device?> =
        _selectedDevice.asStateFlow()


    // =========================================================
    // DATE
    // =========================================================

    private val today =
        Instant.fromEpochMilliseconds(getTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

    val maxSelectableDate =
        today

    val minSelectableDate =
        today.minus(
            3,
            DateTimeUnit.MONTH
        )


    private val _startDate =
        MutableStateFlow(
            today.toString()
        )

    val startDate: StateFlow<String> =
        _startDate.asStateFlow()


    private val _endDate =
        MutableStateFlow(
            today.toString()
        )

    val endDate: StateFlow<String> =
        _endDate.asStateFlow()


    // =========================================================
    // ROUTE POINTS
    // =========================================================

    private val _routePoints =
        MutableStateFlow<List<RoutePoint>>(
            emptyList()
        )

    val routePoints: StateFlow<List<RoutePoint>> =
        _routePoints.asStateFlow()


    // =========================================================
    // VEHICLE TYPE
    // =========================================================
    //
    // The vehicle this terminal is fitted to, as the slug out of the response's
    // `vehicle_icon_url`, so the playback marker is the vehicle rather than a
    // dropped pin. Null until a history response lands, and on any device the
    // server has no vehicle for — the marker falls back to a heading arrow.
    // =========================================================

    private val _vehicleSlug =
        MutableStateFlow<String?>(null)

    val vehicleSlug: StateFlow<String?> =
        _vehicleSlug.asStateFlow()


    // =========================================================
    // ROAD-SNAPPED ROUTE
    // =========================================================
    //
    // The route as the map draws it: the recorded points laid onto the road network
    // so the line follows the streets instead of cutting straight between fixes.
    //
    // Published as the straight trace first and replaced when the Roads API answers,
    // so the route appears immediately and sharpens a moment later.
    // =========================================================

    private val _snappedRoute =
        MutableStateFlow(SnappedRoute.EMPTY)

    val snappedRoute: StateFlow<SnappedRoute> =
        _snappedRoute.asStateFlow()


    // =========================================================
    // PLAYBACK
    // =========================================================

    private val _isPlaying =
        MutableStateFlow(false)

    val isPlaying: StateFlow<Boolean> =
        _isPlaying.asStateFlow()


    private val _currentPlaybackIndex =
        MutableStateFlow(0)

    val currentPlaybackIndex: StateFlow<Int> =
        _currentPlaybackIndex.asStateFlow()


    // =========================================================
    // DISTANCE
    // =========================================================

    private val _totalDistance =
        MutableStateFlow(0.0)

    val totalDistance: StateFlow<Double> =
        _totalDistance.asStateFlow()


    // =========================================================
    // AVERAGE SPEED
    // =========================================================

    private val _averageSpeed =
        MutableStateFlow(0.0)

    val averageSpeed: StateFlow<Double> =
        _averageSpeed.asStateFlow()


    // =========================================================
    // CURRENT SPEED
    // =========================================================

    private val _currentSpeed =
        MutableStateFlow(0)

    val currentSpeed: StateFlow<Int> =
        _currentSpeed.asStateFlow()


    // =========================================================
    // DEVICE DROPDOWN
    // =========================================================

    private val _showDeviceDropdown =
        MutableStateFlow(false)

    val showDeviceDropdown: StateFlow<Boolean> =
        _showDeviceDropdown.asStateFlow()


    // =========================================================
    // NO DATA DIALOG
    // =========================================================

    private val _showNoDataDialog =
        MutableStateFlow(false)

    val showNoDataDialog: StateFlow<Boolean> =
        _showNoDataDialog.asStateFlow()


    // =========================================================
    // PLAYBACK JOB
    // =========================================================

    private var playbackJob: Job? = null


    // =========================================================
    // HISTORY JOB
    // =========================================================

    private var historyJob: Job? = null


    // =========================================================
    // ROAD SNAP JOB
    // =========================================================

    private var snapJob: Job? = null


    // =========================================================
    // LOAD DEVICES
    // =========================================================

    fun loadDevices() {

        viewModelScope.launch {

            _uiState.value =
                HistoryPlaybackUiState.Loading

            try {

                val token =
                    storageManager.getString(
                        "token",
                        ""
                    ) ?: ""

                println(
                    "🔑 [History] loadDevices token=" +
                            if (token.isBlank())
                                "EMPTY ❌"
                            else
                                "OK ✅"
                )


                val response =
                    apiService.getMyDevices(token)


                println(
                    "📱 [History] devices loaded: " +
                            "success=${response.success}, " +
                            "count=${response.devices.size}"
                )


                if (response.success) {

                    _devices.value =
                        response.devices


                    // Auto select first device
                    val first =
                        response.devices.firstOrNull()


                    if (
                        first != null &&
                        _selectedDevice.value == null
                    ) {

                        selectDevice(first)

                    } else {

                        _uiState.value =
                            HistoryPlaybackUiState.Idle
                    }


                } else {

                    _uiState.value =
                        HistoryPlaybackUiState.Error(
                            "Failed to load devices"
                        )
                }


            } catch (e: Exception) {

                println(
                    "💥 [History] loadDevices exception: " +
                            e.message
                )


                _uiState.value =
                    HistoryPlaybackUiState.Error(
                        e.message
                            ?: "Unknown error"
                    )
            }
        }
    }


    // =========================================================
    // SELECT DEVICE
    // =========================================================

    fun selectDevice(
        device: Device
    ) {

        playbackJob?.cancel()

        snapJob?.cancel()

        _isPlaying.value =
            false


        _selectedDevice.value =
            device


        _showDeviceDropdown.value =
            false


        _routePoints.value =
            emptyList()


        _snappedRoute.value =
            SnappedRoute.EMPTY


        _currentPlaybackIndex.value =
            0


        _totalDistance.value =
            0.0


        _averageSpeed.value =
            0.0


        _currentSpeed.value =
            0


        _showNoDataDialog.value =
            false


        println(
            "📱 [History] Device selected: " +
                    "${device.device_id} " +
                    "(${device.device_name}) " +
                    "date=${_startDate.value}"
        )
    }


    // =========================================================
    // DEVICE DROPDOWN
    // =========================================================

    fun toggleDeviceDropdown() {

        _showDeviceDropdown.value =
            !_showDeviceDropdown.value
    }


    // =========================================================
    // NO DATA DIALOG
    // =========================================================

    fun dismissNoDataDialog() {

        _showNoDataDialog.value =
            false
    }


    // =========================================================
    // SELECT DATE RANGE
    // =========================================================

    fun selectDateRange(
        start: String,
        end: String
    ) {

        playbackJob?.cancel()

        _isPlaying.value =
            false


        _startDate.value =
            start


        _endDate.value =
            end


        _currentPlaybackIndex.value =
            0


        _showNoDataDialog.value =
            false


        if (_selectedDevice.value != null) {

            fetchHistory()
        }
    }


    // =========================================================
    // PREVIOUS DATE
    // =========================================================

    fun navigateToPreviousDate() {

        val current =
            LocalDate.parse(
                _startDate.value
            )


        val previous =
            current.minus(
                1,
                DateTimeUnit.DAY
            )


        if (previous >= minSelectableDate) {

            val date =
                previous.toString()


            _startDate.value =
                date


            _endDate.value =
                date


            _currentPlaybackIndex.value =
                0


            _showNoDataDialog.value =
                false


            fetchHistory()
        }
    }


    // =========================================================
    // NEXT DATE
    // =========================================================

    fun navigateToNextDate() {

        val current =
            LocalDate.parse(
                _startDate.value
            )


        val next =
            current.plus(
                1,
                DateTimeUnit.DAY
            )


        if (next <= maxSelectableDate) {

            val date =
                next.toString()


            _startDate.value =
                date


            _endDate.value =
                date


            _currentPlaybackIndex.value =
                0


            _showNoDataDialog.value =
                false


            fetchHistory()
        }
    }


    // =========================================================
    // FETCH HISTORY
    // =========================================================

    fun fetchHistory() {

        val device =
            _selectedDevice.value
                ?: return


        historyJob?.cancel()


        historyJob =
            viewModelScope.launch {

                _uiState.value =
                    HistoryPlaybackUiState.Loading


                try {

                    val token =
                        storageManager.getString(
                            "token",
                            ""
                        ) ?: ""


                    val result =
                        repository.getRouteHistory(

                            token = token,

                            startDate =
                                _startDate.value,

                            endDate =
                                _endDate.value,

                            terminalId =
                                device.device_id
                        )


                    result.fold(

                        onSuccess = { response ->

                            println(
                                "📊 [History] " +
                                        "totalPoints=" +
                                        response.data.size
                            )


                            println(
                                "📊 [History] " +
                                        "count=" +
                                        response.count
                            )


                            println(
                                "📊 [History] " +
                                        "avgSpeed=" +
                                        response.averageSpeed
                            )


                            /*
                             * The vehicle type for the playback marker. Read from the
                             * response rather than the device row: this endpoint is
                             * the one that reports which vehicle the terminal was on
                             * for the range being played back.
                             */

                            _vehicleSlug.value =
                                response.vehicleIconSlug


                            /*
                             * Keep existing validity check.
                             *
                             * IMPORTANT:
                             * This only decides whether the point
                             * can be used by the playback screen.
                             *
                             * It does NOT calculate distance.
                             */

                            /*
                             * The endpoint does not return one vehicle's
                             * journey in travel order. It interleaves a
                             * stale "parked at base" fix into a live trace
                             * (a star radiating from the depot), and it
                             * weaves a second stream through the day — a
                             * backlog re-stamped with its upload time, or
                             * another terminal's fixes — which draws the
                             * route as a cobweb across the city.
                             *
                             * Both have to go before anything reads the
                             * points: no amount of road snapping repairs
                             * geometry that is not a journey. See
                             * dropStaleBeaconFixes() and
                             * keepTravelableFixes().
                             *
                             * Off the main thread: a day of 2,500 fixes
                             * measured ~200 ms of filtering, which is a
                             * dozen dropped frames if it runs where the
                             * map is being drawn.
                             */

                            /*
                             * collapseStationaryRuns last: a tracker reports every
                             * ~60 s whether the vehicle moved or not, so a day that
                             * begins with an overnight park begins with hundreds of
                             * fixes on one coordinate. Playback spends half a second
                             * on each, which is a marker standing still for a minute
                             * — reported, reasonably, as a Play button that does
                             * nothing. One fix per place is all the route and the
                             * marker can show anyway.
                             */

                            val validPoints =
                                withContext(Dispatchers.Default) {

                                    collapseStationaryRuns(
                                        keepTravelableFixes(
                                            dropStaleBeaconFixes(
                                                response.data.filter {
                                                    it.isValid()
                                                }
                                            )
                                        )
                                    )
                                }


                            println(
                                "📊 [History] " +
                                        "validPoints=" +
                                        validPoints.size +
                                        " (filtered from " +
                                        response.data.size +
                                        ")"
                            )


                            response.data
                                .take(3)
                                .forEachIndexed { i, p ->

                                    println(
                                        "  point[$i] " +
                                                "lat=${p.latitude} " +
                                                "lng=${p.longitude} " +
                                                "speed=${p.speed} " +
                                                "movement=${p.movement} " +
                                                "valid=${p.isValid()}"
                                    )
                                }


                            if (
                                validPoints.isEmpty()
                            ) {

                                println(
                                    "⚠️ [History] " +
                                            "No valid points"
                                )


                                _showNoDataDialog.value =
                                    true


                                resetStats()


                                _uiState.value =
                                    HistoryPlaybackUiState.Idle

                            } else {

                                /*
                                 * VERY IMPORTANT:
                                 *
                                 * Do NOT remove GPS points
                                 * because of distance validation.
                                 *
                                 * Playback needs the complete
                                 * route data.
                                 */

                                /*
                                 * A day the vehicle never left its spot: every fix
                                 * carries the same coordinate, so playback runs but
                                 * the marker has nowhere to travel to. Logged because
                                 * that is indistinguishable from a broken Play button
                                 * on screen, and it is the first thing to rule out.
                                 */

                                val distinctSpots =
                                    validPoints
                                        .distinctBy {
                                            it.latitude to it.longitude
                                        }
                                        .size


                                if (distinctSpots <= 1) {

                                    println(
                                        "⚠️ [History] all " +
                                                validPoints.size +
                                                " fixes are at one coordinate — " +
                                                "playback has nothing to move along"
                                    )
                                }


                                _routePoints.value =
                                    validPoints


                                /*
                                 * Lay the trace onto the road network so the
                                 * drawn route follows the streets.
                                 */

                                snapRouteToRoads(
                                    validPoints
                                )


                                /*
                                 * Calculate distance separately.
                                 *
                                 * GPS jumps are rejected inside
                                 * calculateTotalDistance().
                                 */

                                calculateTotalDistance(
                                    validPoints
                                )


                                /*
                                 * Average speed comes from backend.
                                 */

                                _averageSpeed.value =
                                    response.averageSpeed
                                        ?: 0.0


                                /*
                                 * Current speed starts from
                                 * first route point.
                                 */

                                _currentSpeed.value =
                                    validPoints
                                        .firstOrNull()
                                        ?.getSpeedInt()
                                        ?: 0


                                println(
                                    "✅ [History] " +
                                            "Route loaded: " +
                                            "${validPoints.size} points, " +
                                            "dist=${_totalDistance.value}, " +
                                            "avgSpeed=${_averageSpeed.value}"
                                )


                                _uiState.value =
                                    HistoryPlaybackUiState.Success
                            }
                        },


                        onFailure = { err ->

                            println(
                                "❌ [History] " +
                                        "fetchHistory failed: " +
                                        err.message
                            )


                            resetStats()


                            _uiState.value =
                                HistoryPlaybackUiState.Error(
                                    "Failed to fetch history"
                                )
                        }
                    )


                } catch (e: Exception) {

                    println(
                        "💥 [History] Exception: " +
                                e.message
                    )


                    resetStats()


                    _uiState.value =
                        HistoryPlaybackUiState.Error(
                            e.message
                                ?: "Unknown error"
                        )
                }
            }
    }


    // =========================================================
    // SNAP ROUTE TO ROADS
    // =========================================================
    //
    // The tracker reports a position every few seconds, so joining those fixes with
    // straight lines draws a route that cuts across blocks and buildings. The Roads
    // API returns the road geometry between consecutive fixes instead, which is what
    // makes the drawn route follow the road.
    //
    // The straight trace is published first so the route is on screen without waiting
    // for the network, and is replaced in place once the snapped one arrives. A
    // failure leaves the straight trace standing — playback never depends on this.
    // =========================================================

    private fun snapRouteToRoads(
        points: List<RoutePoint>
    ) {

        snapJob?.cancel()


        val geoPoints =
            points.mapNotNull { point ->

                val lat =
                    point.getLatDouble()
                        ?: return@mapNotNull null

                val lng =
                    point.getLngDouble()
                        ?: return@mapNotNull null

                GeoPoint(lat, lng)
            }


        /*
         * Every point here already passed isValid(), so a short list means
         * there is no route to snap rather than bad data.
         */

        if (geoPoints.size != points.size ||
            geoPoints.size < 2
        ) {

            _snappedRoute.value =
                SnappedRoute.straight(geoPoints)

            return
        }


        _snappedRoute.value =
            SnappedRoute.straight(geoPoints)


        snapJob =
            viewModelScope.launch {

                /*
                 * A newer fetch may have replaced the route while a request
                 * was in flight, so every publish checks it is still current.
                 */

                val publish = { route: SnappedRoute ->

                    if (_routePoints.value === points) {

                        _snappedRoute.value =
                            route
                    }
                }


                /*
                 * Snapping lands first and goes straight on the map; routing
                 * the sparse stretches that snapping leaves straight takes a
                 * few more requests, and the map should not wait for them to
                 * stop showing the raw trace.
                 */

                val snapped =
                    roadSnapService.snapToRoads(
                        points = geoPoints,
                        onPartial = publish,
                    )


                publish(snapped)
            }
    }


    // =========================================================
    // RESET
    // =========================================================

    private fun resetStats() {

        snapJob?.cancel()


        _snappedRoute.value =
            SnappedRoute.EMPTY


        _routePoints.value =
            emptyList()


        _totalDistance.value =
            0.0


        _averageSpeed.value =
            0.0


        _currentSpeed.value =
            0


        _currentPlaybackIndex.value =
            0


        _isPlaying.value =
            false
    }


    // =========================================================
    // CALCULATE TOTAL DISTANCE
    // =========================================================
    //
    // FIX:
    // Previously every consecutive GPS point was added blindly.
    //
    // That caused GPS jumps to become real distance.
    //
    // Example:
    // 13 km in 1 second
    // => 46,800 km/h
    // => impossible GPS jump
    //
    // Such segments are ignored.
    //
    // IMPORTANT:
    // We do NOT remove those points from routePoints.
    // They are ignored ONLY for distance calculation.
    // =========================================================


    private fun calculateTotalDistance(points: List<RoutePoint>) {
        if (points.size < 2) {
            _totalDistance.value = 0.0
            return
        }

        // Sort a copy; do not change playback route ordering.
        val sortedPoints = points
            .mapNotNull { point ->
                val timestamp = parseDateTimeToMillisOrNull(point.createdAt)
                    ?: return@mapNotNull null
                point to timestamp
            }
            .sortedBy { it.second }

        if (sortedPoints.size < 2) {
            _totalDistance.value = 0.0
            println("[Distance] Not enough points with valid timestamps")
            return
        }

        var totalKm = 0.0
        var accepted = 0
        var rejected = 0
        var ignoredSmall = 0

        for (i in 0 until sortedPoints.lastIndex) {
            val (first, time1) = sortedPoints[i]
            val (second, time2) = sortedPoints[i + 1]

            val lat1 = first.getLatDouble()
            val lng1 = first.getLngDouble()
            val lat2 = second.getLatDouble()
            val lng2 = second.getLngDouble()

            if (lat1 == null || lng1 == null ||
                lat2 == null || lng2 == null
            ) {
                rejected++
                continue
            }

            if (!lat1.isFinite() || !lng1.isFinite() ||
                !lat2.isFinite() || !lng2.isFinite() ||
                lat1 !in -90.0..90.0 ||
                lat2 !in -90.0..90.0 ||
                lng1 !in -180.0..180.0 ||
                lng2 !in -180.0..180.0
            ) {
                rejected++
                continue
            }

            val timeSeconds = (time2 - time1) / 1000.0

            if (timeSeconds <= 0.0 ||
                timeSeconds > MAX_DISTANCE_GAP_SECONDS
            ) {
                rejected++
                continue
            }

            val distanceKm = haversineDistance(
                lat1, lng1, lat2, lng2
            )

            if (!distanceKm.isFinite() || distanceKm < 0.0) {
                rejected++
                continue
            }

            val speedKmh = distanceKm * 3600.0 / timeSeconds

            if (!speedKmh.isFinite() ||
                speedKmh > MAX_VALID_GPS_SPEED_KMH
            ) {
                println(
                    "[Distance] GPS jump rejected: " +
                            "distance=$distanceKm km, speed=$speedKmh km/h"
                )
                rejected++
                continue
            }

            if (distanceKm < MIN_DISTANCE_KM) {
                ignoredSmall++
                continue
            }

            totalKm += distanceKm
            accepted++
        }

        _totalDistance.value = totalKm

        println(
            "[Distance] Total=${totalKm}km, " +
                    "accepted=$accepted, rejected=$rejected, " +
                    "small=$ignoredSmall"
        )
    }

    private fun parseDateTimeToMillisOrNull(value: String?): Long? {
        if (value.isNullOrBlank()) return null

        val normalized = value.trim().replace(' ', 'T')

        // First handle ISO timestamps containing Z or UTC offsets.
        try {
            return Instant.parse(normalized).toEpochMilliseconds()
        } catch (_: Exception) {
            // Fall through to timezone-less parsing.
        }

        return try {
            LocalDateTime.parse(normalized)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds()
        } catch (_: Exception) {
            println("[Distance] Invalid timestamp: $value")
            null
        }
    }

    private fun haversineDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val earthRadiusKm = 6371.0088

        val lat1Rad = lat1 * PI / 180.0
        val lat2Rad = lat2 * PI / 180.0
        val deltaLat = (lat2 - lat1) * PI / 180.0
        val deltaLng = (lng2 - lng1) * PI / 180.0

        val sinLat = sin(deltaLat / 2.0)
        val sinLng = sin(deltaLng / 2.0)

        val a = (
                sinLat * sinLat +
                        cos(lat1Rad) * cos(lat2Rad) *
                        sinLng * sinLng
                ).coerceIn(0.0, 1.0)

        val centralAngle = 2.0 * atan2(
            sqrt(a),
            sqrt(1.0 - a)
        )

        return earthRadiusKm * centralAngle
    }


    // =========================================================
    // PLAYBACK
    // =========================================================

    fun togglePlayback() {

        if (_routePoints.value.isEmpty()) {

            println(
                "⚠️ [History] " +
                        "No route points available"
            )

            return
        }


        if (_isPlaying.value) {

            pausePlayback()

        } else {

            startPlayback()
        }
    }


    // =========================================================
    // START PLAYBACK
    // =========================================================

    private fun startPlayback() {

        if (
            _routePoints.value.isEmpty()
        ) {

            return
        }


        playbackJob?.cancel()


        /*
         * Play at the end of the route means replay it.
         *
         * Without this the loop below has nothing left to advance: the condition is
         * false on the first check, so isPlaying flipped to true and straight back to
         * false and the marker never moved. A short route makes that the normal case
         * rather than an edge one — four fixes is 1.5 s at PLAYBACK_STEP_MS, so the
         * first press finishes before the user has looked up, and every press after
         * that did nothing at all.
         */

        if (
            _currentPlaybackIndex.value >=
            _routePoints.value.size - 1
        ) {

            _currentPlaybackIndex.value =
                0


            _currentSpeed.value =
                _routePoints.value
                    .firstOrNull()
                    ?.getSpeedInt()
                    ?: 0
        }


        _isPlaying.value =
            true


        println(
            "▶️ [History] playback from " +
                    _currentPlaybackIndex.value +
                    " of " +
                    _routePoints.value.size +
                    " points"
        )


        playbackJob =
            viewModelScope.launch {

                while (
                    _isPlaying.value &&
                    _currentPlaybackIndex.value <
                    _routePoints.value.size - 1
                ) {

                    /*
                     * Paced by the leg the marker is travelling right now, not by a
                     * flat rate — see playbackStepMillis.
                     *
                     * The index it is asked about is the *current* one, deliberately.
                     * The marker glides into point N while this loop is waiting to
                     * advance to N+1, so the wait that has to match that glide is the
                     * duration of the leg ending at N. Asking for N+1 here would draw
                     * every leg at the next leg's pace and leave the marker
                     * perpetually mid-glide when it arrived.
                     */

                    delay(
                        playbackStepMillis(
                            _routePoints.value,
                            _currentPlaybackIndex.value
                        )
                    )


                    _currentPlaybackIndex.value += 1


                    _currentSpeed.value =
                        _routePoints.value
                            .getOrNull(
                                _currentPlaybackIndex.value
                            )
                            ?.getSpeedInt()
                            ?: 0
                }


                _isPlaying.value =
                    false


                println(
                    "⏹️ [History] playback reached " +
                            _currentPlaybackIndex.value
                )
            }
    }


    // =========================================================
    // PAUSE PLAYBACK
    // =========================================================

    private fun pausePlayback() {

        _isPlaying.value =
            false


        playbackJob?.cancel()
    }


    // =========================================================
    // CONSTANTS
    // =========================================================

   /* companion object {

        *//**
         * How long playback dwells on each route point.
         *
         * The map's marker glides across one step in exactly this long
         * (`rememberSmoothPlaybackPosition`), so the two must agree: a glide shorter
         * than the step makes the marker stop and wait between points, and a longer
         * one makes it fall behind the index it is supposed to be showing.
         *//*
        const val PLAYBACK_STEP_MS = 500L


        *//*
         * Maximum physically reasonable speed
         * accepted for a GPS segment.
         *
         * GPS jumps above this are ignored ONLY
         * for distance calculation.
         *
         * Playback point itself is NOT deleted.
         *//*

        private const val
                MAX_VALID_GPS_SPEED_KMH =
            180.0


        *//*
         * Ignore GPS movement smaller than 3 meters.
         *
         * 0.003 km = 3 meters
         *//*

        private const val
                MIN_DISTANCE_KM =
            0.003
    }*/

    companion object {

        /**
         * Playback is no longer paced by a constant: each leg takes the real time it
         * took the vehicle, compressed — see `playbackStepMillis`, which both this
         * loop and `rememberSmoothPlaybackPosition` call so the glide and the index
         * cannot drift apart. `DEFAULT_STEP_MS` is what the old constant became, and
         * is used only where the timestamps cannot answer.
         */
        const val PLAYBACK_STEP_MS = DEFAULT_STEP_MS

        /**
         * Maximum accepted GPS segment speed.
         * Segments exceeding this speed are ignored
         * for distance calculation only.
         * Playback points are not deleted.
         */
        private const val MAX_VALID_GPS_SPEED_KMH = 180.0

        /**
         * Ignore GPS movements smaller than 3 metres
         * to reduce small GPS-position fluctuations.
         */
        private const val MIN_DISTANCE_KM = 0.003

        /**
         * Do not calculate distance across timestamp gaps
         * greater than 3 minutes (180 seconds).
         */
        private const val MAX_DISTANCE_GAP_SECONDS = 180.0
    }


    // =========================================================
    // CLEAR
    // =========================================================

    override fun onCleared() {

        playbackJob?.cancel()

        historyJob?.cancel()

        snapJob?.cancel()

        super.onCleared()
    }
}

