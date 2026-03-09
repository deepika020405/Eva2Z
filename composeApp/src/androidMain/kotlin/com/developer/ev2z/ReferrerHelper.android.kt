package com.developer.ev2z

import android.os.Handler
import android.os.Looper
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.developer.ev2z.data.local.DeepLinkManager

actual fun fetchReferrerIfNeeded(onComplete: () -> Unit) {

    val context = AppContextHolder.context
    val mainHandler = Handler(Looper.getMainLooper())

    if (!DeepLinkManager.isFirstLaunch()) {
        mainHandler.post { onComplete() }
        return
    }

    DeepLinkManager.markFirstLaunchDone()

    val client = InstallReferrerClient.newBuilder(context).build()

    client.startConnection(object : InstallReferrerStateListener {

        override fun onInstallReferrerSetupFinished(responseCode: Int) {

            if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {

                val referrer = client.installReferrer.installReferrer

                if (referrer.contains("deep_link=")) {

                    val path = referrer
                        .split("deep_link=")
                        .getOrNull(1)
                        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

                    path?.let {
                        DeepLinkManager.savePendingDeepLink(it)
                    }
                }
            }

            if (client.isReady) {
                try {
                    client.endConnection()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            mainHandler.post { onComplete() }
        }

        override fun onInstallReferrerServiceDisconnected() {
            mainHandler.post { onComplete() }
        }
    })
}