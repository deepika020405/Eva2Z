package com.developer.ev2z.util


object SmsUtil {
    fun send(deviceId: String) {
        val message = "⚠ Device $deviceId exited geofence!"
        println("SMS SENT: $message")
        // integrate SMS API or SmsManager here
    }
}