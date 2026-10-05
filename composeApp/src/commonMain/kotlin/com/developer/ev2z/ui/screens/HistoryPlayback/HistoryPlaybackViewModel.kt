


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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
                             * Keep existing validity check.
                             *
                             * IMPORTANT:
                             * This only decides whether the point
                             * can be used by the playback screen.
                             *
                             * It does NOT calculate distance.
                             */

                            /*
                             * The endpoint interleaves a stale "parked at
                             * base" fix into a live trace, which draws the
                             * route as a star radiating from the depot.
                             * Those have to go before anything reads the
                             * points — see dropStaleBeaconFixes().
                             */

                            val validPoints =
                                dropStaleBeaconFixes(
                                    response.data.filter {
                                        it.isValid()
                                    }
                                )


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

    private fun calculateTotalDistance(
        points: List<RoutePoint>
    ) {

        var totalDist =
            0.0


        if (points.size < 2) {

            _totalDistance.value =
                0.0

            return
        }


        for (
        i in 0 until points.size - 1
        ) {

            val p1 =
                points[i]


            val p2 =
                points[i + 1]


            val lat1 =
                p1.getLatDouble()
                    ?: continue


            val lng1 =
                p1.getLngDouble()
                    ?: continue


            val lat2 =
                p2.getLatDouble()
                    ?: continue


            val lng2 =
                p2.getLngDouble()
                    ?: continue


            // -------------------------------------------------
            // Validate coordinates
            // -------------------------------------------------

            if (
                lat1 !in -90.0..90.0 ||
                lat2 !in -90.0..90.0 ||
                lng1 !in -180.0..180.0 ||
                lng2 !in -180.0..180.0
            ) {
                continue
            }


            // -------------------------------------------------
            // Calculate GPS distance
            // -------------------------------------------------

            val segmentDistance =
                haversineDistance(
                    lat1,
                    lng1,
                    lat2,
                    lng2
                )


            // -------------------------------------------------
            // Get time difference
            // -------------------------------------------------

            val timeDiffSeconds =
                getTimeDifferenceSeconds(
                    p1.createdAt,
                    p2.createdAt
                )


            /*
             * If timestamp cannot be parsed,
             * do not add the segment.
             *
             * This prevents a bad GPS point from
             * creating fake distance.
             */

            if (timeDiffSeconds <= 0) {

                continue
            }


            // -------------------------------------------------
            // Calculate implied speed
            // -------------------------------------------------

            val calculatedSpeed =
                (
                        segmentDistance /
                                timeDiffSeconds
                        ) * 3600.0


            // -------------------------------------------------
            // Reject impossible GPS jumps
            // -------------------------------------------------

            if (
                calculatedSpeed > MAX_VALID_GPS_SPEED_KMH
            ) {

                println(
                    "⚠️ [Distance] GPS jump ignored: " +
                            "segment=${segmentDistance} km, " +
                            "time=${timeDiffSeconds}s, " +
                            "calculatedSpeed=${calculatedSpeed} km/h"
                )

                continue
            }


            // -------------------------------------------------
            // Ignore tiny GPS noise
            // -------------------------------------------------

            if (
                segmentDistance <
                MIN_DISTANCE_KM
            ) {

                continue
            }


            // -------------------------------------------------
            // Add valid segment
            // -------------------------------------------------

            totalDist +=
                segmentDistance
        }


        _totalDistance.value =
            totalDist


        println(
            "📏 [Distance] Final calculated distance = " +
                    "$totalDist km"
        )
    }


    // =========================================================
    // TIME DIFFERENCE
    // =========================================================

    private fun getTimeDifferenceSeconds(
        firstDate: String?,
        secondDate: String?
    ): Double {

        if (
            firstDate.isNullOrBlank() ||
            secondDate.isNullOrBlank()
        ) {

            return 0.0
        }


        return try {

            val firstMillis =
                parseDateTimeToMillis(
                    firstDate
                )


            val secondMillis =
                parseDateTimeToMillis(
                    secondDate
                )


            (
                    secondMillis -
                            firstMillis
                    ) / 1000.0


        } catch (e: Exception) {

            println(
                "⚠️ [Distance] " +
                        "Unable to parse timestamp: " +
                        "first=$firstDate, " +
                        "second=$secondDate, " +
                        "error=${e.message}"
            )


            0.0
        }
    }


    // =========================================================
    // PARSE API DATE/TIME
    // =========================================================
    //
    // Supports:
    //
    // 2026-10-01 04:24:04
    //
    // and:
    //
    // 2026-10-01T04:24:04
    //
    // Timezone is not important here because we only need
    // the difference between two timestamps.
    // =========================================================

    private fun parseDateTimeToMillis(
        value: String
    ): Long {

        val normalized =
            value
                .trim()
                .replace(
                    " ",
                    "T"
                )


        val localDateTime =
            LocalDateTime.parse(
                normalized
            )


        return localDateTime
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()
    }


    // =========================================================
    // HAVERSINE DISTANCE
    // =========================================================

    private fun haversineDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {

        val earthRadius =
            6371.0


        val dLat =
            (lat2 - lat1) *
                    PI /
                    180.0


        val dLng =
            (lng2 - lng1) *
                    PI /
                    180.0


        val lat1Rad =
            lat1 *
                    PI /
                    180.0


        val lat2Rad =
            lat2 *
                    PI /
                    180.0


        var a =
            sin(dLat / 2.0)
                .let {
                    it * it
                } +
                    cos(lat1Rad) *
                    cos(lat2Rad) *
                    sin(dLng / 2.0)
                        .let {
                            it * it
                        }


        /*
         * Floating point safety.
         *
         * a should always be between 0 and 1.
         */

        a =
            a.coerceIn(
                0.0,
                1.0
            )


        val c =
            2.0 *
                    atan2(
                        sqrt(a),
                        sqrt(1.0 - a)
                    )


        return earthRadius * c
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


        _isPlaying.value =
            true


        playbackJob =
            viewModelScope.launch {

                while (
                    _isPlaying.value &&
                    _currentPlaybackIndex.value <
                    _routePoints.value.size - 1
                ) {

                    delay(PLAYBACK_STEP_MS)


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

    companion object {

        /**
         * How long playback dwells on each route point.
         *
         * The map's marker glides across one step in exactly this long
         * (`rememberSmoothPlaybackPosition`), so the two must agree: a glide shorter
         * than the step makes the marker stop and wait between points, and a longer
         * one makes it fall behind the index it is supposed to be showing.
         */
        const val PLAYBACK_STEP_MS = 500L


        /*
         * Maximum physically reasonable speed
         * accepted for a GPS segment.
         *
         * GPS jumps above this are ignored ONLY
         * for distance calculation.
         *
         * Playback point itself is NOT deleted.
         */

        private const val
                MAX_VALID_GPS_SPEED_KMH =
            180.0


        /*
         * Ignore GPS movement smaller than 3 meters.
         *
         * 0.003 km = 3 meters
         */

        private const val
                MIN_DISTANCE_KM =
            0.003
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
