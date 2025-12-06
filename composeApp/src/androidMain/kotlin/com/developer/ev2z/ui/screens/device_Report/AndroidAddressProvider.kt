package com.developer.ev2z.ui.screens.device_Report

import android.content.Context
import android.location.Geocoder
import java.util.Locale

actual class PlatformAddressProvider actual constructor(context: Any?) {

    private val ctx = context as Context

    actual fun getAddress(lat: Double, lng: Double, callback: (String?) -> Unit) {
        try {
            val geocoder = Geocoder(ctx, Locale.getDefault())
            val result = geocoder.getFromLocation(lat, lng, 1)
            val address = result?.firstOrNull()?.getAddressLine(0)
            callback(address)
        } catch (e: Exception) {
            callback(null)
        }
    }
}