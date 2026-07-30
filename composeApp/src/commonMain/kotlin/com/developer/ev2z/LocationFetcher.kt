package com.developer.ev2z

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

expect fun fetchPhoneLocation(onResult: (lat: Double, lng: Double) -> Unit)

/** Opens the device's location/app settings so the user can turn location services on. */
expect fun openLocationSettings()

/** Opens this app's settings page, where a denied permission can be re-granted. */
expect fun openAppSettings()

/**
 * Whether location access is already granted. Never shows a prompt — use it to decide
 * *whether* to ask, then call [fetchPhoneLocation] to actually ask.
 */
expect fun hasLocationPermission(): Boolean

/** Whether the phone's location services (GPS) are switched on at all. */
expect fun isLocationEnabled(): Boolean

/**
 * Asks the system to switch location services on, showing the in-app "Turn on location"
 * dialog other apps use — the user stays in the app and never visits Settings.
 *
 * [onResult] reports whether location ended up enabled. On iOS there is no API to toggle
 * Location Services, so it reports the current state and callers fall back to sending the
 * user to Settings.
 */
expect fun promptEnableLocationServices(onResult: (enabled: Boolean) -> Unit)

/**
 * Asks for location once when the app opens, the way most apps do. Android already
 * asks from its startup permission chain, so only iOS does work here.
 */
expect fun requestLocationPermissionAtStartup()

/**
 * Fires whenever the user answers a location-permission prompt anywhere in the app,
 * so screens waiting on a fix can retry without polling.
 */
object LocationPermissionEvents {
    // replay = 1 so a screen created just after the user answered the startup prompt
    // still sees the answer instead of waiting for a grant that already happened.
    private val _changed = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val changed: SharedFlow<Unit> = _changed.asSharedFlow()

    fun notifyChanged() {
        _changed.tryEmit(Unit)
    }
}