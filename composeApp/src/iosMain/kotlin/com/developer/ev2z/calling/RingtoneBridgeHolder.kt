package com.developer.ev2z.calling

interface IOSRingtoneDelegate {
    fun stopRingtone()
}

object RingtoneBridgeHolder {
    var delegate: IOSRingtoneDelegate? = null
}