package com.developer.ev2z.network

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

actual class NetworkObserver {

    actual val networkState: Flow<NetworkState> = callbackFlow {
//        val monitor = NWPathMonitor()
//
//        monitor.pathUpdateHandler = { path: NWPath ->
//            val state = when {
//                path.status != NWPathStatus.NWPathStatusSatisfied ->
//                    NetworkState.NoInternet
//
//                path.isExpensive ->
//                    NetworkState.SlowInternet
//
//                else ->
//                    NetworkState.Connected
//            }
//
//            trySend(state)
//        }
//
//        monitor.start(queue = dispatch_get_main_queue())
//
//        awaitClose {
//            monitor.cancel()
//        }
    }
}