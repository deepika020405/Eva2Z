package com.developer.ev2z.util

import com.developer.ev2z.data.local.createStorageManager
import com.developer.ev2z.data.local.model.DeviceResponse
import com.developer.ev2z.network.ApiService

/**
 * The one status notifier the whole app shares, foreground and background alike.
 *
 * A second instance would be a second memory: the screen poll and the background check
 * both have to agree on what each vehicle was last seen doing, or the same change gets a
 * banner from each of them. The statuses live in storage ([StorageStatusSpellStore])
 * rather than in this object, so that agreement holds across process restarts too — the
 * background check runs in a process the user never opened.
 */
object VehicleStatusAlerts {

    private val notifier by lazy {
        VehicleStatusNotifier(store = StorageStatusSpellStore(createStorageManager()))
    }

    /**
     * Shared for the same reason the notifier is: its ignition cache is what keeps the
     * 5-second foreground poll inside the host's rate limit, and a per-caller instance
     * would be a per-caller cache.
     */
    private val idleResolver = IdleStatusResolver()

    /**
     * Hands one poll's devices to both notifiers.
     *
     * Engine On/Off goes out alongside Moving/Parked, off the same samples and on the
     * same tick — which is what makes the two families of alert behave identically. The
     * engine state rides on the samples; see [VehicleStatusSample.ignition].
     */
    fun onPoll(samples: List<VehicleStatusSample>) {
        notifier.onPoll(samples)
        IgnitionAlerts.onStates(samples)
    }

    /**
     * Reads the devices straight out of a `latest-locations` response, with idle derived
     * from the ignition — see [IdleStatusResolver] for why the response alone cannot say
     * it, and [com.developer.ev2z.network.ApiService.getDeviceIgnition] for the lookup
     * every caller passes in.
     *
     * [sessionKindOf] is the fallback for a tracker with no ignition line
     * ([com.developer.ev2z.network.ApiService.getDeviceLatestSessionStatus]); without it
     * those vehicles can never be called idle.
     */
    suspend fun onPoll(
        response: DeviceResponse,
        sessionKindOf: StatusSessionLookup? = null,
        ignitionOf: IgnitionLookup,
    ) = onPoll(idleResolver.resolve(response.data, ignitionOf, sessionKindOf))

    /** Forgets every vehicle's last status — called on logout. */
    fun reset() {
        notifier.reset()
        idleResolver.reset()
    }
}

/**
 * One status check, run from whatever background mechanism the platform gives us:
 * `StatusCheckWorker` on Android, the `BGAppRefreshTask` handler on iOS.
 *
 * Deliberately self-contained — it reads the token out of storage and builds its own
 * [ApiService] — because nothing else exists in that process. No ViewModel, no screen,
 * no repository graph: the app was not opened, the OS simply gave the app a few seconds.
 *
 * Returns whether the check actually got an answer out of the server, which is what both
 * platforms want to hear: `Result.success`/`retry` on Android, `setTaskCompleted(success:)`
 * on iOS, and the scheduler leans on it to decide how eagerly to run us again.
 */
object VehicleStatusBackgroundCheck {

    suspend fun run(): Boolean {
        val token = try {
            createStorageManager().getString("token", "")
        } catch (e: Exception) {
            // Storage not initialised in this process. Nothing to do, and nothing worth
            // crashing a background task over.
            println("⚠️ Status check skipped — storage unavailable: ${e.message}")
            return false
        }

        if (token.isBlank()) {
            println("ℹ️ Status check skipped — nobody is logged in on this device")
            return false
        }

        return try {
            val api = ApiService()
            val response = api.getDevicesLatestLocations(token)
            val today = todayInReportZone()

            VehicleStatusAlerts.onPoll(
                response,
                sessionKindOf = { api.getDeviceLatestSessionStatus(token, it, today) },
            ) { api.getDeviceIgnition(token, it) }

            // Engine on/off too, and `onRound` rather than `onFleetPoll`: this process
            // exists for a few seconds every fifteen minutes, so there is no faster tick
            // to throttle against — and skipping the round because the last one was
            // "recent" would mean never running it in the background at all.
            IgnitionAlerts.onRound(response.data) { api.getDeviceAlarms(token, it).data }

            println("✅ Status check ran over ${response.data.size} device(s)")
            true
        } catch (e: Exception) {
            // Includes the 401 case: a revoked session is not an error worth retrying
            // hard, and the next run will find the token gone once the app notices.
            println("❌ Status check failed: ${e.message}")
            false
        }
    }
}
