// composeApp/src/commonMain/kotlin/com/developer/ev2z/network/MapsConfig.kt
package com.developer.ev2z.network

/**
 * Google Maps Platform credentials used from shared code.
 *
 * [WEB_API_KEY] is called as a web service (Roads API), not through the Maps SDK, so
 * it has to be a key with **no Android/iOS application restriction** — an app-restricted
 * key answers those calls with `REQUEST_DENIED`. It is the same key as the
 * `com.google.android.geo.API_KEY` in AndroidManifest.xml, which is currently
 * unrestricted; if that key is ever locked to the app, issue a separate server key
 * here (IP-restricted, Roads API enabled) instead of removing the restriction.
 */
object MapsConfig {
    const val WEB_API_KEY = "AIzaSyDJLtuwZbTCHxEriYAjfDf5gYT3a7s4a20"
}