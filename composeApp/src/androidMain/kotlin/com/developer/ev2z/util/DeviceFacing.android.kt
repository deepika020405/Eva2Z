package com.developer.ev2z.util

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Looper
import android.view.Surface
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.developer.ev2z.network.GeoPoint
import com.developer.ev2z.network.approxDistanceMeters

@SuppressLint("MissingPermission")
@Composable
actual fun rememberDeviceFacing(): DeviceFacing? {
    val context = LocalContext.current

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    var position by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Degrees from true north, eased. NaN until the first reading — a cone pointing
    // north because nothing has been measured yet is worse than no cone.
    var heading by remember { mutableStateOf(Double.NaN) }

    // Nothing is asked for without permission: this is a nicety on the map, not a
    // reason to put a permission dialog in front of someone opening a screen.
    if (!hasLocationPermission) return null

    DisposableEffect(Unit) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                position = location.latitude to location.longitude
            }
        }

        // Balanced, not high accuracy: this draws a cone a few metres across on a map,
        // and asking for GPS-grade fixes for it would run the receiver flat.
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) position = location.latitude to location.longitude
        }
        client.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 4_000L)
                .setMinUpdateDistanceMeters(5f)
                .build(),
            callback,
            Looper.getMainLooper()
        )

        onDispose { client.removeLocationUpdates(callback) }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        // TYPE_ROTATION_VECTOR, not the deprecated TYPE_ORIENTATION and not
        // accelerometer + magnetometer by hand: the platform already fuses those three
        // and hands back something stable enough to draw.
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor == null) {
            // No compass on this device. Position still updates above; the cone is
            // simply never drawn.
            return@DisposableEffect onDispose { }
        }

        val rotationMatrix = FloatArray(9)
        val remapped = FloatArray(9)
        val orientation = FloatArray(3)

        // The declination currently in use, and where it was worked out.
        //
        // Held across events on purpose. Magnetic declination is a property of *where*
        // the phone is, not of which way it is pointing — it shifts about a degree per
        // hundred kilometres and a fraction of a degree per year — so recomputing it on
        // every sensor reading is pure waste. And it is not cheap waste: each
        // `GeomagneticField` builds a `LegendreTable` of nested float arrays, and at
        // SENSOR_DELAY_UI that ran sixteen times a second for as long as a map screen
        // was open.
        //
        // That is not theoretical. An ANR trace on 2026-10-13 caught the main thread in
        // `WaitingForGcToComplete`, inside `LegendreTable.<init>`, inside this very
        // listener — the app had allocated faster than the collector could keep up and
        // stopped answering taps.
        var declination = 0.0
        var declinationFrom: Pair<Double, Double>? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                // The sensor reports in the device's *natural* orientation, so a phone
                // turned on its side reads ninety degrees out until the axes are
                // remapped to however the screen is currently rotated. This is the
                // whole reason a held-sideways phone used to point the wrong way.
                val (axisX, axisY) = screenAxes(context.displayRotation())
                SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)
                SensorManager.getOrientation(remapped, orientation)

                val magnetic = Math.toDegrees(orientation[0].toDouble())

                // Magnetic north is not true north, and the map's north is true north.
                // The offset is about a degree in Delhi and over fifteen in parts of
                // the north-east, so it is worth correcting wherever the phone is.
                position?.let { (latitude, longitude) ->
                    val from = declinationFrom
                    val moved = from == null || approxDistanceMeters(
                        GeoPoint(from.first, from.second),
                        GeoPoint(latitude, longitude)
                    ) >= DECLINATION_REFRESH_METERS
                    if (moved) {
                        declination = GeomagneticField(
                            latitude.toFloat(),
                            longitude.toFloat(),
                            0f,
                            System.currentTimeMillis()
                        ).declination.toDouble()
                        declinationFrom = latitude to longitude
                    }
                }

                val target = (magnetic + declination + 360.0) % 360.0
                heading = easeBearing(heading, target, COMPASS_EASE)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        // SENSOR_DELAY_UI: about 60 ms between readings, which is plenty for something
        // being eased towards anyway, and a fraction of the wake-ups of _GAME.
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onDispose { sensorManager.unregisterListener(listener) }
    }

    val (latitude, longitude) = position ?: return null
    return DeviceFacing(latitude, longitude, heading)
}

/**
 * The sensor axes that correspond to "right" and "up" on screen for a given display
 * rotation, for [SensorManager.remapCoordinateSystem].
 */
private fun screenAxes(displayRotation: Int): Pair<Int, Int> = when (displayRotation) {
    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
}

/** How far the screen is turned from the device's natural orientation. */
@Suppress("DEPRECATION")
private fun android.content.Context.displayRotation(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display?.rotation ?: Surface.ROTATION_0
    } else {
        getSystemService(WindowManager::class.java)?.defaultDisplay?.rotation
            ?: Surface.ROTATION_0
    }

/**
 * How far the phone has to move before the magnetic declination is worked out again.
 *
 * Declination drifts roughly a degree per hundred kilometres, so two kilometres is
 * worth about 0.02° — far inside the width of the cone this corrects, and inside the
 * easing applied to the heading on top of it. In practice it means the field is built
 * once when a screen opens and essentially never again.
 */
private const val DECLINATION_REFRESH_METERS = 2_000.0
