package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SosNotificationHandler {

    // replay=1 — cold start ke liye
    private val _alertFlow = MutableSharedFlow<Int>(replay = 1)
    val alertFlow: SharedFlow<Int> = _alertFlow.asSharedFlow()

    fun onSosAlertReceived(alertId: Int) {
        println("🚨 SOS ALERT RECEIVED: $alertId")
        _alertFlow.tryEmit(alertId)
    }

    fun clear() {
        _alertFlow.resetReplayCache()
    }
}