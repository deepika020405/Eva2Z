package com.developer.ev2z.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The status check as Swift can call it — `IosStatusCheck.shared.run { success in … }`.
 *
 * `BGTaskScheduler`'s handler is synchronous and has to be told when the work is done,
 * so the suspend function is wrapped in a callback rather than exposed directly (Kotlin
 * suspend functions reach Swift as completion handlers only on the main actor, which is
 * not where a background task runs). See `AppDelegate.registerStatusCheckTask`.
 *
 * [Dispatchers.IO] rather than Main: this is a network call, and the whole point is that
 * no UI is running. The callback hops back to Main because that is where the task's
 * `setTaskCompleted` wants to be.
 */
object IosStatusCheck {

    fun run(completion: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val ranCleanly = VehicleStatusBackgroundCheck.run()
            withContext(Dispatchers.Main) { completion(ranCleanly) }
        }
    }
}
