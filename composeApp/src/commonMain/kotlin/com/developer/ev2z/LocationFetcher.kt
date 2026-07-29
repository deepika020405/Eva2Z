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
 * Asks for location once when the app opens, the way most apps do. Android already
 * asks from its startup permission chain, so only iOS does work here.
 */
expect fun requestLocationPermissionAtStartup()

/**
 * Fires whenever the user answers a location-permission prompt anywhere in the app,
 * so screens waiting on a fix can retry without polling.
 */
object LocationPermissionEvents {
    private val _changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changed: SharedFlow<Unit> = _changed.asSharedFlow()

    fun notifyChanged() {
        _changed.tryEmit(Unit)
    }
}