package com.developer.ev2z.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class NetworkObserver(
    private val context: Context
) {

    @SuppressLint("ServiceCast")
    actual val networkState: Flow<NetworkState> = callbackFlow {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onLost(network: Network) {
                trySend(NetworkState.NoInternet)
            }

            override fun onAvailable(network: Network) {
                trySend(NetworkState.Connected)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val isSlow =
                    capabilities.linkDownstreamBandwidthKbps in 1..150

                trySend(
                    if (isSlow)
                        NetworkState.SlowInternet
                    else
                        NetworkState.Connected
                )
            }
        }

        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }
}
