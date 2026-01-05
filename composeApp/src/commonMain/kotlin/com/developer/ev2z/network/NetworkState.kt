package com.developer.ev2z.network

sealed class NetworkState {
    object Connected : NetworkState()
    object NoInternet : NetworkState()
    object SlowInternet : NetworkState()
}
