package com.developer.ev2z.util

import com.developer.ev2z.ui.screens.Profile.GlobalContext
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual suspend fun isUpdateAvailable(): Boolean = suspendCancellableCoroutine { cont ->
    try {
        val manager = AppUpdateManagerFactory.create(GlobalContext.get())
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                cont.resume(info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
            }
            .addOnFailureListener {
                cont.resume(false)
            }
    } catch (e: Exception) {
        cont.resume(false)
    }
}