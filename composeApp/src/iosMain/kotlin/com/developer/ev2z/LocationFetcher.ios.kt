@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.developer.ev2z

import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

// Strong references so the manager + delegate survive until a result is delivered.
private var locationManager: CLLocationManager? = null
private var locationDelegate: IosLocationDelegate? = null

// Every caller waiting on the request currently in flight. These are globals, so a second
// concurrent caller used to replace the first one's manager and delegate outright — the
// first then never heard back. Screens that both ask at app open (fuel prices and weather)
// now share one request and all get the same fix.
private val pendingWaiters = mutableListOf<(Double, Double) -> Unit>()

actual fun fetchPhoneLocation(onResult: (Double, Double) -> Unit) {
    try {
        pendingWaiters += onResult
        // A request is already in flight — its delegate will fan the result out to
        // everyone queued, so don't start a competing one.
        if (locationManager != null) return

        val manager = CLLocationManager()
        val delegate = IosLocationDelegate()

        locationManager = manager
        locationDelegate = delegate
        manager.delegate = delegate

        // Prompt the user for permission (no-op if already decided).
        manager.requestWhenInUseAuthorization()

        // If permission was already granted, fetch immediately;
        // otherwise the delegate fires once the user responds.
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
            else -> { /* wait for locationManagerDidChangeAuthorization */ }
        }
    } catch (_: Exception) {
        // Answer everyone queued, including this caller, then reset for the next attempt.
        val waiters = pendingWaiters.toList()
        pendingWaiters.clear()
        locationManager = null
        locationDelegate = null
        waiters.forEach { it(0.0, 0.0) }
    }
}

actual fun hasLocationPermission(): Boolean = try {
    when (CLLocationManager().authorizationStatus) {
        kCLAuthorizationStatusAuthorizedWhenInUse,
        kCLAuthorizationStatusAuthorizedAlways -> true
        else -> false
    }
} catch (_: Exception) {
    false
}

// CoreLocation shows its prompt as a side effect of asking for a fix. The delegate
// reports the answer through LocationPermissionEvents, so callers just fire and forget.
// iOS gives apps no way to switch Location Services on — only Settings can. Report the
// current state so the caller falls back to its "Open Settings" prompt.
actual fun promptEnableLocationServices(onResult: (Boolean) -> Unit) {
    onResult(isLocationEnabled())
}

actual fun requestLocationPermissionAtStartup() {
    if (hasLocationPermission()) return
    fetchPhoneLocation { _, _ -> }
}

actual fun isLocationEnabled(): Boolean = try {
    CLLocationManager.locationServicesEnabled()
} catch (_: Exception) {
    true
}

actual fun openLocationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
    val app = UIApplication.sharedApplication
    if (app.canOpenURL(url)) {
        app.openURL(url, options = mapOf<Any?, Any?>(), completionHandler = null)
    }
}

// iOS has no per-app location toggle outside the app's own settings page.
actual fun openAppSettings() = openLocationSettings()

private class IosLocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {

    private var delivered = false

    private fun deliver(lat: Double, lng: Double) {
        if (delivered) return
        delivered = true
        val waiters = pendingWaiters.toList()
        pendingWaiters.clear()
        // Release retained references before calling back, so a waiter that immediately
        // asks for another fix starts a fresh request instead of queueing behind this one.
        locationManager = null
        locationDelegate = null
        waiters.forEach { it(lat, lng) }
    }

    // iOS 14+ authorization callback.
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        LocationPermissionEvents.notifyChanged()
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> manager.requestLocation()
            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> deliver(0.0, 0.0)
            else -> { /* notDetermined — keep waiting for the user's choice */ }
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val loc = didUpdateLocations.lastOrNull() as? CLLocation
        if (loc != null) {
            val lat = loc.coordinate.useContents { latitude }
            val lng = loc.coordinate.useContents { longitude }
            deliver(lat, lng)
        } else {
            deliver(0.0, 0.0)
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        deliver(0.0, 0.0)
    }
}