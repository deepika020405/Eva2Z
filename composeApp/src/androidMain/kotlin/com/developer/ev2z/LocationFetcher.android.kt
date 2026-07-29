package com.developer.ev2z

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

internal const val LOCATION_PERMISSION_REQUEST = 7001
internal var pendingLocationCallback: ((Double, Double) -> Unit)? = null

actual fun hasLocationPermission(): Boolean = try {
    ContextCompat.checkSelfPermission(
        appCtx, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(
        appCtx, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
} catch (_: Exception) {
    false
}

actual fun fetchPhoneLocation(onResult: (Double, Double) -> Unit) {
    try {
        val ctx = appCtx
        if (!hasLocationPermission()) {
            val activity = ActivityProvider.getActivity()
            if (activity != null) {
                pendingLocationCallback = onResult
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )
            } else {
                onResult(0.0, 0.0)
            }
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(ctx)
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onResult(location.latitude, location.longitude)
                } else {
                    val cts = CancellationTokenSource()
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { fresh ->
                            if (fresh != null) onResult(fresh.latitude, fresh.longitude)
                            else onResult(0.0, 0.0)
                        }
                        .addOnFailureListener { onResult(0.0, 0.0) }
                }
            }
            .addOnFailureListener { onResult(0.0, 0.0) }
    } catch (_: Exception) {
        onResult(0.0, 0.0)
    }
}

// No-op: MainActivity's NotificationPermissionHelper chain already asks on app open,
// and firing a second request while that one is in flight would be dropped by the OS.
actual fun requestLocationPermissionAtStartup() = Unit

actual fun isLocationEnabled(): Boolean = try {
    val lm = appCtx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    LocationManagerCompat.isLocationEnabled(lm)
} catch (_: Exception) {
    // Can't tell — assume on and let the fix attempt decide.
    true
}

actual fun openAppSettings() {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", appCtx.packageName, null)
        )
        val activity = ActivityProvider.getActivity()
        if (activity != null) {
            activity.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appCtx.startActivity(intent)
        }
    } catch (_: Exception) {
    }
}

actual fun openLocationSettings() {
    try {
        val activity = ActivityProvider.getActivity()
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        if (activity != null) {
            activity.startActivity(intent)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appCtx.startActivity(intent)
        }
    } catch (_: Exception) {
    }
}