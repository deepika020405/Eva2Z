package com.developer.ev2z.ui.screens.device_Report

expect class PlatformAddressProvider(context: Any?) {
    fun getAddress(lat: Double, lng: Double, callback: (String?) -> Unit)
}