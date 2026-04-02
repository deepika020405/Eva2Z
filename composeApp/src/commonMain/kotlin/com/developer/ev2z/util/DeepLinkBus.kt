package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

object DeepLinkBus {
    // Use SharedFlow with replay=1 so cold-start deep links aren't lost
    private val _navigateTo = MutableSharedFlow<String>(replay = 1)
    val navigateTo: SharedFlow<String> = _navigateTo

    private val _productId = MutableStateFlow<Int?>(null)
    val productId: StateFlow<Int?> = _productId

    fun navigateTo(destination: String, productId: Int? = null) {
        _productId.value = productId
        _navigateTo.tryEmit(destination)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _navigateTo.resetReplayCache()
        _productId.value = null
    }
}
