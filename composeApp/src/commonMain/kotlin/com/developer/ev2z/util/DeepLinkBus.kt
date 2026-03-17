// commonMain/com/developer/ev2z/util/DeepLinkBus.kt
package com.developer.ev2z.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object DeepLinkBus {
    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo

    private val _productId = MutableStateFlow<Int?>(null)
    val productId: StateFlow<Int?> = _productId

    private var counter = 0  // ✅ Simple counter

    fun navigateTo(destination: String, productId: Int? = null) {
        _productId.value = productId
        _navigateTo.value = null
        _navigateTo.value = "${destination}_${counter++}"  // ✅ Unique value
    }

    fun clear() {
        _navigateTo.value = null
        _productId.value = null
    }
}