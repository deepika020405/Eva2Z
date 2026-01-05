package com.developer.ev2z.network

import kotlinx.coroutines.flow.Flow

expect class NetworkObserver {
    val networkState: Flow<NetworkState>
}
