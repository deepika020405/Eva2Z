package com.developer.ev2z

expect fun fetchPhoneLocation(onResult: (lat: Double, lng: Double) -> Unit)

/** Opens the device's location/app settings so the user can turn location services on. */
expect fun openLocationSettings()