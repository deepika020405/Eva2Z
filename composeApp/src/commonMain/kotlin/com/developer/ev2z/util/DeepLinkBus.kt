// commonMain/com/developer/ev2z/util/DeepLinkBus.kt
package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DeepLinkBus {
    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo

    fun navigateTo(destination: String) {
        _navigateTo.value = destination
    }

    fun clear() {
        _navigateTo.value = null
    }
}