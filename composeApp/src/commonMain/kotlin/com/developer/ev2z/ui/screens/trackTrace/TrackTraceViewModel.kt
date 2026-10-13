package com.developer.ev2z.ui.screens.trackTrace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.developer.ev2z.data.local.remote.LiveLocationResponse
import com.developer.ev2z.network.DEVICES_POLL_INTERVAL_MS
import com.developer.ev2z.network.UnauthorizedException
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.RoadSnapService
import com.developer.ev2z.network.approxDistanceMeters
import com.developer.ev2z.network.pollAtFixedRate
import com.developer.ev2z.repository.TrackTraceRepository
import com.developer.ev2z.response.Device
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Simple lat/lng data class for trail points
data class TrailPoint(val latitude: Double, val longitude: Double)

@OptIn(kotlin.time.ExperimentalTime::class)
class TrackTraceViewModel(
    private val repository: TrackTraceRepository,
    private val tokenProvider: () -> String
) : ViewModel() {

    private var liveLocationJob: Job? = null
    private var devicesPollingJob: Job? = null

    /** Coordinates + timestamp of the last fix seen, so a repeat can be named as one. */
    private var lastFixSignature: String? = null

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<Device?>(null)
    val selectedDevice = _selectedDevice.asStateFlow()

    private val _liveLocation = MutableStateFlow<LiveLocationResponse?>(null)
    val liveLocation = _liveLocation.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _unauthorized = MutableStateFlow(false)
    val unauthorized = _unauthorized.asStateFlow()

    private val _trailPoints = MutableStateFlow<List<TrailPoint>>(emptyList())
    val trailPoints = _trailPoints.asStateFlow()

    // =========================================================
    // THE ROAD THE MARKER IS DRIVING ALONG
    // =========================================================
    //
    // The leg between the last two fixes, as road geometry. A tracker reports every
    // few tens of seconds, so the straight line between two fixes is hundreds of
    // metres of chord — and a chord cuts across the blocks, the central reservation
    // and the island of every roundabout on it. The map walks this instead, so the
    // vehicle follows the street.
    //
    // Published twice per fix on purpose: the straight chord the instant the fix
    // lands, so the marker starts moving, then the snapped road a moment later. The
    // walk resumes from where the marker already is, so the swap is not a jump.
    // =========================================================

    private val roadSnapper = RoadSnapService()

    private var roadSnapJob: Job? = null

    /** The fix the current leg starts from, and the one it ends at. */
    private var legStart: GeoPoint? = null

    private val _roadLeg = MutableStateFlow<List<GeoPoint>>(emptyList())
    val roadLeg = _roadLeg.asStateFlow()

    fun start() {
        // First-time load shows a spinner; refreshes are silent.
        if (_devices.value.isEmpty()) {
            _isLoading.value = true
        }
        startDevicesPolling()
    }

    fun stop() {
        liveLocationJob?.cancel()
        devicesPollingJob?.cancel()
        liveLocationJob = null
        devicesPollingJob = null
    }

    fun selectDevice(device: Device) {
        if (_selectedDevice.value?.device_id == device.device_id) return
        _selectedDevice.value = device
        _liveLocation.value = null
        _trailPoints.value = emptyList()
        roadSnapJob?.cancel()
        roadSnapJob = null
        legStart = null
        _roadLeg.value = emptyList()
        lastFixSignature = null
        startLiveLocationPolling(device.device_id)
    }

    private suspend fun loadHistoricalTrail(deviceId: String) {
        try {
            val today = Instant.fromEpochMilliseconds(getTimeMillis())
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toString()
            val token = tokenProvider()
            println("📍 [TrackTrace] Loading today's trail for $deviceId date=$today")
            val result = repository.getTodayRouteHistory(token, deviceId, today)
            if (result.success) {
                val points = result.data
                    .filter { it.isValid() }
                    .mapNotNull { p ->
                        val lat = p.getLatDouble() ?: return@mapNotNull null
                        val lng = p.getLngDouble() ?: return@mapNotNull null
                        TrailPoint(lat, lng)
                    }
                println("📍 [TrackTrace] Historical trail: ${points.size} valid points")
                if (points.isNotEmpty()) {
                    _trailPoints.value = points.takeLast(100)
                }
            }
        } catch (e: Exception) {
            println("💥 [TrackTrace] History trail load failed: ${e.message}")
        }
    }

    private fun startDevicesPolling() {
        devicesPollingJob?.cancel()
        devicesPollingJob = viewModelScope.launch {
            pollAtFixedRate(DEVICES_POLL_INTERVAL_MS) { refreshDevices() }
        }
    }

    private suspend fun refreshDevices() {
        try {
            val token = tokenProvider()
            println("🔑 [TrackTrace] Token used: ${if (token.isBlank()) "EMPTY ❌" else token.take(20) + "..."}")
            val res = repository.getMyDevices(token)
            println("📦 [TrackTrace] API success=${res.success}, devices=${res.devices.size}, username=${res.username}")
            res.devices.forEachIndexed { i, d ->
                println("  [$i] device_id=${d.device_id} name=${d.device_name} status=${d.engine_status}")
            }
            if (!res.success) {
                println("❌ [TrackTrace] API returned success=false")
                if (_devices.value.isEmpty()) {
                    _error.value = "Failed to load devices"
                }
                return
            }

            _username.value = res.username.orEmpty()
            _devices.value = res.devices
            _error.value = null

            val current = _selectedDevice.value
            if (current == null) {
                // First load — auto-select first device, load history, and start live polling.
                res.devices.firstOrNull()?.let { first ->
                    _selectedDevice.value = first
                    startLiveLocationPolling(first.device_id)
                }
            } else {
                // Refresh — preserve selection; update the Device object so
                // engine_status stays current for the selected row.
                val updated = res.devices.firstOrNull { it.device_id == current.device_id }
                if (updated != null) {
                    _selectedDevice.value = updated
                }
            }
        } catch (e: UnauthorizedException) {
            println("🔒 [TrackTrace] 401 Unauthorized — stopping polling")
            stop()
            _unauthorized.value = true
        } catch (e: Exception) {
            println("💥 [TrackTrace] Exception in refreshDevices: ${e::class.simpleName} — ${e.message}")
            if (_devices.value.isEmpty()) {
                _error.value = e.message ?: "Failed to load devices"
            }
        } finally {
            _isLoading.value = false
        }
    }

    private fun startLiveLocationPolling(deviceId: String) {
        liveLocationJob?.cancel()
        liveLocationJob = viewModelScope.launch {
            // Fixed rate, not a fixed gap: the marker animation is paced to the poll
            // interval, so request latency added on top of the delay would leave the
            // marker frozen for the overshoot at the end of every cycle.
            pollAtFixedRate { fetchLiveLocation(deviceId) }
        }
    }

    private suspend fun fetchLiveLocation(deviceId: String) {
        try {
            println("📍 [TrackTrace] Fetching live location for deviceId=$deviceId")
            val res = repository.getLiveLocation(tokenProvider(), deviceId)
            println("📍 [TrackTrace] LiveLocation success=${res.success} data=${res.data}")
            if (!res.success) {
                println("❌ [TrackTrace] LiveLocation returned success=false")
                return
            }

            _liveLocation.value = res

            logFixFreshness(res.data?.latestLocation)

            // validPosition() drops the "0"/"0" a tracker reports while it has no
            // satellite fix; appending that to the trail drew a line out to the
            // Atlantic and back.
            val position = res.data?.latestLocation?.validPosition()

            if (position != null) {
                val (lat, lng) = position
                val newPoint = TrailPoint(lat, lng)
                val current = _trailPoints.value
                if (current.lastOrNull() != newPoint) {
                    _trailPoints.value = (current + newPoint).takeLast(100)
                }
                onFixForRoadLeg(GeoPoint(lat, lng))
            }
        } catch (e: UnauthorizedException) {
            println("🔒 [TrackTrace] 401 Unauthorized (live location) — stopping polling")
            stop()
            _unauthorized.value = true
        } catch (e: Exception) {
            println("💥 [TrackTrace] Exception in fetchLiveLocation: ${e::class.simpleName} — ${e.message}")
        }
    }

    /**
     * Starts a new leg at [fix], straight away and then snapped to the road.
     *
     * Several polls in a row routinely bring the identical fix — the tracker uploads on
     * its own schedule, not the app's — and re-publishing the same leg for each of them
     * would restart the walk and leave the marker stuttering on the spot. Only a fix
     * that has actually moved starts a leg.
     */
    private fun onFixForRoadLeg(fix: GeoPoint) {
        val from = legStart
        legStart = fix

        if (from == null || approxDistanceMeters(from, fix) < MIN_LEG_METERS) {
            // Nothing to drive along: the first fix of the session, or a vehicle that
            // has not moved. The map parks the marker on the single point.
            _roadLeg.value = listOf(fix)
            return
        }

        // The chord first, so the marker is moving within the frame — snapping is a
        // network round trip and the vehicle should not stand still waiting for it.
        _roadLeg.value = listOf(from, fix)

        roadSnapJob?.cancel()
        roadSnapJob = viewModelScope.launch {
            val snapped = roadSnapper.snapToRoads(listOf(from, fix))
            // Only if this is still the leg being driven: a fix that landed while the
            // request was in flight has already moved the marker on, and replacing its
            // leg with the previous one's road would drag the vehicle backwards.
            if (legStart == fix && snapped.path.size >= 2) {
                _roadLeg.value = snapped.path
            }
        }
    }

    /**
     * Says whether each poll actually brought a new fix, which is the one thing that
     * explains a marker standing still.
     *
     * Polling rate and *reporting* rate are separate: the app asks every
     * [LIVE_LOCATION_POLL_INTERVAL_MS], but the tracker only uploads on its own
     * schedule, so several polls in a row can hand back the identical fix. The marker
     * is then correct to stay put — there is nowhere new to go — and polling faster
     * cannot help, because the gap is upstream of the app. REPEAT lines with a rising
     * `age` are that case; a REPEAT with `speed` well above zero is a tracker sending
     * stale coordinates, which is a device-side fault.
     */
    private fun logFixFreshness(fix: com.developer.ev2z.data.local.remote.LatestLocation?) {
        if (fix == null) {
            println("📍 [TrackTrace] fix=NONE — response carried no latest_location")
            return
        }
        val signature = "${fix.latitude},${fix.longitude}@${fix.timestamp}"
        val repeated = signature == lastFixSignature
        lastFixSignature = signature
        println(
            "📍 [TrackTrace] fix=${if (repeated) "REPEAT" else "NEW"} " +
                    "pos=${fix.latitude},${fix.longitude} speed=${fix.speed} " +
                    "status=${fix.status} ts=${fix.timestamp} age=${fix.lastSeenMinutesAgo}min"
        )
    }

    /**
     * The authoritative status for the selected device:
     * prefers the live location's `status` field (real-time) and
     * falls back to the device list's `engine_status` snapshot.
     */
    fun effectiveSelectedStatus(): String {
        val live = _liveLocation.value?.data?.latestLocation?.status
        if (!live.isNullOrBlank()) return live
        return _selectedDevice.value?.engine_status?.ifBlank { "offline" } ?: "offline"
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private companion object {
        /**
         * How far a fix has to be from the one before it to count as a leg to drive.
         *
         * Five metres: below that it is the receiver wandering while the vehicle stands
         * still, and snapping that to a road would ask the Roads API which street a
         * parked car is creeping along — once per poll, all day.
         */
        const val MIN_LEG_METERS = 5.0
    }
}